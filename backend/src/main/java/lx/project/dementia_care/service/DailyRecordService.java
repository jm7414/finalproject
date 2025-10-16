// src/main/java/lx/project/dementia_care/service/DailyRecordService.java
package lx.project.dementia_care.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lx.project.dementia_care.dao.DailyRecordDAO;
import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyRecordService {

    private final DailyRecordDAO recordDAO;
    private final ObjectMapper objectMapper;  // JSON 직렬화/역직렬화 - json 처리에 사용

    /**
     * 일별 기록 저장 또는 업데이트
     */
    @Transactional
    public DailyRecordResponseDTO saveOrUpdate(DailyRecordRequestDTO req) {
        Long userId = req.getUserId();
        LocalDate date = req.getRecordDate();

        // 기존 레코드 조회
        Optional<DailyRecordResponseDTO> existing = recordDAO.findByUserIdAndDate(userId, date);
        if (existing.isPresent()) {
            // 업데이트용 DTO로 변환
            recordDAO.update(req);
            return recordDAO.findByUserIdAndDate(userId, date).get();
        } else {
            // 신규 저장
            recordDAO.save(req);
            return recordDAO.findByUserIdAndDate(userId, date).get();
        }
    }

    /**
     * 사용자와 날짜로 기록 조회
     */
    @Transactional(readOnly = true)
    public DailyRecordResponseDTO getByUserAndDate(Long userId, LocalDate date) {
        return recordDAO.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 기록이 없습니다"));
    }
}
