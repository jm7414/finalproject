// src/main/java/lx/project/dementia_care/controller/DailyRecordController.java
package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import lx.project.dementia_care.service.DailyRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 일별 기록 API
 */
@RestController
@RequestMapping("/api/record")
public class DailyRecordController {

    private final DailyRecordService service;

    public DailyRecordController(DailyRecordService service) {
        this.service = service;
    }

    /**
     * 생성 또는 업데이트
     */
    @PostMapping
    public ResponseEntity<DailyRecordResponseDTO> saveOrUpdate(
            @RequestBody DailyRecordRequestDTO req) {
        DailyRecordResponseDTO res = service.saveOrUpdate(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * 사용자ID+날짜로 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<DailyRecordResponseDTO> getByUserAndDate(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyRecordResponseDTO res = service.getByUserAndDate(userId, date);
        return ResponseEntity.ok(res);
    }
}
