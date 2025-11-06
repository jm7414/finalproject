// src/main/java/lx/project/dementia_care/service/ReportService.java
package lx.project.dementia_care.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lx.project.dementia_care.config.GoogleAiClient;
import lx.project.dementia_care.dao.PeriodDAO;
import lx.project.dementia_care.dao.RecordDAO;
import lx.project.dementia_care.dao.ReportDAO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import lx.project.dementia_care.vo.ReportVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 리포트 통합 서비스 - 커버리지: 주간(7/7), 월/연(기간 전체) 충족 시 생성 - metrics.scores(5개, 0~20) 보장
 * - sections.details: 항목별 1–2문장 (AI 시도 → 실패 시 규칙형 대체) - weekly OK 시
 * sections.quick_action 포함 - 연간 보조(totals/series/details) 제공
 */
@Service
@RequiredArgsConstructor
public class ReportService {

	private static final Logger log = LoggerFactory.getLogger(ReportService.class);
	private static final String AI_FAIL_MARK = "__AI_FAILED__";

	private final RecordDAO recordDAO; // getRange(userId, start, end)
	private final ReportDAO reportDAO; // upsertReturning(...)
	private final PeriodDAO periodDAO; // ensureId(type, key, start, end)
	private final ObjectMapper om;
	private final GoogleAiClient ai; // 외부 AI 클라

	/** 프론트 단일 엔드포인트용 핵심 엔트리 */
	public ReportVO loadOrCreate(Long patientId, String periodType, String periodKey, LocalDate start, LocalDate end,
			String generatedBy) {

		// 0) 기간 커버리지 판정 + 타입 정규화
		List<DailyRecordResponseDTO> rows = safeGetRange(patientId, start, end);
		int coveredDays = rows.size();
		int expectedDays = (int) ChronoUnit.DAYS.between(start, end); // [start, end)

		String normType = Optional.ofNullable(periodType).map(s -> s.trim().toUpperCase(Locale.ROOT)).orElse("");
		if (normType.startsWith("WEEK"))
			normType = "WEEK";
		else if (normType.startsWith("MONTH"))
			normType = "MONTH";
		else if (normType.startsWith("YEAR"))
			normType = "YEAR";

		log.info(
				"[AI-REPORT] loadOrCreate pid={}, periodType={}, normType={}, periodKey={}, start={}, end={}, coveredDays={}, expectedDays={}",
				patientId, periodType, normType, periodKey, start, end, coveredDays, expectedDays);

		if ("WEEK".equals(normType)) {
			if (coveredDays < 7) {
				log.warn("[AI-REPORT] weekly coverage not enough: {} / 7 → return null", coveredDays);
				return null; // 컨트롤러에서 부족 안내
			}
		} else {
			if (coveredDays < expectedDays) {
				log.warn("[AI-REPORT] {} coverage not enough: {} / {} → return null", normType, coveredDays,
						expectedDays);
				return null; // 컨트롤러에서 부족 안내
			}
		}

		// 1) 기간 ID 확보
		Integer periodId = periodDAO.ensureId(normType, periodKey, start, end);

		// 2) 점수(5개, 0~20)
		Map<String, Object> metrics = buildMetrics(rows);

		// 3) 섹션 기본틀
		Map<String, Object> sections = buildSections(rows, metrics, normType, periodKey, start, end);

		// 3-1) 항목별 코멘트 (AI 시도 → 실패 시 규칙형으로 대체)
		List<Map<String, Object>> details = buildDetailsWithAI(metrics, normType, periodKey, start, end);
		sections.put("details", details);

		// 3-2) 주간 OK일 때 '한 줄 작업'
		if ("WEEK".equals(normType)) {
			sections.put("quick_action", suggestQuickAction(rows));
		}

		// 3-3) AI 메타(status: ok/failed) — 한 개라도 AI 성공이면 ok
		boolean allFallback = details.stream().allMatch(it -> "rule".equals(String.valueOf(it.get("source"))));
		Map<String, Object> aiMeta = new LinkedHashMap<>();
		aiMeta.put("provider", "gemini");
		aiMeta.put("status", allFallback ? "failed" : "ok");
		sections.put("ai", aiMeta);

		// 4) 차트 프리셋 + 요약 본문
		Map<String, Object> chartPrefs = buildChartPrefs();
		String content = buildHumanSummary(metrics, sections, normType, periodKey, start, end);

		// 5) 원본 해시
		String sourceHash = hashFor(rows);

		// 6) JSON 직렬화
		String metricsJson, sectionsJson, chartPrefsJson;
		try {
			metricsJson = om.writeValueAsString(metrics);
			sectionsJson = om.writeValueAsString(sections);
			chartPrefsJson = om.writeValueAsString(chartPrefs);
		} catch (Exception e) {
			throw new RuntimeException("JSON 직렬화 실패", e);
		}

		// 7) UPSERT + RETURNING
		ReportVO result = reportDAO.upsertReturning(periodId, patientId, content, normType, periodKey, sourceHash,
				metricsJson, sectionsJson, chartPrefsJson, generatedBy);

		// 요약 로그 (AI 성공/대체 개수)
		long aiOk = details.stream().filter(d -> "ai".equals(d.get("source"))).count();
		long rule = details.size() - aiOk;
		log.info("[AI-REPORT] details summary: ai={}, rule={} (status={})", aiOk, rule, allFallback ? "failed" : "ok");

		return result;
	}

	/* ========================== 연간 보조데이터 ========================== */

	/** 연간: totals(12, 20~80), series, details(연간 평균 기반 AI/규칙 하이브리드 코멘트) */
	public Map<String, Object> buildYearlyExtras(Long patientId, LocalDate startInclusive, LocalDate endExclusive) {
		final int year = startInclusive.getYear();

		List<DailyRecordResponseDTO> rows = safeGetRange(patientId, startInclusive, endExclusive);
		Map<YearMonth, List<DailyRecordResponseDTO>> byMonth = rows.stream().collect(Collectors.groupingBy(r -> {
			LocalDate d = r.getRecordDate();
			return d != null ? YearMonth.from(d) : null;
		}));

		int[] totals = new int[12];
		List<Map<String, Object>> series = new ArrayList<>();

		double sumMS = 0, sumML = 0, sumOR = 0, sumADL = 0, sumBE = 0;
		int monthWithData = 0;

		for (int m = 1; m <= 12; m++) {
			YearMonth ym = YearMonth.of(year, m);
			List<DailyRecordResponseDTO> monthRows = byMonth.getOrDefault(ym, Collections.emptyList());

			Map<String, Object> metrics = buildMetrics(monthRows);
			int sum0to100 = sumScores0to100(metrics);
			int scaled20to80 = toScaled20to80(sum0to100);

			totals[m - 1] = scaled20to80;
			series.add(Map.of("month", ym.toString(), "metrics", metrics, "sum0to100", sum0to100, "scaled20to80",
					scaled20to80));

			Map<String, Object> sc = asMap(metrics.get("scores"));
			if (sc != null) {
				sumMS += toNum(sc.get("memory_short"));
				sumML += toNum(sc.get("memory_long"));
				sumOR += toNum(sc.get("orientation"));
				sumADL += toNum(sc.get("adl"));
				sumBE += toNum(sc.get("behavior_safety"));
				monthWithData++;
			}
		}

		double avgMS = (monthWithData > 0) ? sumMS / monthWithData : 12;
		double avgML = (monthWithData > 0) ? sumML / monthWithData : 12;
		double avgOR = (monthWithData > 0) ? sumOR / monthWithData : 12;
		double avgADL = (monthWithData > 0) ? sumADL / monthWithData : 12;
		double avgBE = (monthWithData > 0) ? sumBE / monthWithData : 12;

		// 연간도 동일 전략: AI 시도 → 실패 시 규칙형
		List<Map<String, Object>> details = new ArrayList<>();
		details.add(makeDetail("memory_short", "단기·작업기억", avgMS, "YEAR", String.valueOf(year), startInclusive,
				endExclusive));
		details.add(
				makeDetail("memory_long", "장기기억", avgML, "YEAR", String.valueOf(year), startInclusive, endExclusive));
		details.add(
				makeDetail("orientation", "지남력", avgOR, "YEAR", String.valueOf(year), startInclusive, endExclusive));
		details.add(makeDetail("adl", "일상기능", avgADL, "YEAR", String.valueOf(year), startInclusive, endExclusive));
		details.add(makeDetail("behavior_safety", "행동·기분·안전", avgBE, "YEAR", String.valueOf(year), startInclusive,
				endExclusive));

		boolean allFallback = details.stream().allMatch(d -> "rule".equals(String.valueOf(d.get("source"))));
		Map<String, Object> aiMeta = Map.of("provider", "gemini", "status", allFallback ? "failed" : "ok");

		return Map.of("totals", totals, "series", series, "details", details, "ai", aiMeta);
	}

	/** 연간 디테일 1개 생성(AI 우선, 실패 시 규칙형으로 대체) */
	private Map<String, Object> makeDetail(String key, String label, double value0to20, String periodType,
			String periodKey, LocalDate start, LocalDate end) {
		String txt = generateOneOrTwoLines(label, value0to20, periodType, periodKey, start, end);
		String source = "ai";
		if (AI_FAIL_MARK.equals(txt)) {
			txt = fallbackDetailText(label, value0to20);
			source = "rule";
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("key", key);
		row.put("label", label);
		row.put("value", (int) Math.round(value0to20));
		row.put("text", txt);
		row.put("source", source); // "ai" | "rule"
		row.put("aiStatus", "ai".equals(source) ? "ok" : "failed");
		return row;
	}

	/** 컨트롤러에서 사용: metrics JSON 문자열로부터 0~100 합계 계산 */
	public int getTotalScore0to100FromJson(String metricsJson) {
		if (metricsJson == null || metricsJson.isEmpty())
			return 0;
		try {
			Map<String, Object> metrics = om.readValue(metricsJson, new TypeReference<Map<String, Object>>() {
			});
			return sumScores0to100(metrics);
		} catch (Exception e) {
			return 0;
		}
	}

	/* ========================== 내부 유틸/스코어링 ========================== */

	private List<DailyRecordResponseDTO> safeGetRange(Long userId, LocalDate start, LocalDate end) {
		try {
			List<DailyRecordResponseDTO> rows = recordDAO.getRange(userId, start, end);
			return (rows != null) ? rows : Collections.emptyList();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	/** metrics.scores 5개(0~20) 보장 */
	private Map<String, Object> buildMetrics(List<DailyRecordResponseDTO> rows) {
		// 데이터 없어도 차트가 보이도록 기본값
		double mShort = 12, mLong = 12, orient = 12, adl = 12, beh = 12;

		if (rows != null && !rows.isEmpty()) {
			int fallCnt = 0, lostCnt = 0, nightCnt = 0, missApptHigh = 0;

			for (DailyRecordResponseDTO r : rows) {
				try {
					Map<String, Object> content = om.readValue(r.getContent(),
							new TypeReference<Map<String, Object>>() {
							});
					Map<String, Object> act = asMap(content.get("act"));
					Map<String, Object> note = asMap(content.get("note"));

					if (act != null) {
						fallCnt += isTrue(act.get("fall")) ? 1 : 0;
						lostCnt += isTrue(act.get("lostWay")) ? 1 : 0;
						Object miss = act.get("missAppt");
						if (miss != null && String.valueOf(miss).contains("3"))
							missApptHigh++;
					}
					if (note != null) {
						nightCnt += isTrue(note.get("nightWander")) ? 1 : 0;
					}
				} catch (Exception ignore) {
				}
			}

			mShort = clamp20(14 - Math.min(6, lostCnt));
			mLong = clamp20(14 - Math.min(6, missApptHigh));
			orient = clamp20(14 - Math.min(6, lostCnt + nightCnt));
			adl = clamp20(15 - Math.min(7, fallCnt + missApptHigh));
			beh = clamp20(15 - Math.min(7, nightCnt + fallCnt));
		}

		Map<String, Object> scores = new LinkedHashMap<>();
		scores.put("memory_short", mShort);
		scores.put("memory_long", mLong);
		scores.put("orientation", orient);
		scores.put("adl", adl);
		scores.put("behavior_safety", beh);

		Map<String, Object> metrics = new LinkedHashMap<>();
		metrics.put("scores", scores);
		return metrics;
	}

	/** sections 기본 틀 */
	private Map<String, Object> buildSections(List<DailyRecordResponseDTO> rows, Map<String, Object> metrics,
			String type, String key, LocalDate start, LocalDate end) {
		Map<String, Object> sections = new LinkedHashMap<>();
		sections.put("summary",
				String.format("%s(%s) 기간 요약: %d일 데이터 기반 간단 집계.", type, key, (rows != null ? rows.size() : 0)));
		sections.put("highlights", Collections.emptyList());
		sections.put("range", Map.of("start", start.toString(), "end", end.toString())); // end는 미포함 표기
		sections.put("period", Map.of("type", type, "key", key));
		// ❌ metricsEcho 제거 (프론트 미사용)
		return sections;
	}

	private Map<String, Object> buildChartPrefs() {
		Map<String, Object> radar = new LinkedHashMap<>();
		radar.put("scaleMax", 20);
		radar.put("labels", List.of("단기·작업기억", "장기기억", "지남력", "일상기능", "행동·기분·안전"));

		Map<String, Object> prefs = new LinkedHashMap<>();
		prefs.put("radar", radar);
		return prefs;
	}

	private String buildHumanSummary(Map<String, Object> metrics, Map<String, Object> sections, String type, String key,
			LocalDate start, LocalDate end) {
		Map<String, Object> s = asMap(metrics.get("scores"));
		String line = (s == null) ? "점수 산출 실패"
				: String.format("단기/작업기억:%s, 장기기억:%s, 지남력:%s, 일상기능:%s, 행동·안전:%s", s.get("memory_short"),
						s.get("memory_long"), s.get("orientation"), s.get("adl"), s.get("behavior_safety"));
		return String.format("초기 생성 리포트(임시). %s(%s) [%s ~ %s). %s", type, key, start, end, line);
	}

	private String hashFor(List<DailyRecordResponseDTO> rows) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			if (rows != null) {
				for (DailyRecordResponseDTO r : rows) {
					String s = (r.getRecordDate() != null ? r.getRecordDate().toString() : "") + "|"
							+ (r.getContent() != null ? r.getContent() : "");
					md.update(s.getBytes(StandardCharsets.UTF_8));
				}
			}
			byte[] b = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte x : b)
				sb.append(String.format("%02x", x));
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	/** 0~100 합계 계산 (metrics.scores 5개 합) */
	private int sumScores0to100(Map<String, Object> metrics) {
		Map<String, Object> s = asMap(metrics.get("scores"));
		if (s == null)
			return 0;
		double sum = 0;
		sum += toNum(s.get("memory_short"));
		sum += toNum(s.get("memory_long"));
		sum += toNum(s.get("orientation"));
		sum += toNum(s.get("adl"));
		sum += toNum(s.get("behavior_safety"));
		sum = Math.max(0, Math.min(100, sum));
		return (int) Math.round(sum);
	}

	/** “보여주기용” 20~80 스케일 변환 */
	private int toScaled20to80(int sum0to100) {
		return 20 + (int) Math.round(sum0to100 * 0.6);
	}

	/* ========================== AI 코멘트 생성 ========================== */

	private List<Map<String, Object>> buildDetailsWithAI(Map<String, Object> metrics, String periodType,
			String periodKey, LocalDate start, LocalDate end) {
		Map<String, Object> s = asMap(metrics.get("scores"));
		if (s == null)
			s = Map.of();

		List<ItemDef> items = Arrays.asList(new ItemDef("memory_short", "단기·작업기억"), new ItemDef("memory_long", "장기기억"),
				new ItemDef("orientation", "지남력"), new ItemDef("adl", "일상기능"),
				new ItemDef("behavior_safety", "행동·기분·안전"));

		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemDef def : items) {
			double v = toNum(s.get(def.key));
			String txt = generateOneOrTwoLines(def.label, v, periodType, periodKey, start, end); // 실패 시 AI_FAIL_MARK

			String source = "ai";
			if (AI_FAIL_MARK.equals(txt)) {
				txt = fallbackDetailText(def.label, v);
				source = "rule";
			}

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("key", def.key);
			row.put("label", def.label);
			row.put("value", (int) Math.round(v)); // 0~20
			row.put("text", txt);
			row.put("source", source); // "ai" | "rule"
			row.put("aiStatus", "ai".equals(source) ? "ok" : "failed");
			out.add(row);
		}
		return out;
	}

	private String generateOneOrTwoLines(String label, double score0to20, String periodType, String periodKey,
			LocalDate start, LocalDate end) {
		String prompt = """
				당신은 고령자 치매 케어 코치입니다.
				아래 항목의 점수(0~20)를 바탕으로 보호자에게 줄 1–2문장, 140자 이내의 코멘트를 한국어로 작성하세요.
				과장·진단 단정·명령조·이모지 금지, 정보 제공/생활 코칭 톤 유지.
				의료적 확정 판단이나 위험 조장은 하지 마세요.
				항목: %s
				점수: %.0f / 20
				기간: %s(%s) [%s ~ %s)
				""".formatted(label, score0to20, periodType, periodKey, start, end);

		try {
			String raw = ai.generateText(prompt); // GoogleAiClient 호출
			String post = postProcessOneOrTwoSentences(raw);
			if (post == null || post.isBlank())
				return AI_FAIL_MARK;
			return post;
		} catch (Exception e) {
			// 외부 응답 원문/키 등은 로그에 남기지 않음 (보안/가독)
			log.warn("[AI-REPORT] AI comment generation failed for label={} ({}: {})", label,
					e.getClass().getSimpleName(), safeMsg(e.getMessage()));
			return AI_FAIL_MARK;
		}
	}

	/** 규칙형(점수대별) 코멘트 – 의학적 확정 표현 지양, 제안/가이드 중심 */
	private String fallbackDetailText(String label, double v) {
		int score = (int) Math.round(Math.max(0, Math.min(20, v)));
		String band = (score >= 16) ? "good" : (score >= 8) ? "mid" : "low";

		Map<String, Map<String, String>> table = new HashMap<>();
		table.put("단기·작업기억",
				Map.of("good", "최근 대화나 일정 기억이 비교적 안정적이에요. 짧은 메모 습관을 유지해 주세요.", "mid",
						"단기 기억에 기복이 보여요. 한 번에 한 가지씩 안내하고 바로 확인하는 루틴이 좋아요.", "low",
						"최근 기억 누락이 잦을 수 있어요. 중요한 일은 메모·알림과 함께 짝지어 주세요."));
		table.put("장기기억",
				Map.of("good", "과거 일화 회상이 비교적 잘 이루어져요. 사진·음악과 함께 대화를 이어가요.", "mid",
						"장기 기억은 보통 수준으로 보입니다. 익숙한 물건이나 사진으로 단서를 더해 보세요.", "low",
						"장기 회상이 쉽지 않을 수 있어요. 짧고 긍정적인 과거 경험으로 안정감을 도와주세요."));
		table.put("지남력",
				Map.of("good", "시간·요일 확인이 비교적 잘 되고 있어요. 큰 글씨 달력과 벽시계를 계속 활용해 주세요.", "mid",
						"요일·시간 혼동이 가끔 보여요. 일정 확인 시간을 하루 1~2회 정해두면 도움이 됩니다.", "low",
						"시간·장소 혼란이 커질 수 있어요. 동선을 단순화하고 안내 표지를 추가해 안전을 확보하세요."));
		table.put("일상기능",
				Map.of("good", "식사·세면·복약 등 일상이 비교적 안정적이에요. 작은 칭찬과 코칭으로 유지해 보세요.", "mid",
						"일부 동작에 도움이 필요해 보여요. 순서 카드를 활용해 한 단계씩 함께 해보세요.", "low",
						"일상 보조가 자주 필요해요. 복약·세면 체크리스트와 동반이 안전에 도움이 됩니다."));
		table.put("행동·기분·안전",
				Map.of("good", "불안·낙상 신호가 낮게 보입니다. 현재 환경을 유지하며 주기적으로 점검해 주세요.", "mid",
						"야간 각성이나 불편 신호가 있어 보여요. 수면 위생과 안전등, 미끄럼 방지 매트를 확인해요.", "low",
						"안전 리스크가 커질 수 있어요. 밤중 동선 차단과 문 경보 등 안전 조치를 우선 정비하세요."));

		Map<String, String> row = table.getOrDefault(label,
				Map.of("good", "현재 상태가 비교적 안정적으로 보입니다. 기존 루틴을 유지해 주세요.", "mid",
						"일부 변화가 보여요. 일과를 단순하게 정리하고 바로 확인하는 습관이 도움 됩니다.", "low",
						"보조가 자주 필요해 보여요. 안전과 반복 안내 중심으로 천천히 진행해 주세요."));
		return row.getOrDefault(band, row.get("mid"));
	}

	/** 1–2문장 / 140자 / 이모지 제거 */
	private String postProcessOneOrTwoSentences(String s) {
		if (s == null)
			return "";
		String t = s.replaceAll("[\\r\\n]+", " ").trim();
		t = t.replaceAll("^\\s*[-•\\*\\d\\.\\)]\\s*", ""); // 머리표 제거
		t = t.replaceAll("^\\p{Zs}+", ""); // 선행 공백 제거
		String[] parts = t.split("(?<=[.!?。？！])\\s+");
		if (parts.length > 2)
			t = parts[0] + " " + parts[1];
		if (t.length() > 140)
			t = t.substring(0, 140).trim();
		t = t.replaceAll("[^\\p{L}\\p{N}\\p{Zs}\\p{P}]", ""); // 이모지/특수 제거
		return t.trim();
	}

	/* ========================== 공용 유틸 ========================== */

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object o) {
		if (o instanceof Map<?, ?> m)
			return (Map<String, Object>) m;
		return null;
	}

	private boolean isTrue(Object o) {
		if (o == null)
			return false;
		if (o instanceof Boolean b)
			return b;
		String s = String.valueOf(o).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s);
	}

	private double toNum(Object o) {
		if (o == null)
			return 0;
		try {
			return Math.max(0, Math.min(20, Double.parseDouble(String.valueOf(o))));
		} catch (Exception e) {
			return 0;
		}
	}

	private double clamp20(double v) {
		if (Double.isNaN(v) || Double.isInfinite(v))
			return 0;
		return Math.max(0, Math.min(20, v));
	}

	private static final class ItemDef {
		final String key, label;

		ItemDef(String key, String label) {
			this.key = key;
			this.label = label;
		}
	}

	/** (선택) 공개 유틸 */
	public int toScaled20to80FromJson(String metricsJson) {
		return toScaled20to80(getTotalScore0to100FromJson(metricsJson));
	}

	private String suggestQuickAction(List<DailyRecordResponseDTO> rows) {
		try {
			int days = (rows == null) ? 0 : rows.size();
			if (days == 7)
				return "이번 주에는 ‘저녁 산책 10분’부터 가볍게 시작해보세요.";
			else if (days >= 5)
				return "매일 같은 시간에 한 가지 질문만 꾸준히 체크해볼까요?";
			else
				return "알림을 켜고 오늘 하루 한 항목만 기록해보는 것부터 시작해요.";
		} catch (Exception e) {
			return "이번 주는 ‘저녁 산책 10분’부터 시작해보세요.";
		}
	}

	/** 예외 메시지 로그용 간단 정제 */
	private String safeMsg(String m) {
		if (m == null)
			return "";
		// URL/키/JSON 통짜 등 민감내용 길게 찍히지 않도록 간략화
		String t = m.replaceAll("\\s+", " ").trim();
		if (t.length() > 160)
			t = t.substring(0, 160) + "...";
		return t;
	}
	/** 일간 이모지 응답: {userId,date,score0to100,level,emoji} */
	public Map<String, Object> buildDailyEmoji(Long userId, LocalDate date) {
	    // [date, date+1) 하루치만 조회
	    List<DailyRecordResponseDTO> rows = safeGetRange(userId, date, date.plusDays(1));
	    int score = computeDailyScore0to100(rows); // 0~100
	    String level = scoreToLevel(score);        // good | mid | low
	    String emoji = switch (level) {
	        case "good" -> "😄";
	        case "mid"  -> "🙂";
	        default     -> "😟";
	    };
	    return Map.of(
	            "userId", userId,
	            "date", date.toString(),
	            "score0to100", score,
	            "level", level,
	            "emoji", emoji
	    );
	}

	/** 일간 점수 산출(0~100). 기존 주/월 스코어 규칙을 간소화하여 하루 기준으로 계산 */
	private int computeDailyScore0to100(List<DailyRecordResponseDTO> rows) {
	    // 하루치이므로 rows는 0 또는 >=1
	    double mShort = 12, mLong = 12, orient = 12, adl = 12, beh = 12;

	    if (rows != null && !rows.isEmpty()) {
	        int fallCnt = 0, lostCnt = 0, nightCnt = 0, missApptHigh = 0;

	        for (DailyRecordResponseDTO r : rows) {
	            try {
	                Map<String, Object> content = om.readValue(r.getContent(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
	                Map<String, Object> act = asMap(content.get("act"));
	                Map<String, Object> note = asMap(content.get("note"));

	                if (act != null) {
	                    fallCnt += isTrue(act.get("fall")) ? 1 : 0;
	                    lostCnt += isTrue(act.get("lostWay")) ? 1 : 0;
	                    Object miss = act.get("missAppt");
	                    if (miss != null && String.valueOf(miss).contains("3")) missApptHigh++;
	                }
	                if (note != null) {
	                    nightCnt += isTrue(note.get("nightWander")) ? 1 : 0;
	                }
	            } catch (Exception ignore) {}
	        }

	        // 주/월과 동일한 감점 로직을 “최대치 1~2회”로 제한
	        mShort = clamp20(14 - Math.min(2, lostCnt));
	        mLong  = clamp20(14 - Math.min(2, missApptHigh));
	        double orBase = lostCnt + nightCnt;
	        double adBase = fallCnt + missApptHigh;
	        double beBase = nightCnt + fallCnt;

	        orient = clamp20(14 - Math.min(2, orBase));
	        adl    = clamp20(15 - Math.min(2, adBase));
	        beh    = clamp20(15 - Math.min(2, beBase));
	    }

	    double sum = mShort + mLong + orient + adl + beh; // 0~100
	    return (int) Math.round(Math.max(0, Math.min(100, sum)));
	}

	/** 0~100 → good/mid/low */
	private String scoreToLevel(int s) {
	    if (s >= 67) return "good";   // 상 (대략 80점 만점대)
	    if (s >= 34) return "mid";    // 중
	    return "low";                 // 하
	}
	
	public String makePeriodKey(String normType, LocalDate start, LocalDate end) {
	    if ("WEEK".equalsIgnoreCase(normType)) {
	        java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
	        int y = start.get(wf.weekBasedYear());
	        int w = start.get(wf.weekOfWeekBasedYear());
	        return String.format("%04d-W%02d", y, w);
	    } else if ("MONTH".equalsIgnoreCase(normType)) {
	        return String.format("%04d-%02d", start.getYear(), start.getMonthValue());
	    }
	    return start.toString() + "_" + end.toString();
	}

	/** 커버리지 일수(컨트롤러 insufficient 응답용) */
	public int countCoveredDays(Long userId, LocalDate start, LocalDate end) {
	    return safeGetRange(userId, start, end).size();
	}
	
}
