// src/main/java/lx/project/dementia_care/service/TotalSupportService.java
package lx.project.dementia_care.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lx.project.dementia_care.dto.TotalSupportDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 공공데이터포털 "한국사회보장정보원_지자체복지서비스"를 호출하는 서비스.
 *
 * - Base URL:
 *   https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations/LcgvWelfarelist
 *
 * - 대표 필수 파라미터:
 *   serviceKey, pageNo, numOfRows
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotalSupportService {

    private final RestTemplate restTemplate;

    // ✅ 하드코딩: 엔드포인트 URL (https)
    private final String baseUrl =
            "https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations/LcgvWelfarelist";

    // ✅ 하드코딩: 인코딩된 서비스키 (data.go.kr에서 발급된 Encoding 키 그대로 넣기)
    // 예: "aBcDeF%2Fghijklmn%3D%3D" 이런 긴 문자열
    private final String serviceKey =
            "b03a0ee420c5d5aba5fc9890ca90ccfd36f621e004029f5147fe2c349bbbcd6b";

    /**
     * 지자체 복지서비스 목록 조회
     * 
     * - 지자체복지서비스 API는 XML 포맷으로 응답.
     * - 여기서는 XML 문자열을 그대로 반환해서 프론트에서 파싱하게 함.
     * - 외부 API에서 오류가 나도 서버 500을 던지지 않고,
     *   upstreamStatus / xml / error 를 JSON에 담아서 반환.
     */
    public Map<String, Object> searchSupports(TotalSupportDTO req) {

        // 1) URL + 쿼리 파라미터 조립
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                // ✅ 필수 파라미터: serviceKey (소문자 s)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", req.getSafePageNo())
                .queryParam("numOfRows", req.getSafeNumOfRows());

        // === 선택 필터들: 실제 OpenAPI 문서의 파라미터 명에 맞춰서 조정 가능 ===
        if (StringUtils.hasText(req.getLocalGovNm())) {
            builder.queryParam("localGovNm", req.getLocalGovNm());
        }
        if (StringUtils.hasText(req.getLifeArray())) {
            builder.queryParam("lifeArray", req.getLifeArray());
        }
        if (StringUtils.hasText(req.getCharTrgterArray())) {
            builder.queryParam("charTrgterArray", req.getCharTrgterArray());
        }
        if (StringUtils.hasText(req.getObstrTyArray())) {
            builder.queryParam("obstrTyArray", req.getObstrTyArray());
        }
        if (StringUtils.hasText(req.getTrgterIndvdlArray())) {
            builder.queryParam("trgterIndvdlArray", req.getTrgterIndvdlArray());
        }
        if (StringUtils.hasText(req.getSprtBizNm())) {
            builder.queryParam("sprtBizNm", req.getSprtBizNm());
        }

        URI uri = builder
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        log.info("[TotalSupportService] calling welfare API: {}", uri);

        Map<String, Object> result = new HashMap<>();

        try {
            // 2) 호출 (XML 문자열로 받아옴)
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String body = response.getBody();
            int status = response.getStatusCodeValue();
            
            log.info("[TotalSupportService] upstreamStatus={}, bodySnippet={}",
                    status,
                    body != null ? body.substring(0, Math.min(body.length(), 200)) : "null");

            log.debug("[TotalSupportService] status={}, raw body={}", status, body);

            // ✅ 2xx가 아니어도 그대로 내려보냄
            result.put("upstreamStatus", status);
            result.put("xml", body);

        } catch (Exception e) {
            // 네트워크 오류, 인증 문제 등
            log.error("[TotalSupportService] error while calling welfare API", e);

            result.put("upstreamStatus", 500);
            result.put("xml", null);
            result.put("error", e.getMessage());
        }

        return result;
    }
}
