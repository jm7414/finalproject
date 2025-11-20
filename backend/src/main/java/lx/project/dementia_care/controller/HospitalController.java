// src/main/java/lx/project/dementia_care/controller/HospitalController.java
package lx.project.dementia_care.controller;

import lx.project.dementia_care.service.HospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * 예:
     * GET /api/hospital/near
     * GET /api/hospital/near?centerLat=37.49&centerLng=126.88&radiusKm=5
     */
    @GetMapping("/api/hospital/near")
    public List<HospitalService.HospitalInfo> getHospitalsNear(
            @RequestParam(required = false) Double centerLat,
            @RequestParam(required = false) Double centerLng,
            @RequestParam(required = false) Double radiusKm
    ) {
        log.info("[HospitalController] /api/hospital/near 요청 centerLat={}, centerLng={}, radiusKm={}",
                centerLat, centerLng, radiusKm);

        return hospitalService.findHospitalsNear(centerLat, centerLng, radiusKm);
    }
}
