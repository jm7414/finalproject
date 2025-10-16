// src/main/java/lx/project/dementia_care/dao/DailyRecordDAO.java
package lx.project.dementia_care.dao;

import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Optional;

@Mapper  // MyBatis 매퍼
public interface DailyRecordDAO {

    // 사용자ID, 날짜로 조회
    Optional<DailyRecordResponseDTO> findByUserIdAndDate(
        @Param("userId") Long userId,
        @Param("recordDate") LocalDate recordDate
    );

    // 레코드ID로 조회
    Optional<DailyRecordResponseDTO> findById(@Param("recordId") Long recordId);

    // 신규 저장
    void save(DailyRecordRequestDTO dto);

    // 수정
    void update(DailyRecordRequestDTO dto);

    // 삭제
    void deleteById(@Param("recordId") Long recordId);
}
