package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.RouteRequest;
import lx.project.dementia_care.dto.RouteResponse;
import lx.project.dementia_care.service.TmapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    @Autowired
    private TmapService tmapService;

    @PostMapping("/pedestrian")
    public ResponseEntity<RouteResponse> pedestrian(@RequestBody RouteRequest req) {
        return ResponseEntity.ok(tmapService.getRouteCoordinates(req));
    }
}
