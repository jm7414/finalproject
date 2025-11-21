package lx.project.dementia_care.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lx.project.dementia_care.dto.TotalSupportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 지자체 복지서비스(한국사회보장정보원_지자체복지서비스) 호출 서비스
 *  - 외부 API를 호출해 JSON을 받아온 뒤 그대로 반환
 *  - properties, application.yml 안 쓰고 serviceKey 하드코딩
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotalSupportService {

    /**
     * 🔥 공공데이터포털에서 발급받은 서비스키 하드코딩
     *  - 이미 URL 인코딩된 키를 쓰고 있다면 그대로 넣으면 됨
     *  - 여기 문자열만 네 키로 바꿔줘
     */
    private static final String SERVICE_KEY = "b03a0ee420c5d5aba5fc9890ca90ccfd36f621e004029f5147fe2c349bbbcd6b";

    /**
     * 지자체복지서비스 목록 조회 URL
     */
    private static final String BASE_URL =
            "https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations/LcgvWelfarelist";

    private final ObjectMapper objectMapper;

    private RestTemplate newRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return new RestTemplate(f);
    }

    /**
     * 외부 지자체복지서비스 API 호출
     *  - TotalSupportDTO → 쿼리파라미터 매핑
     *  - 결과 JSON을 JsonNode로 변환
     */
    public JsonNode fetchWelfareList(TotalSupportDTO dto) throws Exception {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", SERVICE_KEY)
                .queryParam("pageNo", dto.getPageNo() != null ? dto.getPageNo() : 1)
                .queryParam("numOfRows", dto.getNumOfRows() != null ? dto.getNumOfRows() : 100)
                .queryParam("type", "json");  // JSON 응답 강제

        // 선택 파라미터들 (값이 있을 때만 추가)
        if (StringUtils.hasText(dto.getLocalGovNm())) {
            // 한글이라면 UTF-8 인코딩 자동으로 처리됨
            uriBuilder.queryParam("localGovNm", dto.getLocalGovNm());
        }
        if (StringUtils.hasText(dto.getLifeArray())) {
            uriBuilder.queryParam("lifeArray", dto.getLifeArray());
        }
        if (StringUtils.hasText(dto.getCharTrgterArray())) {
            uriBuilder.queryParam("charTrgterArray", dto.getCharTrgterArray());
        }
        if (StringUtils.hasText(dto.getObstrTyArray())) {
            uriBuilder.queryParam("obstrTyArray", dto.getObstrTyArray());
        }
        if (StringUtils.hasText(dto.getTrgterIndvdlArray())) {
            uriBuilder.queryParam("trgterIndvdlArray", dto.getTrgterIndvdlArray());
        }
        if (StringUtils.hasText(dto.getSprtBizNm())) {
            uriBuilder.queryParam("sprtBizNm", dto.getSprtBizNm());
        }

        String url = uriBuilder.encode(StandardCharsets.UTF_8).toUriString();
        log.info("[TotalSupportService] calling welfare API: {}", url);

        RestTemplate restTemplate = newRestTemplate();
        ResponseEntity<String> upstream = restTemplate.getForEntity(url, String.class);

        int status = upstream.getStatusCode().value();
        String body = upstream.getBody();

        log.info("[TotalSupportService] upstreamStatus={}, bodySnippet={}",
                status,
                body != null ? body.substring(0, Math.min(body.length(), 200)) : "null");

        if (!upstream.getStatusCode().is2xxSuccessful() || body == null) {
            throw new IllegalStateException("복지서비스 API 호출 실패, status=" + status);
        }

        // 외부 JSON 문자열 → JsonNode (프론트에서 바로 사용 가능)
        return objectMapper.readTree(body);
    }
}
