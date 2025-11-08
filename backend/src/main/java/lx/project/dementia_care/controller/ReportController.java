// src/main/java/lx/project/dementia_care/controller/ReportController.java
package lx.project.dementia_care.controller;

import lombok.RequiredArgsConstructor;
import lx.project.dementia_care.service.ReportService;
import lx.project.dementia_care.vo.ReportVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	/**
	 * 통합 엔드포인트 period=daily & date=YYYY-MM-DD → 이모지 응답(Map) period=weekly &
	 * start,end → ReportVO period=monthly & start,end → ReportVO period=yearly &
	 * start,end → Map(연간 totals/series/details/ai) mode=loadOrCreate (기본)
	 */
	@GetMapping("/api/ai/report")
	public ResponseEntity<?> getReport(@RequestParam Long userId, @RequestParam String period,
			@RequestParam(required = false) String start, @RequestParam(required = false) String end,
			@RequestParam(required = false) String date, @RequestParam(defaultValue = "loadOrCreate") String mode,
			@RequestParam(required = false) String generatedBy) {
		String p = period.toLowerCase(Locale.ROOT);

		if ("daily".equals(p)) {
			LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
			Map<String, Object> emojiResp = reportService.buildDailyEmoji(userId, d);
			return ResponseEntity.ok(emojiResp);
		}

		// weekly / monthly / yearly
		if (start == null || end == null) {
			return ResponseEntity.badRequest().body(
					Map.of("error", "start/end 파라미터가 필요합니다.", "hint", "예: start=2025-11-03&end=2025-11-10 (end는 미포함)"));
		}
		LocalDate s = LocalDate.parse(start);
		LocalDate e = LocalDate.parse(end);

		if ("yearly".equals(p)) {
			Map<String, Object> extras = reportService.buildYearlyExtras(userId, s, e);
			return ResponseEntity.ok(extras);
		}

		// weekly / monthly
		// periodType은 WEEK/MONTH 키로 통일, key는 프론트가 구성해서 넘기든 컨트롤러가 만들든 상관없음.
		String normType = "WEEK".equalsIgnoreCase(period) ? "WEEK" : "MONTH";
		String periodKey = reportService.makePeriodKey(normType, s, e);

		ReportVO vo = reportService.loadOrCreate(userId, normType, periodKey, s, e,
				(generatedBy != null && !generatedBy.isBlank()) ? generatedBy : "api");
		if (vo == null) {
			// 커버리지 부족 시 프론트가 그대로 안내문 띄우도록
			return ResponseEntity.ok(Map.of("eligibility", "INSUFFICIENT", "expectedDays",
					(int) java.time.temporal.ChronoUnit.DAYS.between(s, e), "coveredDays",
					reportService.countCoveredDays(userId, s, e)));
		}
		return ResponseEntity.ok(vo);
	}
}
