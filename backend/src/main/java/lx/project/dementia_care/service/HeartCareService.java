// src/main/java/lx/project/dementia_care/service/HeartCareService.java
package lx.project.dementia_care.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lx.project.dementia_care.dto.HeartCareDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeartCareService {

    private final RestTemplate restTemplate;

    // ✅ 하드코딩: API 엔드포인트
    private final String baseUrl = "https://api.odcloud.kr/api/3049990/v1/uddi:14a6ea21-af95-4440-bb05-81698f7a1987";

    // ✅ 하드코딩: 서비스 키
    private final String serviceKey = "be9d2baa477b5d2d91d080f3aa4736d87a804bb791c9f101f46e14849af8b0da";

    /**
     * 정신건강 상담소 목록 조회
     * @param page 페이지 번호
     * @param perPage 페이지당 항목 수
     * @param keyword 검색 키워드
     * @return 상담소 목록
     */
    public List<HeartCareDTO> getCounselingCenters(int page, int perPage, String keyword) {
        try {
            URI targetUrl = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("returnType", "JSON")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

            log.info("[HeartCareService] Calling API: {}", targetUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(targetUrl, String.class);
            String body = response.getBody();
            
            log.info("[HeartCareService] Response status: {}", response.getStatusCodeValue());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataArray = root.path("data");

            List<HeartCareDTO> centers = new ArrayList<>();
            
            for (JsonNode node : dataArray) {
                HeartCareDTO dto = new HeartCareDTO();
                dto.setInstitutionName(node.path("기관명").asText());
                dto.setInstitutionType(node.path("기관구분").asText());
                dto.setAddress(node.path("주소").asText());
                dto.setPhone(node.path("전화번호").asText());
                dto.setHomepage(node.path("홈페이지주소").asText());
                
                // 키워드 필터링
                if (keyword == null || keyword.isEmpty() || 
                    dto.getInstitutionName().contains(keyword) || 
                    dto.getAddress().contains(keyword)) {
                    centers.add(dto);
                }
            }
            
            log.info("[HeartCareService] Found {} centers", centers.size());
            return centers;
            
        } catch (Exception e) {
            log.error("[HeartCareService] Error calling API", e);
            throw new RuntimeException("정신건강 상담소 데이터 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 지역별 필터링
     */
    public List<HeartCareDTO> getCentersByRegion(String region) {
        return getCounselingCenters(1, 3000, region);
    }
}
