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
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 리포트 통합 서비스(클린 버전) - 커버리지: WEEK ≥ 5일, MONTH/YEAR ≥ 70% - metrics.scores:
 * 5개(0~20) - details: 항목별 1~2문장(순수 AI). 실패 시 빈 문자열로 남김 - YEAR:
 * totals/series/annual(요약) 생성. 빈 월은 null로 표기(프론트에서 선 끊김)
 */
@Service
@RequiredArgsConstructor
public class ReportService {

	private static final Logger log = LoggerFactory.getLogger(ReportService.class);

	private final RecordDAO recordDAO; // getRange(userId, start, end)
	private final ReportDAO reportDAO; // upsertReturning(...)
	private final PeriodDAO periodDAO; // ensureId(type, key, start, end)
	private final ObjectMapper om;
	private final GoogleAiClient ai;

	/* ============================== 엔트리 ============================== */

	// 기존 6-arg 시그니처 유지
	public ReportVO loadOrCreate(Long patientId, String periodType, String periodKey, LocalDate start, LocalDate end,
			String generatedBy) {
		return loadOrCreate(patientId, periodType, periodKey, start, end, generatedBy, true);
	}

	public ReportVO loadOrCreate(Long patientId, String periodType, String periodKey, LocalDate start, LocalDate end,
			String generatedBy, boolean useCache) {

		// 0) 커버리지 검사
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

		int required = "WEEK".equals(normType) ? 5 : (int) Math.ceil(expectedDays * 0.7);
		log.info("[AI-REPORT] loadOrCreate pid={}, type={}, key={}, start={}, end={}, covered/required/total={}/{}/{}",
				patientId, normType, periodKey, start, end, coveredDays, required, expectedDays);

		if (coveredDays < required) {
			log.warn("[AI-REPORT] coverage insufficient: {} / {} (of {}) → null", coveredDays, required, expectedDays);
			return null;
		}

		// 1) 기간 ID
		Integer periodId = periodDAO.ensureId(normType, periodKey, start, end);

		// 2) 점수 산출
		Map<String, Object> metrics = buildMetrics(rows);

		// 3) 섹션 기본
		Map<String, Object> sections = buildSections(rows, metrics, normType, periodKey, start, end);

		// 3-1) 항목별 코멘트(순수 AI, 실패 시 빈 문자열)
		List<Map<String, Object>> details = buildDetailsWithAI(metrics, normType, periodKey, start, end);
		sections.put("details", details);

		// 3-2) 주간 Quick Action
		if ("WEEK".equals(normType)) {
			sections.put("quick_action", suggestQuickAction(rows));
		}

		// 3-3) AI 메타(단순 표기)
		Map<String, Object> aiMeta = new LinkedHashMap<>();
		aiMeta.put("provider", "gemini");
		aiMeta.put("status", "ok");
		sections.put("ai", aiMeta);

		// 4) 차트 프리셋 + 요약
		Map<String, Object> chartPrefs = buildChartPrefs();
		String content = buildHumanSummary(metrics, sections, normType, periodKey, start, end);

		// 5) 원본 해시
		String sourceHash = hashFor(rows);

		// 6) 직렬화
		try {
			String metricsJson = om.writeValueAsString(metrics);
			String sectionsJson = om.writeValueAsString(sections);
			String chartPrefsJson = om.writeValueAsString(chartPrefs);

			// 7) UPSERT + RETURNING
			return reportDAO.upsertReturning(periodId, patientId, content, normType, periodKey, sourceHash, metricsJson,
					sectionsJson, chartPrefsJson,
					(generatedBy != null && !generatedBy.isBlank()) ? generatedBy : "api");
		} catch (Exception e) {
			throw new RuntimeException("JSON 직렬화 실패", e);
		}
	}

	/* ===================== 연간 보조(요약/시리즈 등) ===================== */

	/** 연간: totals(12, 0~100 or null), series, annual(요약) */
	public Map<String, Object> buildYearlyExtras(Long userId, LocalDate start, LocalDate end) {
		final int year = start.getYear();

		Integer[] totals = new Integer[12]; // 1~12월. 데이터 없으면 null
		List<Map<String, Object>> series = new ArrayList<>();

		// details는 연간 항목별 코멘트를 넣고 싶을 때 사용 (이번 버전은 비워둠)
		List<Map<String, Object>> details = new ArrayList<>();

		Map<String, Object> aiMeta = new LinkedHashMap<>();
		aiMeta.put("status", "ok");

		boolean anyMonthHasData = false;

		for (int m = 1; m <= 12; m++) {
			LocalDate s = LocalDate.of(year, m, 1);
			LocalDate e = s.plusMonths(1);

			// 이미 생성된 월간만 조회(생성 금지)
			String periodKey = makePeriodKey("MONTH", s, e);
			ReportVO vo = loadOrCreate(userId, "MONTH", periodKey, s, e, "annual-scan", true);

			if (vo == null) {
				totals[m - 1] = null;

				Map<String, Object> empty = new LinkedHashMap<>();
				empty.put("month", String.format("%d-%02d", year, m));
				empty.put("metrics", new LinkedHashMap<>()); // 빈 맵
				empty.put("sum0to100", null);
				empty.put("scaled20to80", null);
				series.add(empty);
				continue;
			}

			anyMonthHasData = true;

			Map<String, Object> metrics = parseJsonMaybeTwiceToMap(vo.getMetrics());
			Map<String, Object> scores = (metrics != null) ? asMap(metrics.get("scores")) : null;

			double ms = getAsNumber(scores, "memory_short");
			double ml = getAsNumber(scores, "memory_long");
			double or = getAsNumber(scores, "orientation");
			double ad = getAsNumber(scores, "adl");
			double be = getAsNumber(scores, "behavior_safety");

			int sum0to100 = (int) Math.round(
					clamp(ms, 0, 20) + clamp(ml, 0, 20) + clamp(or, 0, 20) + clamp(ad, 0, 20) + clamp(be, 0, 20));

			int scaled20to80 = (int) Math.round(20 + (sum0to100 * 0.6));
			scaled20to80 = (int) clamp(scaled20to80, 20, 80);

			totals[m - 1] = sum0to100;

			Map<String, Object> one = new LinkedHashMap<>();
			one.put("month", String.format("%d-%02d", year, m));
			one.put("metrics", mapOfFive("memory_short", ms, "memory_long", ml, "orientation", or, "adl", ad,
					"behavior_safety", be));
			one.put("sum0to100", sum0to100);
			one.put("scaled20to80", scaled20to80);
			series.add(one);
		}

		if (!anyMonthHasData)
			aiMeta.put("status", "empty");

		// 연간 내러티브(요약) — 실패 시에도 빈 문자열/빈 리스트로만 채움
		Map<String, Object> annual = buildAnnualNarrativeSafe(year, totals);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("totals", totals);
		out.put("series", series);
		out.put("details", details);
		out.put("ai", aiMeta);
		out.put("annual", annual);
		return out;
	}

	/* ============================== 헬퍼 ============================== */

	private double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object o) {
		if (o instanceof Map)
			return (Map<String, Object>) o;
		return null;
	}

	private double getAsNumber(Map<String, Object> m, String key) {
		if (m == null)
			return 0.0;
		Object v = m.get(key);
		if (v instanceof Number)
			return ((Number) v).doubleValue();
		try {
			return v == null ? 0.0 : Double.parseDouble(String.valueOf(v));
		} catch (Exception ignore) {
			return 0.0;
		}
	}

	private Map<String, Object> parseJsonMaybeTwiceToMap(Object raw) {
		if (raw == null)
			return null;
		try {
			if (raw instanceof String s) {
				Object a = om.readValue(s, Object.class);
				if (a instanceof String s2) {
					Object b = om.readValue(s2, Object.class);
					return asMap(b);
				}
				return asMap(a);
			}
			return asMap(raw);
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> mapOfFive(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4,
			Object v4, String k5, Object v5) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		return m;
	}

	private Map<String, Object> buildAnnualNarrativeSafe(int year, Integer[] totals0to100) {
		// 0~100을 20~80으로 변환한 리스트(JSON 넣기 좋게)
		List<Integer> scaled = new ArrayList<>();
		int monthsWithData = 0;
		for (Integer t : totals0to100) {
			if (t == null) {
				scaled.add(null);
			} else {
				monthsWithData++;
				int sc = 20 + (int) Math.round(t * 0.6);
				scaled.add(Math.max(20, Math.min(80, sc)));
			}
		}

		String prompt = """
				당신은 고령자 치매 케어 코치입니다.
				아래 1년 월별 총점(20~80, null은 데이터 부족)을 보고, 간결한 연간 요약을 한국어로 작성하세요.
				- 톤: 사실 기반, 따뜻하고 과장 없음. 진단 단정 금지.
				- 형식(JSON): {"overview":"...", "milestones":["..."], "next_quarter_focus":["..."]}
				[연도]: %d
				[월별 20~80]: %s
				[데이터 충족 월 수]: %d
				""".formatted(year, safeToJson(scaled), monthsWithData);

		try {
			String raw = ai.generateText(prompt);
			Map<String, Object> m = om.readValue(raw, new TypeReference<Map<String, Object>>() {
			});
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("overview", String.valueOf(m.getOrDefault("overview", "")));
			out.put("milestones", (m.get("milestones") instanceof List<?> l) ? l : List.of());
			out.put("next_quarter_focus", (m.get("next_quarter_focus") instanceof List<?> l) ? l : List.of());
			return out;
		} catch (Exception e) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("overview", "");
			out.put("milestones", List.of());
			out.put("next_quarter_focus", List.of());
			return out;
		}
	}

	private String safeToJson(Object o) {
		try {
			return om.writeValueAsString(o);
		} catch (Exception e) {
			return "[]";
		}
	}

	/* ============== 컨트롤러 보조(총점 변환 등) ============== */

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

	public int toScaled20to80FromJson(String metricsJson) {
		return toScaled20to80(getTotalScore0to100FromJson(metricsJson));
	}

	public String makePeriodKey(String normType, LocalDate start, LocalDate end) {
		if ("WEEK".equalsIgnoreCase(normType)) {
			var wf = java.time.temporal.WeekFields.ISO;
			int y = start.get(wf.weekBasedYear());
			int w = start.get(wf.weekOfWeekBasedYear());
			return String.format("%04d-W%02d", y, w);
		} else if ("MONTH".equalsIgnoreCase(normType)) {
			return String.format("%04d-%02d", start.getYear(), start.getMonthValue());
		}
		return start + "_" + end;
	}

	public int countCoveredDays(Long userId, LocalDate start, LocalDate end) {
		return safeGetRange(userId, start, end).size();
	}

	/* ======================== 일간 이모지 ======================== */

	public Map<String, Object> buildDailyEmoji(Long userId, LocalDate date) {
		List<DailyRecordResponseDTO> rows = safeGetRange(userId, date, date.plusDays(1));
		int covered = (rows == null) ? 0 : rows.size();

		if (covered == 0) {
			return Map.of("userId", userId, "date", date.toString(), "coveredDays", 0, "score0to100", null, "level",
					"none", "emoji", "😴");
		}

		int score = computeDailyScore0to100(rows);
		String level = scoreToLevel(score);
		String emoji = switch (level) {
		case "good" -> "😄";
		case "mid" -> "🙂";
		default -> "😟";
		};

		return Map.of("userId", userId, "date", date.toString(), "coveredDays", covered, "score0to100", score, "level",
				level, "emoji", emoji);
	}

	private int computeDailyScore0to100(List<DailyRecordResponseDTO> rows) {
		double mShort = 12, mLong = 12, orient = 12, adl = 12, beh = 12;
		if (rows != null && !rows.isEmpty()) {
			int fall = 0, lost = 0, night = 0, missHigh = 0;
			for (DailyRecordResponseDTO r : rows) {
				try {
					Map<String, Object> content = om.readValue(r.getContent(), new TypeReference<>() {
					});
					Map<String, Object> act = asMap(content.get("act"));
					Map<String, Object> note = asMap(content.get("note"));
					if (act != null) {
						fall += isTrue(act.get("fall")) ? 1 : 0;
						lost += isTrue(act.get("lostWay")) ? 1 : 0;
						Object miss = act.get("missAppt");
						if (miss != null && String.valueOf(miss).contains("3"))
							missHigh++;
					}
					if (note != null)
						night += isTrue(note.get("nightWander")) ? 1 : 0;
				} catch (Exception ignore) {
				}
			}
			mShort = clamp20(14 - Math.min(2, lost));
			mLong = clamp20(14 - Math.min(2, missHigh));
			double orBase = lost + night;
			double adBase = fall + missHigh;
			double beBase = night + fall;
			orient = clamp20(14 - Math.min(2, orBase));
			adl = clamp20(15 - Math.min(2, adBase));
			beh = clamp20(15 - Math.min(2, beBase));
		}
		double sum = mShort + mLong + orient + adl + beh;
		return (int) Math.round(Math.max(0, Math.min(100, sum)));
	}

	private String scoreToLevel(int s) {
		if (s >= 67)
			return "good";
		if (s >= 34)
			return "mid";
		return "low";
	}

	/* ===================== 내부 스코어/유틸 ===================== */

	private List<DailyRecordResponseDTO> safeGetRange(Long userId, LocalDate start, LocalDate end) {
		try {
			List<DailyRecordResponseDTO> rows = recordDAO.getRange(userId, start, end);
			return (rows != null) ? rows : Collections.emptyList();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private Map<String, Object> buildMetrics(List<DailyRecordResponseDTO> rows) {
		double mShort = 12, mLong = 12, orient = 12, adl = 12, beh = 12;

		if (rows != null && !rows.isEmpty()) {
			int fall = 0, lost = 0, night = 0, missHigh = 0;
			for (DailyRecordResponseDTO r : rows) {
				try {
					Map<String, Object> content = om.readValue(r.getContent(), new TypeReference<>() {
					});
					Map<String, Object> act = asMap(content.get("act"));
					Map<String, Object> note = asMap(content.get("note"));
					if (act != null) {
						fall += isTrue(act.get("fall")) ? 1 : 0;
						lost += isTrue(act.get("lostWay")) ? 1 : 0;
						Object miss = act.get("missAppt");
						if (miss != null && String.valueOf(miss).contains("3"))
							missHigh++;
					}
					if (note != null)
						night += isTrue(note.get("nightWander")) ? 1 : 0;
				} catch (Exception ignore) {
				}
			}
			mShort = clamp20(14 - Math.min(6, lost));
			mLong = clamp20(14 - Math.min(6, missHigh));
			orient = clamp20(14 - Math.min(6, lost + night));
			adl = clamp20(15 - Math.min(7, fall + missHigh));
			beh = clamp20(15 - Math.min(7, night + fall));
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

	private Map<String, Object> buildSections(List<DailyRecordResponseDTO> rows, Map<String, Object> metrics,
			String type, String key, LocalDate start, LocalDate end) {
		Map<String, Object> sections = new LinkedHashMap<>();
		sections.put("summary",
				String.format("%s(%s) 기간 요약: %d일 데이터 기반 간단 집계.", type, key, (rows != null ? rows.size() : 0)));
		sections.put("highlights", List.of());

		Map<String, Object> range = new LinkedHashMap<>();
		range.put("start", start.toString());
		range.put("end", end.toString());
		sections.put("range", range);

		Map<String, Object> period = new LinkedHashMap<>();
		period.put("type", type);
		period.put("key", key);
		sections.put("period", period);

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
		return String.format("초기 생성 리포트. %s(%s) [%s ~ %s). %s", type, key, start, end, line);
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

	private int toScaled20to80(int sum0to100) {
		return 20 + (int) Math.round(sum0to100 * 0.6);
	}

	/* ==================== AI 코멘트(순수, 실패 시 빈값) ==================== */

	private List<Map<String, Object>> buildDetailsWithAI(Map<String, Object> metrics, String periodType,
			String periodKey, LocalDate start, LocalDate end) {
		Map<String, Object> s = asMap(metrics.get("scores"));
		if (s == null)
			s = Map.of();

		record ItemDef(String key, String label) {
		}
		List<ItemDef> items = List.of(new ItemDef("memory_short", "단기·작업기억"), new ItemDef("memory_long", "장기기억"),
				new ItemDef("orientation", "지남력"), new ItemDef("adl", "일상기능"),
				new ItemDef("behavior_safety", "행동·기분·안전"));

		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemDef def : items) {
			double v = toNum(s.get(def.key()));
			String txt = generateOneOrTwoLinesSafe(def.label(), v, periodType, periodKey, start, end); // 실패 시 ""

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("key", def.key());
			row.put("label", def.label());
			row.put("value", (int) Math.round(v)); // 0~20
			row.put("text", txt); // 실패면 빈 문자열
			row.put("source", "ai");
			row.put("aiStatus", txt.isBlank() ? "failed" : "ok");
			out.add(row);
		}
		return out;
	}

	private String generateOneOrTwoLinesSafe(String label, double score0to20, String periodType, String periodKey,
			LocalDate start, LocalDate end) {
		String prompt = """
				당신은 고령자 치매 케어 코치입니다.
				아래 점수(0~20)를 바탕으로 보호자에게 줄 1–2문장(140자 이내)의 코멘트를 한국어로 작성하세요.
				과장/진단 단정/명령조/이모지 금지, 생활 코칭 톤.
				항목: %s
				점수: %.0f / 20
				기간: %s(%s) [%s ~ %s)
				""".formatted(label, score0to20, periodType, periodKey, start, end);
		try {
			String raw = ai.generateText(prompt);
			String post = postProcessOneOrTwoSentences(raw);
			return (post == null) ? "" : post;
		} catch (Exception e) {
			log.warn("[AI-REPORT] detail AI failed for {}: {}", label, safeMsg(e.getMessage()));
			return "";
		}
	}

	/* ======================== 공통 문자 처리 ======================== */

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
		t = t.replaceAll("[^\\p{L}\\p{N}\\p{Zs}\\p{P}]", "");
		return t.trim();
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

	private String safeMsg(String m) {
		if (m == null)
			return "";
		String t = m.replaceAll("\\s+", " ").trim();
		if (t.length() > 160)
			t = t.substring(0, 160) + "...";
		return t;
	}
}
