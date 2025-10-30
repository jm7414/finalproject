// src/main/java/lx/project/dementia_care/controller/ReportController.java
package lx.project.dementia_care.controller;

import lx.project.dementia_care.dao.ReportDAO;
import lx.project.dementia_care.service.ReportService;
import lx.project.dementia_care.vo.ReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ReportController {

	@Autowired
	private ReportService reportService;

	@Autowired
	private ReportDAO reportDAO;

	/**
	 * 단일 엔드포인트 - period: "weekly" | "monthly" | "yearly" - start, end: yyyy-MM-dd
	 * (end는 '미포함' = exclusive) - mode: 기본 "loadOrCreate"
	 */
	@GetMapping("/api/ai/report")
	public ResponseEntity<?> getReport(@RequestParam("userId") Long userId, @RequestParam("period") String period, // weekly
																													// |
																													// monthly
																													// |
																													// yearly
			@RequestParam("start") String startIso, // yyyy-MM-dd
			@RequestParam("end") String endIso, // yyyy-MM-dd (exclusive)
			@RequestParam(value = "mode", required = false, defaultValue = "loadOrCreate") String mode) {
		final LocalDate start;
		final LocalDate endExclusive;
		try {
			start = LocalDate.parse(startIso);
			endExclusive = LocalDate.parse(endIso);
		} catch (DateTimeParseException e) {
			return ResponseEntity.badRequest().body("날짜 파라미터 형식 오류(yyyy-MM-dd): " + e.getParsedString());
		}

		// ===== 연간: totals/series (프론트에서 yearly 그래프/평균에 사용) =====
		if ("yearly".equalsIgnoreCase(period)) {
			Map<String, Object> yearly = reportService.buildYearlyExtras(userId, start, endExclusive);
			// 연간 details(AI)까지 원하면 여기서 추가 생성해 넣을 수 있음. (지금은 옵션)
			return ResponseEntity.ok(yearly);
		}

		// ===== 주/월 공통 처리 =====
		final String periodType;
		final String periodKey;

		if ("weekly".equalsIgnoreCase(period)) {
			periodType = "week";
			periodKey = isoWeekKey(start); // 예: 2025-W44
		} else if ("monthly".equalsIgnoreCase(period)) {
			periodType = "month";
			periodKey = YearMonth.from(start).toString(); // 예: 2025-10
		} else {
			return ResponseEntity.badRequest().body("period는 weekly/monthly/yearly 중 하나여야 합니다.");
		}

		// 1) 현재 구간 load or create
		ReportVO cur = reportService.loadOrCreate(userId, periodType, periodKey, start, endExclusive, "api");

		// 2) 현재(0~100) 합계
		int currentScore = reportService.getTotalScore0to100FromJson(cur.getMetrics());

		// 3) 직전 기간 키/범위 계산
		LocalDate prevStart, prevEndExclusive;
		String prevPeriodKey;

		if ("week".equals(periodType)) {
			prevStart = start.minusWeeks(1);
			prevEndExclusive = endExclusive.minusWeeks(1);
			prevPeriodKey = isoWeekKey(prevStart);
		} else { // month
			YearMonth ym = YearMonth.from(start);
			YearMonth prevYm = ym.minusMonths(1);
			prevStart = prevYm.atDay(1);
			prevEndExclusive = prevYm.plusMonths(1).atDay(1);
			prevPeriodKey = prevYm.toString();
		}

		// 4) 직전 리포트 찾아서 점수(없으면 current로 대체 → delta=0)
		ReportVO prev = reportDAO.findByPatientPeriod(userId, periodType, prevPeriodKey);
		int prevScore = (prev != null) ? reportService.getTotalScore0to100FromJson(prev.getMetrics()) : currentScore;
		int delta = currentScore - prevScore;

		// 5) 프론트 기대 응답 포맷 구성
		Map<String, Object> range = new LinkedHashMap<>();
		range.put("start", start.toString());
		// 표시용: endExclusive-1일
		range.put("end", endExclusive.minusDays(1).toString());
		range.put("label", rangeLabel(periodType, start, endExclusive));

		Map<String, Object> score = new LinkedHashMap<>();
		score.put("cognitive", currentScore); // 0~100 합
		score.put("delta", delta); // 이전 대비

		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("periodType", periodType);
		resp.put("periodKey", periodKey);
		resp.put("range", range);
		resp.put("metrics", cur.getMetrics()); // 문자열(JSON) 그대로
		resp.put("sections", cur.getSections()); // 문자열(JSON) 그대로 (details 포함)
		resp.put("chartPrefs", cur.getChartPrefs()); // 문자열(JSON) 그대로
		resp.put("score", score);

		return ResponseEntity.ok(resp);
	}

	/* ==================== 헬퍼 ==================== */

	/** ISO 주차 키: "YYYY-Www" (ISO 주차/ISO 주차연) */
	private String isoWeekKey(LocalDate anyDay) {
		WeekFields wf = WeekFields.ISO;
		int isoYear = anyDay.get(wf.weekBasedYear());
		int isoWeek = anyDay.get(wf.weekOfWeekBasedYear());
		return String.format("%04d-W%02d", isoYear, isoWeek);
	}

	/** 표시용 라벨: week -> "YYYY.MM.DD ~ M.D", month -> "YYYY년 M월" */
	private String rangeLabel(String type, LocalDate start, LocalDate endExclusive) {
		if ("week".equals(type)) {
			LocalDate end = endExclusive.minusDays(1);
			return String.format("%d.%02d.%02d ~ %d.%02d.%02d", start.getYear(), start.getMonthValue(),
					start.getDayOfMonth(), end.getYear(), end.getMonthValue(), end.getDayOfMonth());
		} else if ("month".equals(type)) {
			YearMonth ym = YearMonth.from(start);
			return String.format("%d년 %d월", ym.getYear(), ym.getMonthValue());
		}
		return "";
	}
}
