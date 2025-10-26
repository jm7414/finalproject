// src/main/java/lx/project/dementia_care/controller/ReportController.java
package lx.project.dementia_care.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lx.project.dementia_care.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	// DAO 없이 DB 저장/조회 (UPSERT & SELECT)
	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	/*
	 * ============================================================ 신규 규격:
	 * /api/reports - periodType: year|month|week (yearly/monthly/weekly도 허용) -
	 * periodKey : 2025 / 2025-07 / 2025-W30 - persist : true 면 저장, false 면 조회만 (기본
	 * true) => persist=false이면 DB에 저장된 게 있으면 그걸 그대로 반환
	 * ============================================================
	 */
	@GetMapping("/api/reports")
	public ResponseEntity<?> getReport(@RequestParam Long patientId, @RequestParam String periodType,
			@RequestParam String periodKey,
			@RequestParam(value = "persist", required = false, defaultValue = "true") boolean persist,
			@RequestParam(value = "save", required = false) Integer saveFallback) {

		String type = normalizePeriod(periodType); // year|month|week
		LocalDate start;
		LocalDate end;

		switch (type) {
		case "year": {
			int year = Integer.parseInt(periodKey);
			start = LocalDate.of(year, 1, 1);
			end = LocalDate.of(year, 12, 31);
			break;
		}
		case "month": {
			String[] ym = periodKey.split("-");
			int y = Integer.parseInt(ym[0]);
			int m = Integer.parseInt(ym[1]);
			start = LocalDate.of(y, m, 1);
			end = start.with(TemporalAdjusters.lastDayOfMonth());
			break;
		}
		case "week": {
			String[] parts = periodKey.split("-W");
			int isoYear = Integer.parseInt(parts[0]);
			int isoWeek = Integer.parseInt(parts[1]);
			WeekFields wf = WeekFields.ISO;
			start = LocalDate.of(isoYear, 1, 4).with(wf.weekOfWeekBasedYear(), isoWeek).with(wf.dayOfWeek(), 1);
			end = start.plusDays(6);
			break;
		}
		default:
			throw new IllegalArgumentException("지원하지 않는 periodType: " + periodType);
		}

		// DB 우선 조회: persist=false(또는 save!=1)이면 저장본 먼저 반환
		boolean doSave = persist || (saveFallback != null && saveFallback == 1);
		if (!doSave) {
			Map<String, Object> savedRow = fetchSavedRow(patientId, type, periodKey);
			if (savedRow != null) {
				Map<String, Object> legacy = adaptStoredToLegacy(savedRow, type);
				legacy.put("preview", false);
				return ResponseEntity.ok(legacy);
			}
		}

		// 저장본이 없거나 저장 요청이면 미리보기 생성
		String label = toApiPeriodLabel(type); // yearly/monthly/weekly
		Map<String, Object> payload = reportService.buildPreviewLegacy(patientId, start, end, label);

		if (doSave) {
			String key = makeKey(type, start); // YYYY / YYYY-MM / YYYY-Www
			Long periodId = upsertPeriod(type, key, start, end);
			upsertReport(patientId, periodId, type, key, payload);
			// 저장 후, 방금 저장한 것을 DB에서 다시 읽어 그대로 리턴(형태 통일)
			Map<String, Object> savedRow = fetchSavedRow(patientId, type, key);
			Map<String, Object> legacy = adaptStoredToLegacy(savedRow, type);
			legacy.put("preview", false);
			return ResponseEntity.ok(legacy);
		} else {
			payload.put("preview", true); // 저장 안 된 미리보기임을 표시
			return ResponseEntity.ok(payload);
		}
	}

	/*
	 * ============================================================ 레거시 규격:
	 * /api/ai/report - 미리보기: GET
	 * /api/ai/report?userId=5&period=monthly&start=...&end=... - 저장하기: GET
	 * /api/ai/report?userId=5&period=monthly&start=...&end=...&persist=true -
	 * period: weekly | monthly | yearly (week|month|year도 허용) => persist=false면 DB
	 * 저장본 있으면 그대로 반환, 없으면 미리보기
	 * ============================================================
	 */
	@GetMapping("/api/ai/report")
	public ResponseEntity<?> getAiReport(@RequestParam("userId") Long userId, @RequestParam("period") String period,
			@RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(value = "persist", required = false, defaultValue = "false") boolean persist,
			@RequestParam(value = "save", required = false) Integer saveFallback) {

		String type = normalizePeriod(period); // year|month|week

		// 경계 보정 (연/월/주 정확히)
		if ("year".equals(type)) {
			start = start.with(TemporalAdjusters.firstDayOfYear());
			end = start.with(TemporalAdjusters.lastDayOfYear());
		} else if ("month".equals(type)) {
			start = start.with(TemporalAdjusters.firstDayOfMonth());
			end = start.with(TemporalAdjusters.lastDayOfMonth());
		} else { // week
			WeekFields wf = WeekFields.ISO;
			start = start.with(wf.dayOfWeek(), 1);
			end = start.plusDays(6);
		}
		String key = makeKey(type, start); // YYYY / YYYY-MM / YYYY-Www

		// DB 우선 조회
		boolean doSave = persist || (saveFallback != null && saveFallback == 1);
		if (!doSave) {
			Map<String, Object> savedRow = fetchSavedRow(userId, type, key);
			if (savedRow != null) {
				Map<String, Object> legacy = adaptStoredToLegacy(savedRow, type);
				legacy.put("preview", false);
				return ResponseEntity.ok(legacy);
			}
		}

		// 저장본 없거나 저장 요청이면 미리보기 생성
		String label = toApiPeriodLabel(type);
		Map<String, Object> payload = reportService.buildPreviewLegacy(userId, start, end, label);

		if (doSave) {
			Long periodId = upsertPeriod(type, key, start, end);
			upsertReport(userId, periodId, type, key, payload);
			Map<String, Object> savedRow = fetchSavedRow(userId, type, key);
			Map<String, Object> legacy = adaptStoredToLegacy(savedRow, type);
			legacy.put("preview", false);
			return ResponseEntity.ok(legacy);
		} else {
			payload.put("preview", true);
			return ResponseEntity.ok(payload);
		}
	}

	/* ======================== DB 조회/저장 유틸 ======================== */

	/** 저장본 1건 조회 (없으면 null) */
	private Map<String, Object> fetchSavedRow(Long userId, String type, String keyOrMaybeKey) {
		// keyOrMaybeKey: /api/reports에선 전달받은 periodKey 그대로, /api/ai/report에선 makeKey 결과
		final String sql = "SELECT r.report_id, r.patient_id, r.content, "
				+ "       r.metrics::text AS metrics, r.sections::text AS sections, r.chart_prefs::text AS chart_prefs, "
				+ "       r.period_type, r.period_key, p.start_date, p.end_date " + "FROM report r "
				+ "JOIN period p ON p.period_id = r.period_id "
				+ "WHERE r.patient_id = :uid AND r.period_type = :t AND r.period_key = :k";
		MapSqlParameterSource ps = new MapSqlParameterSource().addValue("uid", userId).addValue("t", type).addValue("k",
				keyOrMaybeKey);
		return jdbcTemplate.query(sql, ps, rs -> rs.next() ? mapRow(rs) : null);
	}

	private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
		Map<String, Object> m = new HashMap<>();
		m.put("report_id", rs.getLong("report_id"));
		m.put("patient_id", rs.getLong("patient_id"));
		m.put("content", rs.getString("content"));
		m.put("metrics", rs.getString("metrics"));
		m.put("sections", rs.getString("sections"));
		m.put("chart_prefs", rs.getString("chart_prefs"));
		m.put("period_type", rs.getString("period_type"));
		m.put("period_key", rs.getString("period_key"));
		m.put("start_date", rs.getObject("start_date", LocalDate.class));
		m.put("end_date", rs.getObject("end_date", LocalDate.class));
		return m;
	}

	/** 저장: period UPSERT 후 period_id 반환 */
	private Long upsertPeriod(String type, String key, LocalDate start, LocalDate end) {
		final String sql = "INSERT INTO period (period_type, period_key, start_date, end_date) "
				+ "VALUES (:t, :k, :s, :e) " + "ON CONFLICT (period_type, period_key) "
				+ "DO UPDATE SET start_date = EXCLUDED.start_date, " + "              end_date   = EXCLUDED.end_date, "
				+ "              updated_at = NOW() " + "RETURNING period_id";
		MapSqlParameterSource ps = new MapSqlParameterSource().addValue("t", type).addValue("k", key)
				.addValue("s", start).addValue("e", end);
		return jdbcTemplate.queryForObject(sql, ps, Long.class);
	}

	/** 저장: report UPSERT */
	private Long upsertReport(Long patientId, Long periodId, String type, String key,
			Map<String, Object> legacyPayload) {
		final String content = buildContentFromLegacy(legacyPayload);
		final String metricsJson = toJsonSafe(extractMetrics(legacyPayload));
		final String sectionsJson = toJsonSafe(extractSections(legacyPayload));
		final String chartJson = "{}";

		final String sql = "INSERT INTO report (" + "  period_id, patient_id, content, "
				+ "  period_type, period_key, version, generated_at, generated_by, locked, "
				+ "  metrics, sections, chart_prefs" + ") VALUES (" + "  :pid, :uid, :content, "
				+ "  :t, :k, 1, NOW(), 'api', FALSE, "
				+ "  CAST(:metrics AS jsonb), CAST(:sections AS jsonb), CAST(:charts AS jsonb)" + ") "
				+ "ON CONFLICT (patient_id, period_type, period_key) DO UPDATE SET "
				+ "  content     = EXCLUDED.content, " + "  metrics     = EXCLUDED.metrics, "
				+ "  sections    = EXCLUDED.sections, " + "  chart_prefs = EXCLUDED.chart_prefs, "
				+ "  updated_at  = NOW(), " + "  version     = report.version + 1 " + "RETURNING report_id";

		MapSqlParameterSource ps = new MapSqlParameterSource().addValue("pid", periodId).addValue("uid", patientId)
				.addValue("content", content).addValue("t", type).addValue("k", key).addValue("metrics", metricsJson)
				.addValue("sections", sectionsJson).addValue("charts", chartJson);

		return jdbcTemplate.queryForObject(sql, ps, Long.class);
	}

	/* ======================== 변환/빌드 유틸 ======================== */

	// 'yearly/monthly/weekly' → 'year/month/week'
	private String normalizePeriod(String p) {
		if (p == null)
			return "year";
		p = p.toLowerCase(Locale.ROOT);
		if (p.startsWith("year"))
			return "year";
		if (p.startsWith("month"))
			return "month";
		if (p.startsWith("week"))
			return "week";
		throw new IllegalArgumentException("지원하지 않는 period: " + p);
	}

	// 서비스에 넘길 라벨
	private String toApiPeriodLabel(String type) {
		switch (type) {
		case "year":
			return "yearly";
		case "month":
			return "monthly";
		default:
			return "weekly";
		}
	}

	// DB → 레거시 payload로 변환 (저장된 걸 그대로 화면 포맷으로)
	@SuppressWarnings("unchecked")
	private Map<String, Object> adaptStoredToLegacy(Map<String, Object> row, String type) {
		Map<String, Object> out = new HashMap<>();

		// range/period
		LocalDate s = (LocalDate) row.get("start_date");
		LocalDate e = (LocalDate) row.get("end_date");
		Map<String, Object> range = new HashMap<>();
		range.put("start", s != null ? s.toString() : null);
		range.put("end", e != null ? e.toString() : null);
		range.put("label", makeKey(type, s != null ? s : LocalDate.now()));
		out.put("range", range);
		out.put("period", toApiPeriodLabel(type));

		// metrics → score
		Map<String, Object> score = new HashMap<>();
		try {
			String metricsTxt = (String) row.get("metrics");
			if (metricsTxt != null && !metricsTxt.isBlank()) {
				Map<String, Object> metrics = objectMapper.readValue(metricsTxt, Map.class);
				if (metrics.get("cognitive") != null)
					score.put("cognitive", metrics.get("cognitive"));
				if (metrics.get("delta") != null)
					score.put("delta", metrics.get("delta"));
			}
		} catch (Exception ignored) {
		}
		out.put("score", score);

		// sections → weeklySummary / behaviorChanges
		try {
			String sectionsTxt = (String) row.get("sections");
			if (sectionsTxt != null && !sectionsTxt.isBlank()) {
				Map<String, Object> sections = objectMapper.readValue(sectionsTxt, Map.class);
				Object summary = sections.get("summary");
				Object behavior = sections.get("behavior");
				out.put("weeklySummary", summary != null ? String.valueOf(summary) : "");
				out.put("behaviorChanges", behavior != null ? String.valueOf(behavior) : "");
			} else {
				out.put("weeklySummary", "");
				out.put("behaviorChanges", "");
			}
		} catch (Exception e2) {
			out.put("weeklySummary", "");
			out.put("behaviorChanges", "");
		}

		// 나머지(activities/memory/recommendations/aiTip)는 저장 스키마에 없으면 생략/기본값
		out.putIfAbsent("activities", Map.of());
		out.putIfAbsent("memory", Map.of());
		out.putIfAbsent("recommendations", new String[0]);
		out.putIfAbsent("aiTip", Map.of());

		return out;
	}

	// period_key 생성 (YYYY / YYYY-MM / YYYY-Www)
	private String makeKey(String type, LocalDate start) {
		if ("year".equals(type))
			return String.format("%04d", start.getYear());
		if ("month".equals(type))
			return String.format("%04d-%02d", start.getYear(), start.getMonthValue());
		WeekFields wf = WeekFields.ISO; // week
		int isoYear = start.get(wf.weekBasedYear());
		int isoWeek = start.get(wf.weekOfWeekBasedYear());
		return String.format("%04d-W%02d", isoYear, isoWeek);
	}

	// 화면용 content 구성(미리보기 저장 시 사용)
	private String buildContentFromLegacy(Map<String, Object> p) {
		String summary = cleanHeading(str(p.get("weeklySummary")));
		String changes = cleanHeading(str(p.get("behaviorChanges")));
		String tipTitle = nestedStr(p, "aiTip", "title");
		String tipBody = nestedStr(p, "aiTip", "body");

		StringBuilder sb = new StringBuilder();
		if (!changes.isBlank()) {
			sb.append("## 행동 변화\n").append(changes).append("\n\n");
		}
		if (!summary.isBlank()) {
			sb.append("## 종합 요약\n").append(summary).append("\n\n");
		}
		if (!tipTitle.isBlank() || !tipBody.isBlank()) {
			sb.append("## AI Tip\n");
			if (!tipTitle.isBlank())
				sb.append("**").append(tipTitle).append("**\n");
			if (!tipBody.isBlank())
				sb.append(tipBody).append("\n");
		}
		if (sb.length() == 0)
			sb.append("[자동 저장된 요약 내용이 없습니다]");
		return sb.toString();
	}

	private String cleanHeading(String s) {
		if (s == null)
			return "";
		s = s.trim();
		return s.replaceFirst("^##\\s*[^\\n]+\\n?", "").trim();
	}

	private Map<String, Object> extractMetrics(Map<String, Object> p) {
		Object score = p.get("score");
		if (score instanceof Map) {
			Map<?, ?> sc = (Map<?, ?>) score;
			Map<String, Object> out = new HashMap<>();
			if (sc.get("cognitive") != null)
				out.put("cognitive", sc.get("cognitive"));
			if (sc.get("delta") != null)
				out.put("delta", sc.get("delta"));
			return out;
		}
		return Map.of();
	}

	private Map<String, Object> extractSections(Map<String, Object> p) {
		Map<String, Object> out = new HashMap<>();
		String summary = str(p.get("weeklySummary"));
		String changes = str(p.get("behaviorChanges"));
		if (!summary.isBlank())
			out.put("summary", summary);
		if (!changes.isBlank())
			out.put("behavior", changes);
		return out;
	}

	private String toJsonSafe(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj == null ? Map.of() : obj);
		} catch (JsonProcessingException e) {
			return "{}";
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	@SuppressWarnings("unchecked")
	private static String nestedStr(Map<String, Object> m, String k1, String k2) {
		if (m == null)
			return "";
		Object inner = m.get(k1);
		if (inner instanceof Map) {
			Object v = ((Map<String, Object>) inner).get(k2);
			return v == null ? "" : String.valueOf(v).trim();
		}
		return "";
	}
}
