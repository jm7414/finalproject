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

	@GetMapping("/api/ai/report")
	public ResponseEntity<?> getReport(@RequestParam Long userId, @RequestParam String period,
			@RequestParam(required = false) String start, @RequestParam(required = false) String end,
			@RequestParam(required = false) String date, @RequestParam(defaultValue = "loadOrCreate") String mode,
			@RequestParam(required = false) String generatedBy,
			@RequestParam(required = false, defaultValue = "false") boolean force) {
		final String p = period.toLowerCase(Locale.ROOT);

		// 일간
		if ("daily".equals(p)) {
			LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
			Map<String, Object> emojiResp = reportService.buildDailyEmoji(userId, d);
			return ResponseEntity.ok(emojiResp);
		}

		// 공통: start/end
		if (start == null || end == null) {
			return ResponseEntity.badRequest().body(
					Map.of("error", "start/end 파라미터가 필요합니다.", "hint", "예: start=2025-11-03&end=2025-11-10 (end는 미포함)"));
		}
		LocalDate s = LocalDate.parse(start);
		LocalDate e = LocalDate.parse(end);

		// 연간: YEAR도 “처음만 생성, 이후 DB 고정(+부족 월은 스케치 보강)”
		if ("yearly".equals(p)) {
			try {
				String yearKey = reportService.makePeriodKey("YEAR", s, e);
				reportService.loadOrCreate(userId, "YEAR", yearKey, s, e,
						(generatedBy != null && !generatedBy.isBlank()) ? generatedBy : "api", false // 캐시만 사용
				);
			} catch (Exception ignore) {
			}
			Map<String, Object> extras = reportService.buildYearlyExtras(userId, s, e);
			return ResponseEntity.ok(extras);
		}

		// 주/월
		final String normType = "weekly".equals(p) ? "WEEK"
				: ("monthly".equals(p) ? "MONTH" : p.toUpperCase(Locale.ROOT));
		String periodKey = reportService.makePeriodKey(normType, s, e);

		ReportVO vo = reportService.loadOrCreate(userId, normType, periodKey, s, e,
				(generatedBy != null && !generatedBy.isBlank()) ? generatedBy : "api", force /* 기본 false → 저장본 우선 */
		);

		if (vo == null) {
			int expected = (int) java.time.temporal.ChronoUnit.DAYS.between(s, e);
			int covered = reportService.countCoveredDays(userId, s, e);
			return ResponseEntity
					.ok(Map.of("eligibility", "INSUFFICIENT", "expectedDays", expected, "coveredDays", covered));
		}
		return ResponseEntity.ok(vo);
	}
}
