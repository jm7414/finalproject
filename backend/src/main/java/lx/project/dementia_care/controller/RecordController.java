// src/main/java/lx/project/dementia_care/controller/RecordController.java
package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import lx.project.dementia_care.service.RecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record")
public class RecordController {

	private final RecordService recordService;

	public RecordController(RecordService recordService) {
		this.recordService = recordService;
	}

	/** (user_id, record_date) 기준 업서트 */
	@PostMapping
	public ResponseEntity<DailyRecordResponseDTO> upsert(@RequestBody DailyRecordRequestDTO req) {
		DailyRecordResponseDTO saved = recordService.upsert(req);
		// 새로 만들었든 갱신했든 201로 응답(프론트는 saved.id 등 활용)
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	/** 단건 조회: /api/record/user/{userId}?date=YYYY-MM-DD */
	@GetMapping(value = "/user/{userId}", params = "date")
	public ResponseEntity<DailyRecordResponseDTO> getOne(@PathVariable Long userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		DailyRecordResponseDTO one = recordService.getOne(userId, date);
		if (one == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(one);
	}

	/** 기간 조회: /api/record/user/{userId}?start=YYYY-MM-DD&end=YYYY-MM-DD */
	@GetMapping(value = "/user/{userId}", params = { "start", "end" })
	public ResponseEntity<List<DailyRecordResponseDTO>> getRange(@PathVariable Long userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		List<DailyRecordResponseDTO> list = recordService.getRange(userId, start, end);
		return ResponseEntity.ok(list);
	}

	/** 잘못된 요청 파라미터 등에 대한 400 응답(서비스에서 IllegalArgumentException 던진 경우) */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(e.getMessage());
	}
}
