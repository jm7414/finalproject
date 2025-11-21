package lx.project.dementia_care.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lx.project.dementia_care.dto.TotalSupportDTO;
import lx.project.dementia_care.service.TotalSupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 지자체 복지서비스 종합지원 컨트롤러
 *  - /api/support/welfare : 프론트(TotalSupport.vue, Benefit.vue)가 호출하는 엔드포인트
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TotalSupportController {

    private final TotalSupportService totalSupportService;

    /**
     * 프론트에서 호출하는 지자체 복지서비스 조회 엔드포인트
     *
     * 예)
     *  GET /api/support/welfare?localGovNm=서울특별시&pageNo=1&numOfRows=500
     */
    @GetMapping("/api/support/welfare")
    public ResponseEntity<?> getWelfare(@ModelAttribute TotalSupportDTO dto) {
        log.info("[TotalSupportController] 요청: {}", dto);

        try {
            JsonNode result = totalSupportService.fetchWelfareList(dto);
            // result 안에 이미: totalCount, pageNo, numOfRows, resultCode, resultMessage, servList[...]
            // 프론트(Benefit.vue)는 data.servList 기준으로 사용
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[TotalSupportController] 복지서비스 조회 실패", e);

            // 프론트에서 data.success === false 체크할 수 있게 형식 통일
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            new ErrorResponse(
                                    false,
                                    "지자체 복지서비스 조회 중 오류가 발생했습니다.",
                                    e.getMessage()
                            )
                    );
        }
    }

    /**
     * 간단한 에러 응답용 내부 클래스
     */
    private record ErrorResponse(
            boolean success,
            String message,
            String error
    ) {
    }
}
