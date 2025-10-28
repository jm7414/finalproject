package lx.project.dementia_care.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lx.project.dementia_care.config.GoogleAiClient;
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
 * 연간 보고서: 처음 호출 시 생성→DB 저장(캐시형) 주간/월간: 프리뷰(즉석 생성, DB 저장 없음) CARE-5 레이더(5축 ·
 * 0–20점) 기준으로 통일
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
	// 연간 보고서: get-or-create (DB 캐시형) → CARE-5(0–20)로 저장
	// ---------------------------------------------------------------------
	@Transactional
	public Map<String, Object> getOrCreateYearReport(Long patientId, int year) {
		// 1) 이미 있으면 그대로 반환
		ReportVO found = reportDAO.findByPatientPeriod(patientId, "year", String.valueOf(year));
		if (found != null) {
			return mapReportRowToUi(found);
		}

		// 2) 없으면 생성
		LocalDate start = LocalDate.of(year, 1, 1);
		LocalDate end = LocalDate.of(year, 12, 31);

		List<DailyRecordResponseDTO> rows = safeList(recordDAO.getRange(patientId, start, end));
		Map<String, Object> stats = computeStats(rows); // 규칙 기반 통계
		String sourceHash = calcSourceHash(rows); // 원본 스냅샷 해시
		Map<String, Object> sections = buildSectionsSafe("yearly", start, end, stats); // AI 요약(실패해도 기본값)
		Map<String, Object> chartPrefs = defaultChartPrefs(); // 레이더 기본 옵션(0–20, 5축)
		Map<String, Object> metrics = care5MetricsEnvelope(stats); // ★ CARE-5(0–20) 점수셋 래퍼

		// FK: period_id (해당 연도 없으면 생성)
		Integer periodId = periodDAO.getOrCreateYear(start, end);

		// INSERT (동시성 대비: 중복키면 재조회)
		try {
			reportDAO.insert(periodId, patientId, toStringOrEmpty(sections.get("summary")), // content: 요약 본문
					"year", String.valueOf(year), sourceHash, toJson(metrics), // ★ CARE-5
					toJson(sections), toJson(chartPrefs), "gemini");
		} catch (DuplicateKeyException ignore) {
			// 동시 최초 호출로 이미 생성됨 → 재조회
		}

		ReportVO created = reportDAO.findByPatientPeriod(patientId, "year", String.valueOf(year));
		if (created == null) {
			// 방어: INSERT가 실패했지만 호출자는 결과를 기대함
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

		// ★ CARE-5(0–20) 래퍼 포함 → 프론트 레이더가 실제 점수로 렌더 가능
		Map<String, Object> care5 = care5MetricsEnvelope(stats);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("period", period);
		out.put("range", Map.of("start", start.toString(), "end", end.toString(), "label",
				makeRangeLabelForLegacy(period, start, end)));
		out.put("score", Map.of("cognitive", cognitive, "delta", 0));
		out.put("memory", Map.of("short", Map.of("label", gaugeLabel(shortGauge), "gauge", shortGauge), "long",
				Map.of("label", gaugeLabel(longGauge), "gauge", longGauge)));
		out.put("activities", Map.of("meal", Map.of("label", "보통"), "sleep", Map.of("label", "보통"), "exercise",
				Map.of("label", "보통")));
		out.put("behaviorChanges", sections.getOrDefault("behavior_change", "이번 기간의 행동 변화를 분석했습니다."));
		out.put("weeklySummary", sections.getOrDefault("summary", "전반적으로 안정적인 상태입니다."));
		out.put("recommendations",
				sections.getOrDefault("recommendations", List.of("규칙적인 수면 패턴 유지", "가벼운 운동 권장", "가족과의 대화 시간 확보")));
		out.put("aiTip", Map.of("title", "오늘의 추천", "body", "산책과 퍼즐 등 가벼운 인지 자극 활동을 권장합니다."));

		out.put("metrics", care5); // ★ 추가
		return out;
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
	// ★ 연간(캐시) → 레거시 스키마로 변환 (프론트 호환) + CARE-5 동봉
	// ---------------------------------------------------------------------
	public Map<String, Object> adaptYearToLegacy(Map<String, Object> yearMap) {
		Map<String, Object> metrics = asMap(yearMap.get("metrics"));
		Map<String, Object> sections = asMap(yearMap.get("sections"));

		// CARE-5 정규화(구버전 0–100 기반 저장본도 방어)
		Map<String, Object> care5 = normalizeCare5(metrics);

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

		// (레거시 카드용) 단기/장기 게이지
		int shortMem = (int) getNum(care5Nested(care5, "scores", "memory_short"));
		int longMem = (int) getNum(care5Nested(care5, "scores", "memory_long"));
		double shortGauge = clamp01(shortMem / 20.0);
		double longGauge = clamp01(longMem / 20.0);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("period", "yearly");
		out.put("range", Map.of("start", start, "end", end, "label", label));
		out.put("score", Map.of("cognitive", (int) getNum(care5Nested(care5, "scores", "total")), "delta", 0));
		out.put("memory", Map.of("short", Map.of("label", gaugeLabel(shortGauge), "gauge", shortGauge), "long",
				Map.of("label", gaugeLabel(longGauge), "gauge", longGauge)));
		out.put("activities", Map.of("meal", Map.of("label", "보통"), "sleep", Map.of("label", "보통"), "exercise",
				Map.of("label", "보통")));
		out.put("behaviorChanges", sections.getOrDefault("behavior_change", "이번 기간의 행동 변화를 분석했습니다."));
		out.put("weeklySummary", sections.getOrDefault("summary", "전반적으로 안정적인 상태입니다."));
		out.put("recommendations",
				sections.getOrDefault("recommendations", List.of("규칙적인 수면 패턴 유지", "가벼운 운동 권장", "가족과의 대화 시간 확보")));
		out.put("aiTip", Map.of("title", "오늘의 추천", "body", "산책과 퍼즐 등 가벼운 인지 자극 활동을 권장합니다."));

		out.put("metrics", care5); // ★ CARE-5 포함
		return out;
	}

	private Object care5Nested(Map<String, Object> care5, String key1, String key2) {
		Object s = (care5 == null) ? null : care5.get(key1);
		if (!(s instanceof Map<?, ?> sm))
			return 0;
		Object v = sm.get(key2);
		return (v == null) ? 0 : v;
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
	// 통계/점수 계산(간단 규칙; 추후 고도화 가능)
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
		int cognitiveScore = (int) Math.round((moodAvg5 / 5.0) * 100); // 0~100 (임시 총합형)

		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("cognitiveScore", cognitiveScore);
		stats.put("shortMemoryGauge", clamp01(0.5 + (moodAvg5 - 3) / 6.0)); // 임시 산식(0~1)
		stats.put("longMemoryGauge", clamp01(0.4)); // 임시 상수
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

	// ★ CARE-5 기준 프롬프트로 교체
	private String buildPrompt(String period, LocalDate start, LocalDate end, Map<String, Object> stats) {
		return """
				당신은 치매 케어 코치입니다. 의료적 진단이 아닌 생활 코칭 관점으로 간결히 답하세요.
				- 톤: 가족에게 설명하듯 따뜻하고 명료하게
				- 금지: 과잉 확신/의학적 단정/처방
				- 출력: 1) 행동 변화(2~3문장) 2) 종합 요약(2~3문장) 3) 권장 행동(불릿 3개)
				- 점수 스케일: CARE-5(각 축 0–20, 총합 0–100)
				  축: 단기·작업기억(memory_short), 장기기억(memory_long), 지남력(orientation), 일상기능(adl), 행동·기분·안전(behavior_safety)

				기간: %s ~ %s (%s)
				관찰 통계(JSON): %s
				""".formatted(start, end, period, safeToString(stats));
	}

	// ---------------------------------------------------------------------
	// ★ “그래프에 물어보기”용 Gemini Q&A
	// ---------------------------------------------------------------------
	public Map<String, Object> askCare5Insight(Long userId, LocalDate start, LocalDate end,
			String period /* weekly|monthly|yearly */, String question) {
		var rows = safeList(recordDAO.getRange(userId, start, end));
		var stats = computeStats(rows);
		var care5 = care5MetricsEnvelope(stats);

		// ★ CARE-5 기준 프롬프트로 교체
		String prompt = """
				당신은 치매 케어 코치입니다. 의료적 진단이 아닌 생활 코칭 관점으로 간결히 답하세요.
				- 톤: 가족에게 설명하듯 따뜻하고 명료하게
				- 금지: 과잉 확신/의학적 단정/처방
				- 출력: 1) 핵심 요약(2~3문장) 2) 해석 포인트 3) 권장 행동(불릿 3개)
				- 점수 스케일: CARE-5(각 축 0–20, 총합 0–100)
				  축: 단기·작업기억(memory_short), 장기기억(memory_long), 지남력(orientation), 일상기능(adl), 행동·기분·안전(behavior_safety)

				기간: %s ~ %s (%s)
				CARE-5(JSON): %s
				관찰 통계(JSON): %s

				사용자의 질문: %s
				""".formatted(start, end, period, safeToString(care5), safeToString(stats), question);

		String answer;
		try {
			answer = safeString(ai.generateText(prompt));
		} catch (Exception e) {
			answer = "분석 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.";
		}

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("answer", answer);
		res.put("metrics", care5);
		res.put("period", period);
		res.put("range", Map.of("start", start.toString(), "end", end.toString()));
		return res;
	}

	// ---------------------------------------------------------------------
	// DB Row → 응답 변환 (캐시 조회용, 컨트롤러 직반환 X)
	// ---------------------------------------------------------------------
	private Map<String, Object> mapReportRowToUi(ReportVO r) {
		Map<String, Object> metrics = toMap(r.getMetrics());
		Map<String, Object> sections = toMap(r.getSections());
		Map<String, Object> prefs = toMap(r.getChartPrefs());

		// ★ 저장 포맷이 구버전(0–100/6축)일 수도 있으니 CARE-5로 정규화
		metrics = normalizeCare5(metrics);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("patientId", r.getPatientId());
		res.put("periodType", nullTo("year", r.getPeriodType()));
		res.put("periodKey", r.getPeriodKey());
		res.put("version", r.getVersion());
		res.put("generatedAt", r.getGeneratedAt());
		res.put("metrics", metrics); // ★ CARE-5 래퍼
		res.put("sections", sections);
		res.put("chartPrefs", prefs);
		res.put("changed", false);
		return res;
	}

	/** 레이더 차트 기본 옵션 (0–20, 5축) */
	private Map<String, Object> defaultChartPrefs() {
		Map<String, Object> scale = new LinkedHashMap<>();
		scale.put("min", 0);
		scale.put("max", 20);
		scale.put("tick", 5);

		Map<String, Object> prefs = new LinkedHashMap<>();
		prefs.put("labels", List.of("단기·작업기억", "장기기억", "지남력", "일상기능", "행동·기분·안전"));
		prefs.put("scale", scale);
		prefs.put("polygon", 5);
		return prefs;
	}

	// ---------------------------------------------------------------------
	// ★ CARE-5(0–20) 스코어 생성 & 정규화
	// ---------------------------------------------------------------------
	/** CARE-5 점수(0–20) 계산: stats → {scale, version, scores{...}, total} */
	private Map<String, Object> care5MetricsEnvelope(Map<String, Object> s) {
		// 필요한 통계 꺼내기(널 안전)
		double shortGauge = clamp01(numOrZero(s.get("shortMemoryGauge"))); // 0~1
		double longGauge = clamp01(numOrZero(s.get("longMemoryGauge"))); // 0~1
		int nightWander = (int) Math.round(numOrZero(s.get("nightWanderDays")));
		int fallDays = (int) Math.round(numOrZero(s.get("fallDays")));
		double mealRatio = clamp01(numOrZero(s.get("regularMealRatio"))); // 0~1
		double moodAvg5 = numOrZero(s.get("moodAvg5")); // 1~5(없으면 0)

		// 5축 산식(임시 합리식) → 0~20 클램프
		int memoryShort = clamp20(Math.round((float) (shortGauge * 20)));
		int memoryLong = clamp20(Math.round((float) (longGauge * 20)));
		int orientation = clamp20((int) Math.round(20 - 4.0 * nightWander - 2.0 * fallDays));
		int adl = clamp20((int) Math.round(20 * mealRatio));
		int behavior = clamp20((int) Math.round(20 - 6.0 * fallDays - 3.0 * nightWander - Math.max(0, 5 - moodAvg5)));

		Map<String, Integer> scores = new LinkedHashMap<>();
		scores.put("memory_short", memoryShort);
		scores.put("memory_long", memoryLong);
		scores.put("orientation", orientation);
		scores.put("adl", adl);
		scores.put("behavior_safety", behavior);
		scores.put("total", memoryShort + memoryLong + orientation + adl + behavior);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("scale", "CARE5");
		out.put("version", "1.0");
		out.put("scores", scores);
		return out;
	}

	/** 저장된 metrics가 구버전(0–100/6축)일 수 있으니 CARE-5(0–20/5축)로 변환 */
	private Map<String, Object> normalizeCare5(Map<String, Object> m) {
		if (m == null || m.isEmpty())
			return Map.of();
		// 이미 CARE-5 래퍼면 그대로
		if ("CARE5".equals(String.valueOf(m.get("scale"))) && m.containsKey("scores"))
			return m;

		// 구버전 키(0~100) → 0~20 근사 매핑
		Integer sm = asNumber(m.get("short_memory")) != null ? (int) Math.round(numOrZero(m.get("short_memory")) * 0.2)
				: null;
		Integer lm = asNumber(m.get("long_memory")) != null ? (int) Math.round(numOrZero(m.get("long_memory")) * 0.2)
				: null;
		Integer ex = asNumber(m.get("exec_func")) != null ? (int) Math.round(numOrZero(m.get("exec_func")) * 0.2)
				: null;
		Integer at = asNumber(m.get("attention")) != null ? (int) Math.round(numOrZero(m.get("attention")) * 0.2)
				: null;
		Integer lg = asNumber(m.get("language")) != null ? (int) Math.round(numOrZero(m.get("language")) * 0.2) : null;
		Integer af = asNumber(m.get("affect_activity")) != null
				? (int) Math.round(numOrZero(m.get("affect_activity")) * 0.2)
				: null;

		int memoryShort = clamp20(nz(sm));
		int memoryLong = clamp20(nz(lm));
		int orientation = clamp20(nz(ex, 10)); // 임시 보정
		int adl = clamp20(nz(at, 10));
		int behaviorSafety = clamp20((nz(ex) + nz(at) + nz(lg) + nz(af)) / 2);

		Map<String, Integer> scores = new LinkedHashMap<>();
		scores.put("memory_short", memoryShort);
		scores.put("memory_long", memoryLong);
		scores.put("orientation", orientation);
		scores.put("adl", adl);
		scores.put("behavior_safety", behaviorSafety);
		scores.put("total", memoryShort + memoryLong + orientation + adl + behaviorSafety);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("scale", "CARE5");
		out.put("version", "compat-legacy");
		out.put("scores", scores);
		return out;
	}

	private int clamp20(int v) {
		return Math.max(0, Math.min(20, v));
	}

	private int nz(Integer v) {
		return v == null ? 0 : v;
	}

	private int nz(Integer v, int def) {
		return v == null ? def : v;
	}

	// ---------------------------------------------------------------------
	// 해시/JSON/널 안전 유틸
	// ---------------------------------------------------------------------
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

	// ---------------------------------------------------------------------
	// ▶ 주/월/연 집계 유틸(주간 CARE-5를 기반으로 월/연 집계)
	// ---------------------------------------------------------------------
	private record Care5(int ms, int ml, int ori, int adl, int beh) {
	}

	private static int clamp20i(int v) {
		return Math.max(0, Math.min(20, v));
	}

	private Care5 weeklyCare5(Long userId, LocalDate start, LocalDate end) {
		var rows = safeList(recordDAO.getRange(userId, start, end));
		var stats = computeStats(rows);
		Map<String, Object> care5 = care5MetricsEnvelope(stats); // scale: CARE5, scores:...
		@SuppressWarnings("unchecked")
		Map<String, ?> sRaw = (Map<String, ?>) care5.get("scores");
		if (sRaw == null || sRaw.isEmpty()) {
			return new Care5(0, 0, 0, 0, 0);
		}
		int ms = clampSafe20(sRaw.get("memory_short"));
		int ml = clampSafe20(sRaw.get("memory_long"));
		int ori = clampSafe20(sRaw.get("orientation"));
		int adl = clampSafe20(sRaw.get("adl"));
		int beh = clampSafe20(sRaw.get("behavior_safety"));
		return new Care5(ms, ml, ori, adl, beh);
	}

	private int clampSafe20(Object v) {
		Number n = asNumber(v);
		return clamp20i(n == null ? 0 : n.intValue());
	}

	private static int overlapDays(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) {
		LocalDate s = s1.isAfter(s2) ? s1 : s2;
		LocalDate e = e1.isBefore(e2) ? e1 : e2;
		if (e.isBefore(s))
			return 0;
		return (int) (e.toEpochDay() - s.toEpochDay() + 1);
	}

	/** ▶ 주간 점수들을 “해당 월에 겹치는 일수 가중 평균”으로 월간 CARE-5 산출(0~20) */
	public Map<String, Object> monthlyFromWeeks(Long userId, int year, int month) {
		LocalDate ms = LocalDate.of(year, month, 1);
		LocalDate me = ms.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

		java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
		LocalDate cursor = ms.with(wf.dayOfWeek(), 1);
		if (cursor.isAfter(ms))
			cursor = cursor.minusWeeks(1);

		double wSum = 0, msSum = 0, mlSum = 0, oriSum = 0, adlSum = 0, behSum = 0;
		while (!cursor.isAfter(me)) {
			LocalDate ws = cursor, we = cursor.plusDays(6);
			int w = overlapDays(ws, we, ms, me); // 이 주가 이번 달에 겹치는 일수(0~7)
			if (w > 0) {
				Care5 c = weeklyCare5(userId, ws, we);
				wSum += w;
				msSum += c.ms * w;
				mlSum += c.ml * w;
				oriSum += c.ori * w;
				adlSum += c.adl * w;
				behSum += c.beh * w;
			}
			cursor = cursor.plusWeeks(1);
		}

		int msAvg = (wSum == 0) ? 0 : clamp20i((int) Math.round(msSum / wSum));
		int mlAvg = (wSum == 0) ? 0 : clamp20i((int) Math.round(mlSum / wSum));
		int oriAvg = (wSum == 0) ? 0 : clamp20i((int) Math.round(oriSum / wSum));
		int adlAvg = (wSum == 0) ? 0 : clamp20i((int) Math.round(adlSum / wSum));
		int behAvg = (wSum == 0) ? 0 : clamp20i((int) Math.round(behSum / wSum));

		Map<String, Integer> scores = new LinkedHashMap<>();
		scores.put("memory_short", msAvg);
		scores.put("memory_long", mlAvg);
		scores.put("orientation", oriAvg);
		scores.put("adl", adlAvg);
		scores.put("behavior_safety", behAvg);
		scores.put("total", msAvg + mlAvg + oriAvg + adlAvg + behAvg); // 0~100

		return Map.of("period", "monthly", "range",
				Map.of("start", ms.toString(), "end", me.toString(), "label", "%d-%02d".formatted(year, month)),
				"metrics", Map.of("scale", "CARE5", "version", "1.0", "scores", scores), "score",
				Map.of("cognitive", scores.get("total"), "delta", 0));
	}

	/** ▶ 1~12월을 모두 산출 → 연간 시리즈(라인차트용 totals + 월별 metrics 포함) */
	public Map<String, Object> yearlyFromWeeks(Long userId, int year) {
		List<Map<String, Object>> months = new ArrayList<>();
		int[] totals = new int[12];
		for (int m = 1; m <= 12; m++) {
			Map<String, Object> one = monthlyFromWeeks(userId, year, m);
			@SuppressWarnings("unchecked")
			Map<String, Number> sc = (Map<String, Number>) ((Map<String, Object>) one.get("metrics")).get("scores");
			int total = sc.get("total").intValue();
			totals[m - 1] = total;
			months.add(Map.of("key", "%d-%02d".formatted(year, m), "total", total, "metrics", one.get("metrics")));
		}
		return Map.of("period", "yearly", "year", year, "series", months, // 월별 metrics 포함
				"totals", totals // 12길이 배열(0~100) → 라인차트용
		);
	}
	
	public Map<String, Object> explainCare5Items(
	        Long userId,
	        LocalDate start,
	        LocalDate end,
	        String period,                           // weekly | monthly | yearly
	        Map<String, Object> providedMetrics      // 선택: 프런트에서 보낸 CARE-5
	) {
	    // 1) CARE-5 확보: 프런트가 보내줬으면 그대로, 없으면 기간으로 재계산
	    Map<String, Object> care5;
	    if (providedMetrics != null && "CARE5".equals(String.valueOf(providedMetrics.get("scale")))) {
	        care5 = providedMetrics;
	    } else {
	        // 기간이 없으면 최근 7일로 보정
	        if (start == null || end == null) {
	            LocalDate today = LocalDate.now();
	            start = today.minusDays(6);
	            end = today;
	        }
	        var rows = safeList(recordDAO.getRange(userId, start, end));
	        var stats = computeStats(rows);
	        care5 = care5MetricsEnvelope(stats);
	    }

	    // 점수 꺼내기
	    @SuppressWarnings("unchecked")
	    Map<String, Object> scoresObj = (Map<String, Object>) care5.getOrDefault("scores", Map.of());
	    int ms  = toInt(scoresObj.get("memory_short"));
	    int ml  = toInt(scoresObj.get("memory_long"));
	    int ori = toInt(scoresObj.get("orientation"));
	    int adl = toInt(scoresObj.get("adl"));
	    int beh = toInt(scoresObj.get("behavior_safety"));

	    // 2) 기본 규칙 기반 한줄 설명(LLM 실패 시 fallback)
	    Map<String, String> fallback = new LinkedHashMap<>();
	    fallback.put("memory_short", bandText("memory_short", ms));
	    fallback.put("memory_long", bandText("memory_long", ml));
	    fallback.put("orientation", bandText("orientation", ori));
	    fallback.put("adl", bandText("adl", adl));
	    fallback.put("behavior_safety", bandText("behavior_safety", beh));

	    // 3) LLM 프롬프트 생성 → 반드시 JSON만 반환하도록 유도
	    String care5Json;
	    try { care5Json = om.writeValueAsString(care5); } catch (Exception e) { care5Json = "{}"; }

	    String prompt = """
	        당신은 치매 케어 코치입니다. 아래 CARE-5 점수(0~20)를 바탕으로
	        각 항목에 대해 '1~2문장'의 한국어 설명을 작성하세요.
	        - 의료적 단정/진단 금지, 생활 코칭 위주
	        - 출력은 반드시 JSON 하나만! (키는 정확히 다음 5개)
	          {"memory_short":"...", "memory_long":"...", "orientation":"...", "adl":"...", "behavior_safety":"..."}
	        CARE-5:
	        %s
	        """.formatted(care5Json);

	    Map<String, String> llmResult = null;
	    try {
	        String out = safeString(ai.generateText(prompt)).trim();
	        // 혹시 앞뒤에 코드블록/문장 섞이면 JSON 부분만 뽑기 시도
	        String json = extractJson(out);
	        if (json != null && !json.isBlank()) {
	            // {"key":"..."} 형태 파싱
	            @SuppressWarnings("unchecked")
	            Map<String, String> parsed = om.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
	            llmResult = parsed;
	        }
	    } catch (Exception ignore) {
	        // LLM 오류 시 fallback 사용
	    }

	    // 4) 응답 payload 구성
	    List<Map<String, Object>> items = new ArrayList<>();
	    items.add(buildItem("memory_short", "단기·작업기억", ms, (llmResult!=null? llmResult.get("memory_short"): null), fallback));
	    items.add(buildItem("memory_long", "장기기억",       ml, (llmResult!=null? llmResult.get("memory_long"):  null), fallback));
	    items.add(buildItem("orientation",  "지남력",        ori,(llmResult!=null? llmResult.get("orientation"):   null), fallback));
	    items.add(buildItem("adl",          "일상기능",      adl,(llmResult!=null? llmResult.get("adl"):           null), fallback));
	    items.add(buildItem("behavior_safety","행동·기분·안전", beh,(llmResult!=null? llmResult.get("behavior_safety"):null), fallback));

	    Map<String, Object> res = new LinkedHashMap<>();
	    res.put("period", (period==null? "weekly" : period));
	    if (start != null && end != null) {
	        res.put("range", Map.of("start", start.toString(), "end", end.toString()));
	    }
	    res.put("metrics", care5);   // 프론트에서 그대로 재사용 가능
	    res.put("items", items);     // 프론트 "항목별 자세히 보기"에 그대로 바인딩
	    return res;
	}

	// ------ 아래는 ReportService 내부 private helper들 ---------

	private static int toInt(Object v) {
	    if (v instanceof Number n) return Math.max(0, Math.min(20, n.intValue()));
	    try { return Math.max(0, Math.min(20, Integer.parseInt(String.valueOf(v)))); }
	    catch (Exception e) { return 0; }
	}

	private static String extractJson(String s) {
	    // 가장 바깥 { ... } 구간만 추출
	    int i = s.indexOf('{'); int j = s.lastIndexOf('}');
	    if (i >= 0 && j > i) return s.substring(i, j+1);
	    return null;
	}

	private Map<String,Object> buildItem(String key, String label, int value, String llm, Map<String,String> fallback){
	    String text = (llm != null && !llm.isBlank()) ? llm.trim() : fallback.getOrDefault(key, "");
	    Map<String,Object> m = new LinkedHashMap<>();
	    m.put("key", key);
	    m.put("label", label);
	    m.put("value", value);
	    m.put("text", text);
	    return m;
	}

	private String bandText(String key, int v){
	    String band = (v >= 16) ? "good" : (v >= 8) ? "mid" : "low";
	    return switch (key) {
	        case "memory_short" -> switch (band) {
	            case "good" -> "최근 대화·지시 기억이 비교적 안정적입니다. 짧은 작업기억 활동을 꾸준히 유지하세요.";
	            case "mid"  -> "단기 기억에 기복이 있습니다. 메모·알림과 단계별 안내가 도움이 됩니다.";
	            default     -> "최근 기억 누락이 잦을 수 있습니다. 한 번에 한 가지 요청과 반복 확인이 필요합니다.";
	        };
	        case "memory_long" -> switch (band) {
	            case "good" -> "과거 일화·인물 회상이 양호합니다. 사진·음악 등 장기기억 자극을 이어가세요.";
	            case "mid"  -> "장기 기억은 보통입니다. 감각 단서(사진·향·음악)를 활용해보세요.";
	            default     -> "장기 회상이 어려울 수 있습니다. 긍정적 과거 경험을 짧게 상기시키며 안정감을 돕세요.";
	        };
	        case "orientation" -> switch (band) {
	            case "good" -> "시간·장소 인지가 좋습니다. 달력·시계 확인 루틴을 유지하세요.";
	            case "mid"  -> "요일·시간 혼동이 가끔 있습니다. 큰 글씨 시계와 일정 안내판이 도움이 됩니다.";
	            default     -> "낯선 환경에서 혼란이 클 수 있습니다. 동선 단순화와 명확한 표지로 안전을 확보하세요.";
	        };
	        case "adl" -> switch (band) {
	            case "good" -> "식사·세면·복약 등 일상 기능이 비교적 안정적입니다. 최소한의 코칭으로 자율성을 지켜주세요.";
	            case "mid"  -> "일부 동작에 도움이 필요합니다. 순서 카드·체크리스트를 함께 사용해보세요.";
	            default     -> "일상 기능 보조가 필요합니다. 복약·세면은 동반 확인, 위험 작업은 보호자 동행이 안전합니다.";
	        };
	        default /* behavior_safety */ -> switch (band) {
	            case "good" -> "낙상·야간 배회 신호가 낮습니다. 현재 환경을 유지하며 정기 점검하세요.";
	            case "mid"  -> "가끔 불안·산만·야간 각성이 보입니다. 수면 위생과 안전등, 미끄럼 방지 매트를 확인하세요.";
	            default     -> "안전 리스크가 큽니다. 문 경보·야간 동선 차단 등 환경 안전을 우선 정비하세요.";
	        };
	    };
	}
	
}
