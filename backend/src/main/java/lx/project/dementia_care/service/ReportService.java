package lx.project.dementia_care.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lx.project.dementia_care.client.GoogleAiClient;
import lx.project.dementia_care.dao.PeriodDAO;
import lx.project.dementia_care.dao.RecordDAO;
import lx.project.dementia_care.dao.ReportDAO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import lx.project.dementia_care.vo.ReportVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/**
 * 연간 보고서: 처음 호출 시 생성→DB 저장(캐시형) 주간/월간: 프리뷰(즉석 생성, DB 저장 없음)
 */
@Service
public class ReportService {

	private final RecordDAO recordDAO;
	private final ReportDAO reportDAO;
	private final PeriodDAO periodDAO;
	private final GoogleAiClient ai;
	private final ObjectMapper om;

	public ReportService(RecordDAO recordDAO, ReportDAO reportDAO, PeriodDAO periodDAO, GoogleAiClient ai,
			ObjectMapper om) {
		this.recordDAO = recordDAO;
		this.reportDAO = reportDAO;
		this.periodDAO = periodDAO;
		this.ai = ai;
		this.om = om;
	}

	// ---------------------------------------------------------------------
	// 연간 보고서: get-or-create (DB 캐시형)
	// ---------------------------------------------------------------------
	@Transactional
	public Map<String, Object> getOrCreateYearReport(Long patientId, int year) {
		// 1) 이미 있으면 그대로 반환
		ReportVO found = reportDAO.findByPatientPeriod(patientId, "year", String.valueOf(year));
		if (found != null)
			return mapReportRowToUi(found);

		// 2) 없으면 생성
		LocalDate start = LocalDate.of(year, 1, 1);
		LocalDate end = LocalDate.of(year, 12, 31);

		List<DailyRecordResponseDTO> rows = safeList(recordDAO.getRange(patientId, start, end));
		Map<String, Object> stats = computeStats(rows); // 규칙 기반 통계/점수
		String sourceHash = calcSourceHash(rows); // 원본 스냅샷 해시
		Map<String, Object> sections = buildSectionsSafe("yearly", start, end, stats); // AI 요약(실패해도 기본값)
		Map<String, Object> chartPrefs = defaultChartPrefs(); // 레이더 기본 옵션
		Map<String, Integer> metrics = statsToMetrics(stats); // 0~100 정수 점수셋

		// FK: period_id (해당 연도 없으면 생성)
		Integer periodId = periodDAO.getOrCreateYear(start, end);

		// INSERT (동시성 대비: 중복키면 재조회)
		try {
			reportDAO.insert(periodId, patientId, toStringOrEmpty(sections.get("summary")), // content: 요약 본문
					"year", String.valueOf(year), sourceHash, toJson(metrics), toJson(sections), toJson(chartPrefs),
					"gemini");
		} catch (DuplicateKeyException ignore) {
			// 동시 최초 호출로 이미 생성됨 → 재조회
		}

		ReportVO created = reportDAO.findByPatientPeriod(patientId, "year", String.valueOf(year));
		if (created == null) {
			// 방어
			created = new ReportVO();
			created.setPatientId(patientId);
			created.setPeriodType("year");
			created.setPeriodKey(String.valueOf(year));
			created.setVersion(1);
			created.setMetrics(toJson(metrics));
			created.setSections(toJson(sections));
			created.setChartPrefs(toJson(chartPrefs));
		}
		return mapReportRowToUi(created);
	}

	// ---------------------------------------------------------------------
	// (옵션) 프리뷰: 주/월 → 프론트 레거시 응답 포맷으로 즉석 생성(DB 저장 없음)
	// ---------------------------------------------------------------------
	public Map<String, Object> buildPreviewLegacy(Long userId, LocalDate start, LocalDate end,
			String period /* weekly|monthly */) {
		var rows = safeList(recordDAO.getRange(userId, start, end));
		var stats = computeStats(rows);
		var sections = buildSectionsSafe(period, start, end, stats);

		double shortGauge = clamp01(numOrZero(stats.get("shortMemoryGauge")));
		double longGauge = clamp01(numOrZero(stats.get("longMemoryGauge")));
		int cognitive = (int) Math.round(numOrZero(stats.get("cognitiveScore")));

		return Map.of("period", period, "range",
				Map.of("start", start.toString(), "end", end.toString(), "label",
						makeRangeLabelForLegacy(period, start, end)),
				"score", Map.of("cognitive", cognitive, "delta", 0), "memory",
				Map.of("short", Map.of("label", gaugeLabel(shortGauge), "gauge", shortGauge), "long",
						Map.of("label", gaugeLabel(longGauge), "gauge", longGauge)),
				"activities",
				Map.of("meal", Map.of("label", "보통"), "sleep", Map.of("label", "보통"), "exercise",
						Map.of("label", "보통")),
				"behaviorChanges", sections.getOrDefault("behavior_change", "이번 기간의 행동 변화를 분석했습니다."), "weeklySummary",
				sections.getOrDefault("summary", "전반적으로 안정적인 상태입니다."), "recommendations",
				sections.getOrDefault("recommendations", List.of("규칙적인 수면 패턴 유지", "가벼운 운동 권장", "가족과의 대화 시간 확보")),
				"aiTip", Map.of("title", "오늘의 추천", "body", "산책과 퍼즐 등 가벼운 인지 자극 활동을 권장합니다."));
	}

	private String makeRangeLabelForLegacy(String period, LocalDate start, LocalDate end) {
		if ("weekly".equalsIgnoreCase(period)) {
			int weekOfMonth = (start.getDayOfMonth() - 1) / 7 + 1;
			return "%d년 %d월 %d주차".formatted(start.getYear(), start.getMonthValue(), weekOfMonth);
		}
		if ("monthly".equalsIgnoreCase(period)) {
			return "%d년 %d월".formatted(start.getYear(), start.getMonthValue());
		}
		return "%s ~ %s".formatted(start, end);
	}

	private String gaugeLabel(double g) {
		if (g >= 0.7)
			return "양호";
		if (g >= 0.4)
			return "보통";
		return "주의";
	}

	// ---------------------------------------------------------------------
	// ★ 연간(캐시) → 레거시 스키마로 변환 (프론트 호환)
	// ---------------------------------------------------------------------
	public Map<String, Object> adaptYearToLegacy(Map<String, Object> yearMap) {
		Map<String, Object> metrics = asMap(yearMap.get("metrics"));
		Map<String, Object> sections = asMap(yearMap.get("sections"));

		String periodKey = String.valueOf(yearMap.getOrDefault("periodKey", ""));
		int year;
		try {
			year = Integer.parseInt(periodKey);
		} catch (Exception e) {
			year = LocalDate.now().getYear();
		}

		String start = year + "-01-01";
		String end = year + "-12-31";
		String label = year + "년";

		int shortMem = (int) getNum(metrics.get("short_memory"));
		int longMem = (int) getNum(metrics.get("long_memory"));
		int cognitive = (int) getNum(metrics.getOrDefault("exec_func", metrics.getOrDefault("cognitive", 0)));

		double shortGauge = clamp01(shortMem / 100.0);
		double longGauge = clamp01(longMem / 100.0);

		return Map.of("period", "yearly", "range", Map.of("start", start, "end", end, "label", label), "score",
				Map.of("cognitive", cognitive, "delta", 0), "memory",
				Map.of("short", Map.of("label", gaugeLabel(shortGauge), "gauge", shortGauge), "long",
						Map.of("label", gaugeLabel(longGauge), "gauge", longGauge)),
				"activities",
				Map.of("meal", Map.of("label", "보통"), "sleep", Map.of("label", "보통"), "exercise",
						Map.of("label", "보통")),
				"behaviorChanges", sections.getOrDefault("behavior_change", "이번 기간의 행동 변화를 분석했습니다."), "weeklySummary",
				sections.getOrDefault("summary", "전반적으로 안정적인 상태입니다."), "recommendations",
				sections.getOrDefault("recommendations", List.of("규칙적인 수면 패턴 유지", "가벼운 운동 권장", "가족과의 대화 시간 확보")),
				"aiTip", Map.of("title", "오늘의 추천", "body", "산책과 퍼즐 등 가벼운 인지 자극 활동을 권장합니다."));
	}

	private double getNum(Object o) {
		if (o instanceof Number n)
			return n.doubleValue();
		try {
			return Double.parseDouble(String.valueOf(o));
		} catch (Exception e) {
			return 0;
		}
	}

	// ---------------------------------------------------------------------
	// 통계/점수 계산
	// ---------------------------------------------------------------------
	private Map<String, Object> computeStats(List<DailyRecordResponseDTO> rows) {
		int days = Math.max(1, rows.size());
		int moodSum = 0, moodCount = 0;
		int nightWanderDays = 0, fallDays = 0;
		int regularMealDays = 0;

		for (DailyRecordResponseDTO r : rows) {
			Map<String, Object> content = parseJson(r.getContent());

			Map<String, Object> feel = asMap(content.get("feel"));
			Number mood = asNumber(feel.get("moodScore"));
			if (mood != null) {
				moodSum += mood.intValue();
				moodCount++;
			}

			Map<String, Object> note = asMap(content.get("note"));
			if (asBooleanTrue(note.get("nightWander")))
				nightWanderDays++;

			Map<String, Object> act = asMap(content.get("act"));
			if (asBooleanTrue(act.get("fall")))
				fallDays++;

			Map<String, Object> meal = asMap(content.get("meal"));
			Object skip = meal.get("skipMeal"); // "아침"/"점심"/"저녁"/"없음"
			if ("없음".equals(skip))
				regularMealDays++;
		}

		double moodAvg5 = (moodCount == 0) ? 0.0 : (double) moodSum / moodCount; // 1~5
		int cognitiveScore = (int) Math.round((moodAvg5 / 5.0) * 100); // 0~100

		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("cognitiveScore", cognitiveScore);
		stats.put("shortMemoryGauge", clamp01(0.5 + (moodAvg5 - 3) / 6.0)); // 임시 산식
		stats.put("longMemoryGauge", clamp01(0.4));
		stats.put("nightWanderDays", nightWanderDays);
		stats.put("fallDays", fallDays);
		stats.put("regularMealRatio", (double) regularMealDays / days);
		stats.put("moodAvg5", moodAvg5);
		return stats;
	}

	// ---------------------------------------------------------------------
	// AI 섹션 생성 (실패 안전 / 동기 try-catch)
	// ---------------------------------------------------------------------
	private Map<String, Object> buildSectionsSafe(String period, LocalDate start, LocalDate end,
			Map<String, Object> stats) {
		Map<String, Object> sections = new LinkedHashMap<>();
		sections.put("summary", "전반적으로 안정적인 기간이었습니다.");
		sections.put("behavior_change", "두드러진 변화는 크지 않습니다. 일상 루틴 유지를 권장합니다.");
		sections.put("recommendations", List.of("규칙적인 수면", "가벼운 운동", "충분한 수분 섭취"));

		try {
			String prompt = buildPrompt(period, start, end, stats);
			String out;
			try {
				// 동기 호출 + 예외 방어
				out = safeString(ai.generateText(prompt));
			} catch (Exception e) {
				out = "";
			}
			if (!out.isBlank()) {
				sections.put("summary", (out.length() > 800) ? (out.substring(0, 800) + "…") : out);
			}
		} catch (Exception ignore) {
			// 실패 시 기본 문구 유지
		}
		return sections;
	}

	private String buildPrompt(String period, LocalDate start, LocalDate end, Map<String, Object> stats) {
		return """
				당신은 치매 환자의 주기별 상태를 가족에게 쉽게 설명하는 코치입니다.
				아래 통계를 바탕으로 3가지 섹션을 한국어로 짧고 친절하게 작성하세요.
				1) 행동 변화(두세 문장)
				2) 종합 요약(두세 문장)
				3) 권장사항(불렛 3개)
				숫자 나열보다 해석 위주로.

				통계(JSON):
				%s
				기간: %s ~ %s (%s)
				""".formatted(safeToString(stats), start, end, period);
	}

	// ---------------------------------------------------------------------
	// DB Row → 응답 변환 (캐시 조회용, 컨트롤러 직반환 X)
	// ---------------------------------------------------------------------
	private Map<String, Object> mapReportRowToUi(ReportVO r) {
		Map<String, Object> metrics = toMap(r.getMetrics());
		Map<String, Object> sections = toMap(r.getSections());
		Map<String, Object> prefs = toMap(r.getChartPrefs());

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("patientId", r.getPatientId());
		res.put("periodType", nullTo("year", r.getPeriodType()));
		res.put("periodKey", r.getPeriodKey());
		res.put("version", r.getVersion());
		res.put("generatedAt", r.getGeneratedAt());
		res.put("metrics", metrics);
		res.put("sections", sections);
		res.put("chartPrefs", prefs);
		res.put("changed", false);
		return res;
	}

	/** 레이더 차트 기본 옵션 */
	private Map<String, Object> defaultChartPrefs() {
		Map<String, Object> scale = new LinkedHashMap<>();
		scale.put("min", 0);
		scale.put("max", 100);
		scale.put("tick", 20);

		Map<String, Object> prefs = new LinkedHashMap<>();
		prefs.put("labels", List.of("단기기억", "장기기억", "집행기능", "주의력", "언어", "정서·활동"));
		prefs.put("scale", scale);
		prefs.put("polygon", 6);
		return prefs;
	}

	/** 원본 스냅샷 해시(MD5). updatedAt/recordDate/내용 포함 — 널/타입 안전 처리 */
	private String calcSourceHash(List<DailyRecordResponseDTO> rows) {
		try {
			List<Map<String, Object>> flat = new ArrayList<>();
			for (DailyRecordResponseDTO r : rows) {
				Map<String, Object> one = new LinkedHashMap<>();
				Object rd = r.getRecordDate();
				Object ut = r.getUpdatedAt();
				one.put("date", (rd == null) ? "" : rd.toString());
				one.put("updatedAt", (ut == null) ? "" : ut.toString());
				one.put("content", parseJson(r.getContent()));
				flat.add(one);
			}
			String json = om.writeValueAsString(flat);
			return md5(json);
		} catch (Exception e) {
			return "";
		}
	}

	private String md5(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : d)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	/** 통계 → 0~100 정수 점수셋(레이더용) */
	private Map<String, Integer> statsToMetrics(Map<String, Object> s) {
		int shortMem = (int) Math.round(numOrZero(s.get("shortMemoryGauge")) * 100.0);
		int longMem = (int) Math.round(numOrZero(s.get("longMemoryGauge")) * 100.0);
		int execFunc = (int) Math.round(numOrZero(s.get("cognitiveScore"))); // 임시 매핑
		int attention = 60; // TODO
		int language = 62; // TODO
		int affect = 66; // TODO

		Map<String, Integer> m = new LinkedHashMap<>();
		m.put("short_memory", shortMem);
		m.put("long_memory", longMem);
		m.put("exec_func", execFunc);
		m.put("attention", attention);
		m.put("language", language);
		m.put("affect_activity", affect);
		return m;
	}

	// ---------------------------------------------------------------------
	// 유틸
	// ---------------------------------------------------------------------
	private List<DailyRecordResponseDTO> safeList(List<DailyRecordResponseDTO> list) {
		return (list == null) ? Collections.emptyList() : list;
	}

	private Map<String, Object> parseJson(String json) {
		try {
			if (json == null || json.isBlank())
				return Map.of();
			return om.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private Map<String, Object> toMap(Object jsonLike) {
		try {
			if (jsonLike == null)
				return Map.of();
			if (jsonLike instanceof Map<?, ?> m) {
				Map<String, Object> res = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					res.put(String.valueOf(e.getKey()), e.getValue());
				}
				return res;
			}
			if (jsonLike instanceof String s) {
				if (s.isBlank())
					return Map.of();
				return om.readValue(s, new TypeReference<Map<String, Object>>() {
				});
			}
			return om.convertValue(jsonLike, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private Map<String, Object> asMap(Object o) {
		if (o instanceof Map<?, ?> m) {
			Map<String, Object> res = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				res.put(String.valueOf(e.getKey()), e.getValue());
			}
			return res;
		}
		return Map.of();
	}

	private boolean asBooleanTrue(Object o) {
		if (o instanceof Boolean b)
			return b;
		if (o instanceof String s)
			return "true".equalsIgnoreCase(s);
		return false;
	}

	private Number asNumber(Object o) {
		if (o instanceof Number n)
			return n;
		if (o instanceof String s) {
			try {
				return Double.parseDouble(s);
			} catch (Exception ignore) {
			}
		}
		return null;
	}

	private double numOrZero(Object o) {
		Number n = asNumber(o);
		return (n == null) ? 0.0 : n.doubleValue();
	}

	private double clamp01(double v) {
		return (v < 0) ? 0 : Math.min(1, v);
	}

	private String toJson(Object o) {
		try {
			return om.writeValueAsString(o);
		} catch (Exception e) {
			return "{}";
		}
	}

	private String safeToString(Object o) {
		try {
			return om.writeValueAsString(o);
		} catch (Exception e) {
			return String.valueOf(o);
		}
	}

	private String toStringOrEmpty(Object o) {
		return (o == null) ? "" : String.valueOf(o);
	}

	private String nullTo(String def, String v) {
		return (v == null || v.isBlank()) ? def : v;
	}

	private String safeString(Object o) {
		return (o == null) ? "" : String.valueOf(o);
	}
}
