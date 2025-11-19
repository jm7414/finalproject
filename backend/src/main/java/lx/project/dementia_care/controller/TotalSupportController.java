// src/main/java/lx/project/dementia_care/controller/TotalSupportController.java
package lx.project.dementia_care.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lx.project.dementia_care.dto.TotalSupportDTO;
import lx.project.dementia_care.service.TotalSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 종합지원 > 지자체 복지서비스 조회용 컨트롤러
 * 프론트에서 /api/support/welfare 로 호출해서 사용
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/support")
public class TotalSupportController {

    private final TotalSupportService totalSupportService;

    /**
     * 예: GET /api/support/welfare?localGovNm=서울특별시%20강남구&pageNo=1&numOfRows=10
     *
     * 프론트에서는 axios.get('/api/support/welfare', { params: {...} }) 형태로 호출하면 됨.
     */
    @GetMapping("/welfare")
    public ResponseEntity<Map<String, Object>> getLocalWelfareList(TotalSupportDTO req) {
        log.info("[TotalSupportController] 요청: {}", req);
        Map<String, Object> result = totalSupportService.searchSupports(req);
        return ResponseEntity.ok(result);
    }
}
