package lx.project.dementia_care.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lx.project.dementia_care.dto.CoordinateDto;
import lx.project.dementia_care.dto.RouteRequest;
import lx.project.dementia_care.dto.RouteResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TmapService {

    @Value("${tmap.api.key}")
    private String apiKey;

    @Value("${tmap.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public RouteResponse getRouteCoordinates(RouteRequest req) {
        // 1) URL
        String url = baseUrl + "/routes/pedestrian?version=1";

        // 2) 요청 바디: DTO 필드 사용
        Map<String,Object> body = new HashMap<>();
        body.put("startName",    req.getStartName());
        body.put("startX",       req.getStartX());
        body.put("startY",       req.getStartY());
        body.put("endName",      req.getEndName());
        body.put("endX",         req.getEndX());
        body.put("endY",         req.getEndY());
        body.put("reqCoordType", req.getReqCoordType());
        body.put("resCoordType", req.getResCoordType());
        body.put("searchOption", req.getSearchOption());

        // 3) 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", apiKey);

        // 4) Request
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(body, headers);
        String resp = restTemplate.postForObject(url, entity, String.class);

        // 5) Parse JSON → List<CoordinateDto>
        List<CoordinateDto> coords = new ArrayList<>();
        try {
            JsonNode features = mapper.readTree(resp).get("features");
            for (JsonNode feature : features) {
                if ("LineString".equals(feature.get("geometry").get("type").asText())) {
                    for (JsonNode pt : feature.get("geometry").get("coordinates")) {
                        double lng = pt.get(0).asDouble();
                        double lat = pt.get(1).asDouble();
                        coords.add(new CoordinateDto(lat, lng));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("파싱 오류", e);
        }

        return new RouteResponse(coords);
    }
}
