// src/main/java/lx/project/dementia_care/service/ReportService.java
package lx.project.dementia_care.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lx.project.dementia_care.config.GoogleAiClient; // ★ config 패키지의 AI 클라이언트
import lx.project.dementia_care.dao.PeriodDAO;
import lx.project.dementia_care.dao.RecordDAO;
import lx.project.dementia_care.dao.ReportDAO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import lx.project.dementia_care.vo.ReportVO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 리포트 통합 서비스 - metrics.scores(5개, 0~20) 보장 - sections.details: 각 항목 1–2문장
 * 코멘트(AI 생성, 실패 시 "") - 연간 보조데이터: totals(12, 20~80 스케일), series, details(연간 평균
 * 기준 AI 코멘트)
 */
@Service
@RequiredArgsConstructor
public class ReportService {

	private final RecordDAO recordDAO; // getRange(userId, start, end)
	private final ReportDAO reportDAO; // upsertReturning(...)
	private final PeriodDAO periodDAO; // ensureId(type, key, start, end)
	private final ObjectMapper om;
	private final GoogleAiClient ai; // ★ config 패키지에 존재

	/** 프론트 단일 엔드포인트용 핵심 엔트리 */
	public ReportVO loadOrCreate(Long patientId, String periodType, // "week" | "month" | "year"
			String periodKey, // 예: 2025-W44 / 2025-09 / 2025
			LocalDate start, // [start, end) 컨벤션
			LocalDate end, String generatedBy) {

		// 1) 기간 ID 확보
		Integer periodId = periodDAO.ensureId(periodType, periodKey, start, end);

		// 2) 기간 데이터
		List<DailyRecordResponseDTO> rows = safeGetRange(patientId, start, end);

		// 3) 점수(5개, 0~20)
		Map<String, Object> metrics = buildMetrics(rows);

		// 4) 섹션/차트/본문
		Map<String, Object> sections = buildSections(rows, metrics, periodType, periodKey, start, end);

		// 4-1) ★ 항목별 AI 코멘트 생성(실패해도 예외 던지지 않고 ""로 대체)
		List<Map<String, Object>> details = buildDetailsWithAI(metrics, periodType, periodKey, start, end);
		sections.put("details", details);

		Map<String, Object> chartPrefs = buildChartPrefs();
		String content = buildHumanSummary(metrics, sections, periodType, periodKey, start, end);

		// 5) 원본 해시
		String sourceHash = hashFor(rows);

		// 6) JSON 직렬화
		String metricsJson, sectionsJson, chartPrefsJson;
		try {
			metricsJson = om.writeValueAsString(metrics);
			sectionsJson = om.writeValueAsString(sections);
			chartPrefsJson = om.writeValueAsString(chartPrefs);
		} catch (Exception e) {
			throw new RuntimeException("JSON 직렬화 실패: " + e.getMessage(), e);
		}

		// 7) UPSERT + RETURNING
		return reportDAO.upsertReturning(periodId, patientId, content, periodType, periodKey, sourceHash, metricsJson,
				sectionsJson, chartPrefsJson, generatedBy);
	}

	/* ========================== 연간 보조데이터 ========================== */

	/** 연간 라인차트/평균용: totals(12, 20~80 스케일), series, details(연간 평균 기반 AI 코멘트) */
	public Map<String, Object> buildYearlyExtras(Long patientId, LocalDate startInclusive, LocalDate endExclusive) {
		final int year = startInclusive.getYear();

		List<DailyRecordResponseDTO> rows = safeGetRange(patientId, startInclusive, endExclusive);
		Map<YearMonth, List<DailyRecordResponseDTO>> byMonth = rows.stream().collect(Collectors.groupingBy(r -> {
			LocalDate d = r.getRecordDate();
			return d != null ? YearMonth.from(d) : null;
		}));

		int[] totals = new int[12];
		List<Map<String, Object>> series = new ArrayList<>();

		// 연간 평균 계산을 위해 각 달의 scores를 누적
		double sumMS = 0, sumML = 0, sumOR = 0, sumADL = 0, sumBE = 0;
		int monthWithData = 0;

		for (int m = 1; m <= 12; m++) {
			YearMonth ym = YearMonth.of(year, m);
			List<DailyRecordResponseDTO> monthRows = byMonth.getOrDefault(ym, Collections.emptyList());

			Map<String, Object> metrics = buildMetrics(monthRows);
			int sum0to100 = sumScores0to100(metrics); // 실제 합(0~100)
			int scaled20to80 = toScaled20to80(sum0to100); // 보여주기 스케일(20~80)

			totals[m - 1] = scaled20to80;
			series.add(Map.of("month", ym.toString(), "metrics", metrics, "sum0to100", sum0to100, "scaled20to80",
					scaled20to80));

			// 연간 평균용 누적
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

		// 평균(0~20). 데이터 한 달도 없으면 기본 12로 표시
		double avgMS = (monthWithData > 0) ? sumMS / monthWithData : 12;
		double avgML = (monthWithData > 0) ? sumML / monthWithData : 12;
		double avgOR = (monthWithData > 0) ? sumOR / monthWithData : 12;
		double avgADL = (monthWithData > 0) ? sumADL / monthWithData : 12;
		double avgBE = (monthWithData > 0) ? sumBE / monthWithData : 12;

		// ★ 연간도 AI 코멘트를 생성해서 details에 담아 반환
		List<Map<String, Object>> details = new ArrayList<>();
		details.add(Map.of("key", "memory_short", "label", "단기·작업기억", "value", (int) Math.round(avgMS), "text",
				generateOneOrTwoLines("단기·작업기억", avgMS, "year", String.valueOf(year), startInclusive, endExclusive)));
		details.add(Map.of("key", "memory_long", "label", "장기기억", "value", (int) Math.round(avgML), "text",
				generateOneOrTwoLines("장기기억", avgML, "year", String.valueOf(year), startInclusive, endExclusive)));
		details.add(Map.of("key", "orientation", "label", "지남력", "value", (int) Math.round(avgOR), "text",
				generateOneOrTwoLines("지남력", avgOR, "year", String.valueOf(year), startInclusive, endExclusive)));
		details.add(Map.of("key", "adl", "label", "일상기능", "value", (int) Math.round(avgADL), "text",
				generateOneOrTwoLines("일상기능", avgADL, "year", String.valueOf(year), startInclusive, endExclusive)));
		details.add(Map.of("key", "behavior_safety", "label", "행동·기분·안전", "value", (int) Math.round(avgBE), "text",
				generateOneOrTwoLines("행동·기분·안전", avgBE, "year", String.valueOf(year), startInclusive, endExclusive)));

		return Map.of("totals", totals, "series", series, "details", details);
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
			return rows != null ? rows : Collections.emptyList();
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
				String.format("%s(%s) 기간 요약: %d일 데이터 기반 간단 집계.", type, key, rows != null ? rows.size() : 0));
		sections.put("highlights", Collections.emptyList());
		sections.put("range", Map.of("start", start.toString(), "end", end.toString()));
		sections.put("period", Map.of("type", type, "key", key));
		sections.put("metricsEcho", metrics);
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

		return String.format("초기 생성 리포트(임시). %s(%s) [%s ~ %s]. %s", type, key, start, end, line);
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

	/* ========================== 점수/헬퍼 ========================== */

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

	/* ========================== AI 코멘트 생성 (무조건 AI) ========================== */

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
			String txt = generateOneOrTwoLines(def.label, v, periodType, periodKey, start, end); // 실패 시 "" 반환
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("key", def.key);
			row.put("label", def.label);
			row.put("value", (int) Math.round(v)); // 0~20
			row.put("text", txt);
			out.add(row);
		}
		return out;
	}

	/** AI 호출 + 안전한 후처리(1–2문장, 140자, 이모지 제거). 실패 시 "" 반환 */
	private String generateOneOrTwoLines(String label, double score0to20, String periodType, String periodKey,
			LocalDate start, LocalDate end) {
		String prompt = """
				당신은 고령자 치매 케어 코치입니다.
				아래 항목의 점수(0~20)를 바탕으로 보호자에게 줄 1–2문장, 140자 이내의 코멘트를 한국어로 작성하세요.
				과장·이모지·명령조 금지, 따뜻하고 구체적으로.
				항목: %s
				점수: %.0f / 20
				기간: %s(%s) [%s ~ %s]
				""".formatted(label, score0to20, periodType, periodKey, start, end);

		try {
			String raw = ai.generateText(prompt); // ← GoogleAiClient.generateText 사용
			return postProcessOneOrTwoSentences(raw);
		} catch (Exception e) {
			return ""; // 백업 문구 없이 빈 문자열
		}
	}

	/** 1–2문장 / 140자 / 이모지 제거 */
	private String postProcessOneOrTwoSentences(String s) {
		if (s == null)
			return "";
		String t = s.replaceAll("[\\r\\n]+", " ").trim();
		t = t.replaceAll("^\\s*[-•\\*\\d\\.\\)]\\s*", "");
		t = t.replaceAll("^\\p{Zs}+", "");
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

	/* 항목 정의용 작은 클래스 */
	private static final class ItemDef {
		final String key, label;

		ItemDef(String key, String label) {
			this.key = key;
			this.label = label;
		}
	}
}
