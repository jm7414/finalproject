package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.LocationRequestDTO;
import lx.project.dementia_care.dto.LocationResponseDTO;
import lx.project.dementia_care.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * 환자 위치 업데이트 (임시 저장소에 저장)
     * POST /api/location/update
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateLocation(@RequestBody LocationRequestDTO request) {
        try {
            locationService.updateTemporaryLocation(request);
            return ResponseEntity.ok(Map.of("success", true, "message", "위치 업데이트 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "위치 업데이트 실패: " + e.getMessage()));
        }
    }

    /**
     * 환자 위치 조회 (보호자용)
     * GET /api/location/patient/{patientUserNo}
     * [시연용] 항상 고정 좌표 반환
     */
    @GetMapping("/patient/{patientUserNo}")
    public ResponseEntity<LocationResponseDTO> getPatientLocation(
            @PathVariable Integer patientUserNo, 
            Authentication authentication) {
        try {
            // TODO: authentication에서 보호자 정보 추출하여 해당 환자에 대한 권한 확인
            // 현재는 임시로 모든 요청 허용 (나중에 보안 강화 필요)
            
            // [시연용] 항상 고정 좌표 반환 (null 반환 없음)
            LocationResponseDTO location = locationService.getPatientLocation(patientUserNo);
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


}
