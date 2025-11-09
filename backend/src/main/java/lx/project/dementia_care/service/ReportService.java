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

@Service
@RequiredArgsConstructor
public class ReportService {

	private static final Logger log = LoggerFactory.getLogger(ReportService.class);

	private final RecordDAO recordDAO;
	private final ReportDAO reportDAO;
	private final PeriodDAO periodDAO;
	private final ObjectMapper om;
	private final GoogleAiClient ai;

	/** 점수를 AI로 추정/보정할지 여부 (기본: false=현행 규칙 점수) */
	private static final boolean USE_AI_FOR_SCORES = false;

	// ====== 429 대응: 일정 시간 AI 호출 스킵(쿨다운) ======
	private volatile long cooldownUntil = 0L; // 429 이후 n초 동안 AI 호출 생략

	private boolean inCooldown() {
		return System.currentTimeMillis() < cooldownUntil;
	}

	/** 기존 6-인자 유지 (force=false) */
	public ReportVO loadOrCreate(Long patientId, String periodType, String periodKey, LocalDate start, LocalDate end,
			String generatedBy) {
		return loadOrCreate(patientId, periodType, periodKey, start, end, generatedBy, false);
	}

	/**
	 * 원하는 동작: - force=false: 기존 저장본 있으면 **무조건 반환**(원본이 바뀌었어도 재생성 금지) - force=true :
	 * 새로 생성/업서트
	 */
	public ReportVO loadOrCreate(Long patientId, String periodType, String periodKey, LocalDate start, LocalDate end,
			String generatedBy, boolean force) {

		String normType = Optional.ofNullable(periodType).map(s -> s.trim().toUpperCase(Locale.ROOT)).orElse("");
		if (normType.startsWith("WEEK"))
			normType = "WEEK";
		else if (normType.startsWith("MONTH"))
			normType = "MONTH";
		else if (normType.startsWith("YEAR"))
			normType = "YEAR";

		// 0) 캐시 우선
		if (!force) {
			ReportVO existing = reportDAO.findByPatientPeriod(patientId, normType, periodKey);
			if (existing != null)
				return existing;
		}

		// 1) 커버리지 확인(최초 생성 시에만)
		List<DailyRecordResponseDTO> rows = safeGetRange(patientId, start, end);
		int coveredDays = rows.size();
		int expectedDays = (int) ChronoUnit.DAYS.between(start, end); // [start, end)
		int required = "WEEK".equals(normType) ? 5 : (int) Math.ceil(expectedDays * 0.7);
		if (coveredDays < required) {
			log.warn("[AI-REPORT] {} coverage not enough: {}/{}(req) of total {}", normType, coveredDays, required,
					expectedDays);
			return null;
		}

		// 2) 기간 ID
		Integer periodId = periodDAO.ensureId(normType, periodKey, start, end);

		// 3) 점수·섹션
		Map<String, Object> metrics = buildMetrics(rows, normType, periodKey, start, end);
		Map<String, Object> sections = buildSections(rows, metrics, normType, periodKey, start, end);

		// 3-1) 항목 코멘트(AI) 실패 시 안전문구 (→ 단일 프롬프트로 1회 호출)
		List<Map<String, Object>> details = buildDetailsWithAI(metrics, normType, periodKey, start, end);
		sections.put("details", details);

		// 3-2) 주간 퀵액션
		if ("WEEK".equals(normType))
			sections.put("quick_action", suggestQuickAction(rows));

		// 3-3) AI 메타 (부분 성공을 partial-fallback으로 표기)
		boolean anyFailed = details.stream().anyMatch(d -> "failed".equals(String.valueOf(d.get("aiStatus"))));
		boolean anyOk = details.stream().anyMatch(d -> "ok".equals(String.valueOf(d.get("aiStatus"))));
		Map<String, Object> aiMeta = new LinkedHashMap<>();
		aiMeta.put("provider", "gemini");
		aiMeta.put("status", anyOk ? (anyFailed ? "partial-fallback" : "ok") : "failed");
		sections.put("ai", aiMeta);

		// 4) 차트 prefs + 본문 요약
		Map<String, Object> chartPrefs = buildChartPrefs();
		String content = buildHumanSummary(metrics, sections, normType, periodKey, start, end);

		// 5) 소스 해시(참고용)
		String sourceHash = hashFor(rows);

		// 6) 직렬화 & 업서트
		try {
			String metricsJson = om.writeValueAsString(metrics);
			String sectionsJson = om.writeValueAsString(sections);
			String chartPrefsJson = om.writeValueAsString(chartPrefs);
			return reportDAO.upsertReturning(periodId, patientId, content, normType, periodKey, sourceHash, metricsJson,
					sectionsJson, chartPrefsJson,
					(generatedBy != null && !generatedBy.isBlank()) ? generatedBy : "api");
		} catch (Exception e) {
			throw new RuntimeException("JSON 직렬화 실패", e);
		}
	}

	// ====================== 연간 보조(월간 캐시 + 스케치 보강) ======================

	/** 연간: totals/series/details/ai/annual */
	public Map<String, Object> buildYearlyExtras(Long userId, LocalDate start, LocalDate end) {
		final int year = start.getYear();

		Integer[] totals = new Integer[12];
		List<Map<String, Object>> series = new ArrayList<>();
		double sumMS = 0, sumML = 0, sumOR = 0, sumADL = 0, sumBE = 0;
		int monthsWithData = 0;

		for (int m = 1; m <= 12; m++) {
			LocalDate s = LocalDate.of(year, m, 1);
			LocalDate e = s.plusMonths(1);

			String periodKey = makePeriodKey("MONTH", s, e);
			// ★ 캐시만 사용: 기존 있으면 그걸, 없으면 생성 시도(커버리지 미달이면 null 반환)
			ReportVO vo = loadOrCreate(userId, "MONTH", periodKey, s, e, "annual-scan", false);

			Map<String, Object> rowMap = new LinkedHashMap<>();
			rowMap.put("month", String.format("%d-%02d", year, m));

			if (vo == null) {
				// ✦ 커버리지 부족 → 스케치 계산(저장 안함)
				List<DailyRecordResponseDTO> rows = safeGetRange(userId, s, e);
				Map<String, Object> sketchMetrics = buildMetrics(rows, "MONTH", periodKey, s, e);
				Map<String, Object> scores = asMap(sketchMetrics.get("scores"));

				double ms = getAsNumber(scores, "memory_short");
				double ml = getAsNumber(scores, "memory_long");
				double or = getAsNumber(scores, "orientation");
				double ad = getAsNumber(scores, "adl");
				double be = getAsNumber(scores, "behavior_safety");

				int sum0to100 = (int) Math.round(clamp20(ms) + clamp20(ml) + clamp20(or) + clamp20(ad) + clamp20(be));
				Integer scaled20to80 = rows.isEmpty() ? null
						: Math.max(20, Math.min(80, (int) Math.round(20 + sum0to100 * 0.6)));

				rowMap.put("metrics", mapOfFive("memory_short", ms, "memory_long", ml, "orientation", or, "adl", ad,
						"behavior_safety", be));
				rowMap.put("sum0to100", rows.isEmpty() ? null : sum0to100);
				rowMap.put("scaled20to80", scaled20to80);

				series.add(rowMap);
				totals[m - 1] = rows.isEmpty() ? null : sum0to100;

				if (!rows.isEmpty()) {
					sumMS += ms;
					sumML += ml;
					sumOR += or;
					sumADL += ad;
					sumBE += be;
					monthsWithData++;
				}
				continue;
			}

			// 저장본이 있는 월
			Map<String, Object> metrics = parseJsonMaybeTwiceToMap(vo.getMetrics());
			Map<String, Object> scores = (metrics != null) ? asMap(metrics.get("scores")) : null;

			double ms = getAsNumber(scores, "memory_short");
			double ml = getAsNumber(scores, "memory_long");
			double or = getAsNumber(scores, "orientation");
			double ad = getAsNumber(scores, "adl");
			double be = getAsNumber(scores, "behavior_safety");

			int sum0to100 = (int) Math.round(clamp20(ms) + clamp20(ml) + clamp20(or) + clamp20(ad) + clamp20(be));
			int scaled20to80 = Math.max(20, Math.min(80, (int) Math.round(20 + sum0to100 * 0.6)));

			rowMap.put("metrics", mapOfFive("memory_short", ms, "memory_long", ml, "orientation", or, "adl", ad,
					"behavior_safety", be));
			rowMap.put("sum0to100", sum0to100);
			rowMap.put("scaled20to80", scaled20to80);

			series.add(rowMap);
			totals[m - 1] = sum0to100;

			sumMS += ms;
			sumML += ml;
			sumOR += or;
			sumADL += ad;
			sumBE += be;
			monthsWithData++;
		}

		Map<String, Object> aiMeta = new LinkedHashMap<>();
		aiMeta.put("status", monthsWithData == 0 ? "empty" : "ok");

		List<Map<String, Object>> details = new ArrayList<>();
		Map<String, Object> annual = new LinkedHashMap<>();

		if (monthsWithData > 0) {
			double avgMS = sumMS / monthsWithData;
			double avgML = sumML / monthsWithData;
			double avgOR = sumOR / monthsWithData;
			double avgADL = sumADL / monthsWithData;
			double avgBE = sumBE / monthsWithData;

			details.add(makeAnnualDetail("memory_short", "단기·작업기억", avgMS, year));
			details.add(makeAnnualDetail("memory_long", "장기기억", avgML, year));
			details.add(makeAnnualDetail("orientation", "지남력", avgOR, year));
			details.add(makeAnnualDetail("adl", "일상기능", avgADL, year));
			details.add(makeAnnualDetail("behavior_safety", "행동·기분·안전", avgBE, year));

			List<Integer> totals20to80 = new ArrayList<>();
			for (Integer t : totals) {
				totals20to80.add(t == null ? null : Math.max(20, Math.min(80, (int) Math.round(20 + t * 0.6))));
			}
			annual = buildAnnualNarrative(year, totals20to80, avgMS, avgML, avgOR, avgADL, avgBE, monthsWithData);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("totals", totals);
		result.put("series", series);
		result.put("details", details);
		result.put("ai", aiMeta);
		result.put("annual", annual);

		if (monthsWithData == 0) {
			int expectedDays = (int) ChronoUnit.DAYS.between(start, end);
			result.put("eligibility", "INSUFFICIENT");
			result.put("expectedDays", expectedDays);
			result.put("coveredDays", 0);
		}
		return result;
	}

	// ---------------- 연간 헬퍼 3종 ----------------
	private Map<String, Object> makeAnnualDetail(String key, String label, double avg0to20, int year) {
		String txt = generateYearlyDetailText(label, avg0to20, year);
		String source = "ai";
		String aiStatus = "ok";
		if (txt == null || txt.isBlank()) {
			txt = fallbackGuidance(label, avg0to20);
			source = "rule";
			aiStatus = "failed";
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("key", key);
		row.put("label", label);
		row.put("value", (int) Math.round(avg0to20));
		row.put("text", txt);
		row.put("source", source);
		row.put("aiStatus", aiStatus);
		return row;
	}

	private String generateYearlyDetailText(String label, double avg0to20, int year) {
		String prompt = """
				당신은 치매 가족을 돕는 케어 코치입니다.
				아래 정보를 바탕으로 '연간 항목 코멘트'를 한국어로 1~2문장(140자 이내)으로 작성하세요.
				- 톤: 사실 기반+따뜻한 조언. 과장/단정/진단/이모지 금지.
				- 반드시 %d년을 한 번 언급.
				- 문장에 '유지/변화/권장' 중 1개 이상 포함.

				[항목] %s
				[연간 평균 점수] %.0f / 20
				""".formatted(year, label, avg0to20);
		try {
			String raw = ai.generateText(prompt);
			String post = postProcessOneOrTwoSentences(raw);
			if (post == null || post.isBlank())
				return fallbackGuidance(label, avg0to20);
			return post;
		} catch (Exception e) {
			String msg = safeMsg(e.getMessage());
			if (msg.contains("429") || msg.contains("TooManyRequests"))
				mark429Cooldown();
			log.warn("[AI-REPORT] Yearly detail failed: {} - {}", e.getClass().getSimpleName(), msg);
			return fallbackGuidance(label, avg0to20);
		}
	}

	private Map<String, Object> buildAnnualNarrative(int year, List<Integer> totals20to80, double avgMS, double avgML,
			double avgOR, double avgADL, double avgBE, int monthsWithData) {
		String jsonTotals = "[]";
		try {
			jsonTotals = om.writeValueAsString(totals20to80);
		} catch (Exception ignore) {
		}

		String prompt = """
				당신은 치매 케어 코치이자 연간 리포트 편집장입니다.
				아래 데이터를 기반으로 '연간 총정리'를 한국어로 작성하세요.

				[작성 지침]
				- 톤: 사실 기반, 따뜻하고 실천지향. 과장/진단/위협 표현, 이모지 금지.
				- 길이: overview 3~5문장, milestones 2~3개, next_quarter_focus 3개(행동 동사로 시작).

				[연도] %d년
				[월별 총점(20~80, null=데이터 부족)]: %s
				[연간 평균(0~20)] 단기·작업기억 %.1f, 장기기억 %.1f, 지남력 %.1f, 일상기능 %.1f, 행동·기분·안전 %.1f
				[커버리지 충족 월 수] %d

				[출력 형식(JSON만)]
				{
				  "overview": "3~5문장",
				  "milestones": ["...", "..."],
				  "next_quarter_focus": ["...", "...", "..."]
				}
				""".formatted(year, jsonTotals, avgMS, avgML, avgOR, avgADL, avgBE, monthsWithData);

		try {
			String raw = ai.generateText(prompt);
			Map<String, Object> m = om.readValue(raw, new TypeReference<>() {
			});
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("overview", String.valueOf(m.getOrDefault("overview", "")));
			Object ms = m.get("milestones");
			Object nq = m.get("next_quarter_focus");
			out.put("milestones", (ms instanceof List<?> l) ? l : List.of());
			out.put("next_quarter_focus", (nq instanceof List<?> l) ? l : List.of());
			return out;
		} catch (Exception e) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("overview", "올해 기록을 바탕으로 전반 경향을 정리했습니다. 일부 월은 데이터 부족으로 추세 확인이 제한될 수 있어요.");
			out.put("milestones", List.of("기록 충족 월 중심으로 일과 안정성 확인", "여름철 야간 각성·낙상 등 안전 신호 주기 점검"));
			out.put("next_quarter_focus", List.of("취침·기상 시각 고정", "복약 체크리스트 주 5회 확인", "안전등/미끄럼 방지 재점검"));
			log.warn("[AI-REPORT] Annual narrative failed: {} - {}", e.getClass().getSimpleName(),
					safeMsg(e.getMessage()));
			return out;
		}
	}

	// ================= 공통 유틸 =================
	private List<DailyRecordResponseDTO> safeGetRange(Long userId, LocalDate start, LocalDate end) {
		try {
			List<DailyRecordResponseDTO> rows = recordDAO.getRange(userId, start, end);
			return (rows != null) ? rows : Collections.emptyList();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	/** 점수 생성: 스위치에 따라 (A)규칙 or (B)AI(+폴백) */
	private Map<String, Object> buildMetrics(List<DailyRecordResponseDTO> rows, String periodType, String periodKey,
			LocalDate start, LocalDate end) {
		if (USE_AI_FOR_SCORES && !inCooldown()) {
			Map<String, Object> m = buildMetricsByAI(rows, periodType, periodKey, start, end);
			if (m != null)
				return m;
		}
		return buildMetricsByRule(rows);
	}

	/** (A) 규칙 점수 */
	private Map<String, Object> buildMetricsByRule(List<DailyRecordResponseDTO> rows) {
		double mShort = 12, mLong = 12, orient = 12, adl = 12, beh = 12;
		if (rows != null && !rows.isEmpty()) {
			int fallCnt = 0, lostCnt = 0, nightCnt = 0, missApptHigh = 0;
			for (DailyRecordResponseDTO r : rows) {
				try {
					Map<String, Object> content = om.readValue(r.getContent(), new TypeReference<>() {
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
					if (note != null)
						nightCnt += isTrue(note.get("nightWander")) ? 1 : 0;
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
		return new LinkedHashMap<>(Map.of("scores", scores));
	}

	/** (B) AI 점수 (실패/429 시 규칙 점수로 폴백) */
	private Map<String, Object> buildMetricsByAI(List<DailyRecordResponseDTO> rows, String periodType, String periodKey,
			LocalDate start, LocalDate end) {
		try {
			// 요약 피처 만들기
			int fallCnt = 0, lostCnt = 0, nightCnt = 0, missApptHigh = 0, days = 0;
			if (rows != null) {
				days = rows.size();
				for (DailyRecordResponseDTO r : rows) {
					try {
						Map<String, Object> content = om.readValue(r.getContent(), new TypeReference<>() {
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
						if (note != null)
							nightCnt += isTrue(note.get("nightWander")) ? 1 : 0;
					} catch (Exception ignore) {
					}
				}
			}

			String prompt = """
					당신은 치매 케어 코치입니다. 아래 기간의 기록 요약을 보고 5개 영역 점수(0~20)를 JSON 배열로만 출력하세요.
					- 키: memory_short, memory_long, orientation, adl, behavior_safety
					- 각 값은 0~20 정수.
					- 설명 문장, 코드블록 금지. JSON만.

					[기간] %s(%s) [%s ~ %s)
					[요약지표]
					- 기록일수: %d
					- 낙상일수: %d
					- 길 잃음/방향상실: %d
					- 야간배회: %d
					- 약속/일정 미준수(심각): %d
					""".formatted(periodType, periodKey, start, end, days, fallCnt, lostCnt, nightCnt, missApptHigh);

			String raw = ai.generateText(prompt);
			String json = extractJsonArray(raw);

			// 기대: [{"key":"memory_short","value":12}, ...] 또는 [{"memory_short":12,...}]도 허용
			List<Map<String, Object>> arr = om.readValue(json, new TypeReference<>() {
			});
			Map<String, Object> scores = new LinkedHashMap<>();

			// 케이스1: [{"key":"memory_short","value":12},...]
			for (Map<String, Object> e : arr) {
				if (e.containsKey("key")) {
					String k = String.valueOf(e.get("key"));
					Object v = e.getOrDefault("value", e.get("score"));
					if (k != null && v != null)
						scores.put(k, clamp20(toNum(v)));
				} else {
					// 케이스2: [{"memory_short":12,"memory_long":...}]
					for (String k : List.of("memory_short", "memory_long", "orientation", "adl", "behavior_safety")) {
						if (e.containsKey(k))
							scores.put(k, clamp20(toNum(e.get(k))));
					}
				}
			}

			// 검증: 5키 모두 없으면 실패 처리
			boolean ok = scores.keySet()
					.containsAll(Set.of("memory_short", "memory_long", "orientation", "adl", "behavior_safety"));
			if (!ok)
				throw new IllegalStateException("AI score parse fail");

			return Map.of("scores", scores);
		} catch (Exception e) {
			String msg = safeMsg(e.getMessage());
			if (msg.contains("429") || msg.contains("TooManyRequests"))
				mark429Cooldown();
			log.warn("[AI-REPORT] AI scores failed → fallback to rule: {} - {}", e.getClass().getSimpleName(), msg);
			return buildMetricsByRule(rows);
		}
	}

	private Map<String, Object> buildSections(List<DailyRecordResponseDTO> rows, Map<String, Object> metrics,
			String type, String key, LocalDate start, LocalDate end) {
		Map<String, Object> sections = new LinkedHashMap<>();
		sections.put("summary",
				String.format("%s(%s) 기간 요약: %d일 데이터 기반 간단 집계.", type, key, (rows != null ? rows.size() : 0)));
		sections.put("highlights", Collections.emptyList());
		sections.put("range", Map.of("start", start.toString(), "end", end.toString(), "label", key));
		sections.put("period", Map.of("type", type, "key", key));
		return sections;
	}

	private Map<String, Object> buildChartPrefs() {
		Map<String, Object> radar = new LinkedHashMap<>();
		radar.put("scaleMax", 20);
		radar.put("labels", List.of("단기·작업기억", "장기기억", "지남력", "일상기능", "행동·기분·안전"));
		return new LinkedHashMap<>(Map.of("radar", radar));
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
			if (rows != null)
				for (DailyRecordResponseDTO r : rows) {
					String s = (r.getRecordDate() != null ? r.getRecordDate().toString() : "") + "|"
							+ (r.getContent() != null ? r.getContent() : "");
					md.update(s.getBytes(StandardCharsets.UTF_8));
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

	public int getTotalScore0to100FromJson(String metricsJson) {
		if (metricsJson == null || metricsJson.isEmpty())
			return 0;
		try {
			Map<String, Object> metrics = om.readValue(metricsJson, new TypeReference<>() {
			});
			return sumScores0to100(metrics);
		} catch (Exception e) {
			return 0;
		}
	}

	public int toScaled20to80FromJson(String metricsJson) {
		return 20 + (int) Math.round(getTotalScore0to100FromJson(metricsJson) * 0.6);
	}

	public String makePeriodKey(String normType, LocalDate start, LocalDate end) {
		if ("WEEK".equalsIgnoreCase(normType)) {
			java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
			int y = start.get(wf.weekBasedYear());
			int w = start.get(wf.weekOfWeekBasedYear());
			return String.format("%04d-W%02d", y, w);
		} else if ("MONTH".equalsIgnoreCase(normType)) {
			return String.format("%04d-%02d", start.getYear(), start.getMonthValue());
		} else if ("YEAR".equalsIgnoreCase(normType)) {
			return String.format("%04d", start.getYear());
		}
		return start.toString() + "_" + end.toString();
	}

	public int countCoveredDays(Long userId, LocalDate start, LocalDate end) {
		return safeGetRange(userId, start, end).size();
	}

	// ===== 일간 이모지 =====
	public Map<String, Object> buildDailyEmoji(Long userId, LocalDate date) {
		List<DailyRecordResponseDTO> rows = safeGetRange(userId, date, date.plusDays(1));
		int covered = (rows == null) ? 0 : rows.size();
		if (covered == 0)
			return Map.of("userId", userId, "date", date.toString(), "coveredDays", 0, "score0to100", null, "level",
					"none", "emoji", "😴");
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
			int fallCnt = 0, lostCnt = 0, nightCnt = 0, missApptHigh = 0;
			for (DailyRecordResponseDTO r : rows) {
				try {
					Map<String, Object> content = om.readValue(r.getContent(), new TypeReference<>() {
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
					if (note != null)
						nightCnt += isTrue(note.get("nightWander")) ? 1 : 0;
				} catch (Exception ignore) {
				}
			}
			mShort = clamp20(14 - Math.min(2, lostCnt));
			mLong = clamp20(14 - Math.min(2, missApptHigh));
			double orBase = lostCnt + nightCnt, adBase = fallCnt + missApptHigh, beBase = nightCnt + fallCnt;
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

	// ===== 공통 =====
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
		return (int) Math.max(0, Math.min(100, Math.round(sum)));
	}

	private double clamp20(double v) {
		if (Double.isNaN(v) || Double.isInfinite(v))
			return 0;
		return Math.max(0, Math.min(20, v));
	}

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

	private double getAsNumber(Map<String, Object> m, String key) {
		if (m == null)
			return 0.0;
		Object v = m.get(key);
		if (v instanceof Number)
			return ((Number) v).doubleValue();
		try {
			return v == null ? 0.0 : Double.parseDouble(String.valueOf(v));
		} catch (Exception e) {
			return 0.0;
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

	/**
	 * (핵심 변경) 5항목 코멘트를 단일 프롬프트로 한 번에 생성. 429 발생 시 쿨다운 동안 규칙기반으로 즉시 대체.
	 */
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

		// 쿨다운이면 규칙기반으로 즉시 반환
		if (inCooldown()) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (ItemDef def : items) {
				double v = toNum(s.get(def.key()));
				out.add(Map.of("key", def.key(), "label", def.label(), "value", (int) Math.round(v), "text",
						fallbackGuidance(def.label(), v), "source", "rule", "aiStatus", "failed"));
			}
			return out;
		}

		// === 단일 프롬프트 ===
		StringBuilder sb = new StringBuilder();
		sb.append("""
				당신은 치매 가족을 돕는 케어 코치입니다.
				아래 5개 항목에 대해 각 1–2문장(140자 이내) 코멘트를 한국어로 작성하세요.
				- 톤: 사실 기반 + 따뜻한 조언. 과장/단정/진단/이모지 금지.
				- 출력은 JSON 배열로, 각 원소는 {"key": "...", "text": "..."} 형태.
				- 입력 기간: %s(%s) [%s ~ %s)
				""".formatted(periodType, periodKey, start, end));

		sb.append("\n[항목/점수(0~20)]\n");
		for (ItemDef def : items) {
			double v = toNum(s.get(def.key()));
			sb.append("- ").append(def.key()).append(" | ").append(def.label()).append(" : ")
					.append((int) Math.round(v)).append("\n");
		}

		String prompt = sb.toString();
		List<Map<String, Object>> out = new ArrayList<>();
		try {
			String raw = ai.generateText(prompt); // ← 단 1회 호출
			String json = extractJsonArray(raw);
			// 기대 형식: [{"key":"memory_short","text":"..."}, ...]
			List<Map<String, Object>> arr = om.readValue(json, new TypeReference<>() {
			});
			// 매핑 + 누락 보정
			Map<String, String> textByKey = new HashMap<>();
			if (arr != null) {
				for (Map<String, Object> e : arr) {
					String k = String.valueOf(e.get("key"));
					String t = postProcessOneOrTwoSentences(String.valueOf(e.get("text")));
					if (k != null && t != null && !t.isBlank())
						textByKey.put(k, t);
				}
			}

			for (ItemDef def : items) {
				double v = toNum(s.get(def.key()));
				String txt = textByKey.get(def.key());
				boolean ok = (txt != null && !txt.isBlank());
				if (!ok)
					txt = fallbackGuidance(def.label(), v);

				Map<String, Object> row = new LinkedHashMap<>();
				row.put("key", def.key());
				row.put("label", def.label());
				row.put("value", (int) Math.round(v));
				row.put("text", txt);
				row.put("source", ok ? "ai" : "rule");
				row.put("aiStatus", ok ? "ok" : "failed");
				out.add(row);
			}
			return out;

		} catch (Exception e) {
			String msg = safeMsg(e.getMessage());
			if (msg.contains("429") || msg.contains("TooManyRequests"))
				mark429Cooldown();

			// 실패 시 전부 규칙기반
			for (ItemDef def : items) {
				double v = toNum(s.get(def.key()));
				out.add(Map.of("key", def.key(), "label", def.label(), "value", (int) Math.round(v), "text",
						fallbackGuidance(def.label(), v), "source", "rule", "aiStatus", "failed"));
			}
			return out;
		}
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

	// ---- 규칙 기반 대체 문구 & 429 마킹 ----
	private String fallbackGuidance(String label, double score0to20) {
		int s = (int) Math.round(score0to20);
		if (s >= 15)
			return "%s은(는) %d년 동안 전반적으로 안정적이에요. 작은 규칙을 유지해 현재 수준을 지켜봐요.".formatted(label, LocalDate.now().getYear());
		if (s >= 10)
			return "%s은(는) 보통 수준이에요. 한 가지 루틴을 정해 꾸준함을 높여보세요.".formatted(label);
		if (s >= 5)
			return "%s이(가) 다소 불안정해 보여요. 이번 분기엔 한 영역을 골라 가볍게 연습을 늘려봐요.".formatted(label);
		return "%s이(가) 전반적으로 낮아요. 무리하지 말고 하루 한 번 체크부터 시작해요.".formatted(label);
	}

	private void mark429Cooldown() {
		long coolMs = 30 * 1000L; // 30초로 완화
		cooldownUntil = System.currentTimeMillis() + coolMs;
		log.warn("[AI-REPORT] 429 detected. cooldown {} sec", coolMs / 1000);
	}

	/** 모델 응답에서 JSON 배열만 뽑아낸다. ```json ...``` 또는 설명+JSON 모두 대응 */
	private String extractJsonArray(String s) {
		if (s == null)
			return "[]";
		String t = s.trim();
		// 코드펜스 제거
		if (t.startsWith("```")) {
			t = t.replaceAll("^```(?:json)?\\s*", "");
			t = t.replaceAll("\\s*```\\s*$", "");
		}
		// 첫 '['부터 마지막 ']'까지 잘라냄
		int i = t.indexOf('[');
		int j = t.lastIndexOf(']');
		if (i >= 0 && j > i)
			return t.substring(i, j + 1).trim();
		// 단일 오브젝트로 내려온 경우 배열로 감싸기
		int a = t.indexOf('{');
		int b = t.lastIndexOf('}');
		if (a >= 0 && b > a) {
			String obj = t.substring(a, b + 1).trim();
			return "[" + obj + "]";
		}
		return "[]";
	}
}
