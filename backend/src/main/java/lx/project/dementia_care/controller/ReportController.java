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
	 * 통합 엔드포인트 period=daily & date=YYYY-MM-DD → Map(이모지 응답) period=weekly &
	 * start,end → ReportVO (저장본 우선, force 재생성 가능) period=monthly & start,end →
	 * ReportVO (저장본 우선, force 재생성 가능) period=yearly & start,end → Map(연간
	 * totals/series/details/ai)
	 */
	@GetMapping("/api/ai/report")
	public ResponseEntity<?> getReport(@RequestParam Long userId, @RequestParam String period,
			@RequestParam(required = false) String start, @RequestParam(required = false) String end,
			@RequestParam(required = false) String date, @RequestParam(defaultValue = "loadOrCreate") String mode,
			@RequestParam(required = false) String generatedBy,
			@RequestParam(required = false, defaultValue = "false") boolean force) {
		final String p = period.toLowerCase(Locale.ROOT);

		// 일간: 오늘/특정일 이모지
		if ("daily".equals(p)) {
			LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
			Map<String, Object> emojiResp = reportService.buildDailyEmoji(userId, d);
			return ResponseEntity.ok(emojiResp);
		}

		// 주/월/연: start/end 필수
		if (start == null || end == null) {
			return ResponseEntity.badRequest().body(
					Map.of("error", "start/end 파라미터가 필요합니다.", "hint", "예: start=2025-11-03&end=2025-11-10 (end는 미포함)"));
		}
		LocalDate s = LocalDate.parse(start);
		LocalDate e = LocalDate.parse(end);

		// 연간
		if ("yearly".equals(p)) {
			Map<String, Object> extras = reportService.buildYearlyExtras(userId, s, e);
			return ResponseEntity.ok(extras);
		}

		// 주/월: weekly → WEEK, monthly → MONTH 로 매핑
		final String normType;
		if ("weekly".equals(p))
			normType = "WEEK";
		else if ("monthly".equals(p))
			normType = "MONTH";
		else
			normType = p.toUpperCase(Locale.ROOT); // 혹시 모를 확장 대비

		String periodKey = reportService.makePeriodKey(normType, s, e);

		// ✅ 여기서 force 전달 (7번째 인자)
		ReportVO vo = reportService.loadOrCreate(userId, normType, periodKey, s, e,
				(generatedBy != null && !generatedBy.isBlank()) ? generatedBy : "api", force);

		if (vo == null) {
			// 커버리지 부족 → 프론트 안내용
			int expected = (int) java.time.temporal.ChronoUnit.DAYS.between(s, e);
			int covered = reportService.countCoveredDays(userId, s, e);
			return ResponseEntity
					.ok(Map.of("eligibility", "INSUFFICIENT", "expectedDays", expected, "coveredDays", covered));
		}
		return ResponseEntity.ok(vo);
	}
}
