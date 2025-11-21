// src/main/java/lx/project/dementia_care/service/HospitalService.java
package lx.project.dementia_care.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class HospitalService {

    // 🔒 여기다 그냥 하드코딩 쓸 거라서 application.properties 안 씀
    private static final String SERVICE_KEY =
            "b03a0ee420c5d5aba5fc9890ca90ccfd36f621e004029f5147fe2c349bbbcd6b";

    private static final String BASE_URL =
            "https://apis.data.go.kr/B551182/hospInfoServicev2/getHospBasisList";

    // 기본 기준점: 서울 구로구청 근처 좌표
    private static final double DEFAULT_CENTER_LAT = 37.495985;
    private static final double DEFAULT_CENTER_LNG = 126.887560;
    private static final double DEFAULT_RADIUS_KM = 10.0;

    // --- 외부에서 쓰는 메인 메서드 ----------------------------------------

    /**
     * 구로구청 기준 반경 radiusKm km 이내 병원 목록 조회 (거리순 정렬)
     */
    public List<HospitalInfo> findHospitalsNear(Double centerLat,
                                                Double centerLng,
                                                Double radiusKm) {

        double lat = centerLat != null ? centerLat : DEFAULT_CENTER_LAT;
        double lng = centerLng != null ? centerLng : DEFAULT_CENTER_LNG;
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : DEFAULT_RADIUS_KM;

        log.info("[HospitalService] 병원 근처 조회 lat={}, lng={}, radiusKm={}", lat, lng, radius);

        String url = BASE_URL
                + "?serviceKey=" + SERVICE_KEY
                + "&sgguCd=110005"         // 서울 구로구 코드
                + "&pageNo=1"
                + "&numOfRows=500"
                + "&_type=json";           // 🔴 JSON으로 강제

        String body;
        try {
            RestTemplate rt = new RestTemplate();
            log.info("[HospitalService] 병원 API 호출: {}", url);
            body = rt.getForObject(url, String.class);
        } catch (HttpStatusCodeException e) {
            log.error("[HospitalService] 병원 API HTTP 오류 status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "병원 API 호출 실패(" + e.getStatusCode().value() + ")"
            );
        } catch (RestClientException e) {
            log.error("[HospitalService] 병원 API 호출 중 예외", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "병원 API 호출 중 오류가 발생했습니다."
            );
        }

        if (body == null || body.isBlank()) {
            log.error("[HospitalService] 병원 API 응답이 비어있음");
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "병원 API 응답이 비어 있습니다."
            );
        }

        String snippet = body.length() > 300 ? body.substring(0, 300) : body;
        log.info("[HospitalService] raw 응답 snippet: {}", snippet);

        // 🔴 이제부터는 XML 말고 JSON으로만 파싱
        List<HospitalInfo> all = parseHospitalsFromJson(body);

        // 거리 계산 + 반경 필터링 + 정렬
        List<HospitalInfo> result = new ArrayList<>();
        for (HospitalInfo h : all) {
            if (Double.isNaN(h.lat) || Double.isNaN(h.lng)) {
                continue;
            }
            h.distanceKm = calcDistanceKm(lat, lng, h.lat, h.lng);
            if (h.distanceKm <= radius) {
                result.add(h);
            }
        }

        result.sort(Comparator.comparingDouble(h -> h.distanceKm));

        // 너무 많으면 적당히 자르기 (원하면 숫자 조절)
        if (result.size() > 100) {
            result = result.subList(0, 100);
        }

        log.info("[HospitalService] 필터링 후 병원 수: {}", result.size());
        return result;
    }

    // --- JSON 파싱 -------------------------------------------------------

    private List<HospitalInfo> parseHospitalsFromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode itemsNode = root
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            List<HospitalInfo> list = new ArrayList<>();

            if (itemsNode.isMissingNode() || !itemsNode.isArray()) {
                log.warn("[HospitalService] items 노드가 비어 있음 또는 배열이 아님");
                return list;
            }

            for (JsonNode node : itemsNode) {
                String name = node.path("yadmNm").asText(null);
                if (name == null || name.isBlank()) continue;

                String addr = node.path("addr").asText("");
                String tel = node.path("telno").asText("");

                double x = node.path("XPos").asDouble(Double.NaN); // 경도
                double y = node.path("YPos").asDouble(Double.NaN); // 위도

                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
                }

                HospitalInfo info = new HospitalInfo();
                info.name = name;
                info.address = addr;
                info.tel = tel;
                info.lng = x;
                info.lat = y;

                list.add(info);
            }

            log.info("[HospitalService] JSON 파싱 결과 병원 수: {}", list.size());
            return list;
        } catch (JsonProcessingException e) {
            log.error("[HospitalService] JSON 파싱 실패", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "병원 데이터 파싱 중 오류가 발생했습니다."
            );
        }
    }

    // --- 거리 계산 (하버사인) --------------------------------------------

    private double calcDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0; // 지구 반지름 km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // --- 프론트로 내려보낼 DTO ------------------------------------------

    /**
     * Jackson이 바로 JSON으로 만들어줄 단순 DTO
     */
    public static class HospitalInfo {
        public String name;
        public String address;
        public String tel;
        public double lat;
        public double lng;
        public double distanceKm;
    }
}
