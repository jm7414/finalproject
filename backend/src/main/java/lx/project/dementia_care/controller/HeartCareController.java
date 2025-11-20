// src/main/java/lx/project/dementia_care/controller/HeartCareController.java
package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.HeartCareDTO;
import lx.project.dementia_care.service.HeartCareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/heart-care")
@RequiredArgsConstructor
@Slf4j
public class HeartCareController {

    private final HeartCareService heartCareService;

    /**
     * 정신건강 상담소 목록 조회
     * GET /api/heart-care/centers?page=1&perPage=10&keyword=화성
     */
    @GetMapping("/centers")
    public ResponseEntity<?> getCounselingCenters(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int perPage,
        @RequestParam(required = false) String keyword
    ) {
        try {
            log.info("[HeartCareController] Request - page: {}, perPage: {}, keyword: {}", 
                     page, perPage, keyword);
            
            List<HeartCareDTO> centers = heartCareService.getCounselingCenters(page, perPage, keyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", centers);
            response.put("total", centers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[HeartCareController] Error", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 지역별 상담소 조회
     * GET /api/heart-care/centers/region?region=경기도
     */
    @GetMapping("/centers/region")
    public ResponseEntity<?> getCentersByRegion(@RequestParam String region) {
        try {
            log.info("[HeartCareController] Region request: {}", region);
            
            List<HeartCareDTO> centers = heartCareService.getCentersByRegion(region);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", centers);
            response.put("total", centers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[HeartCareController] Error", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
