package lx.project.dementia_care.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

@Component
public class GoogleAiClient {
	private static final String MODEL = "gemini-2.0-flash";
	private static final String BASE = "generativelanguage.googleapis.com";

	// 환경변수에서 API 키 가져오기 (필수 - Git에 올라가면 Gemini에서 키 차단)
	// 개발 환경: 로컬 .env 파일 또는 환경변수로 설정
	// 프로덕션: GitHub Secrets 또는 서버 환경변수로 설정
	@Value("${GEMINI_API_KEY}")
	private String apiKey;

	private String resolveApiKey() {
		return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		
	}

	// 간단 타임아웃 설정
	private RestTemplate newRestTemplate() {
		var f = new SimpleClientHttpRequestFactory();
		f.setConnectTimeout(5000);
		f.setReadTimeout(15000);
		return new RestTemplate(f);
	}

	public String generateText(String prompt) {
		String apiKey = resolveApiKey();
		String url = "https://" + BASE + "/v1beta/models/" + MODEL + ":generateContent?key=" + apiKey;

		var body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.set("User-Agent", "dementia-care/1.0");

		RestTemplate rt = newRestTemplate();
		ResponseEntity<Map> resp = rt.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			throw new RuntimeException("Gemini 호출 실패: " + resp.getStatusCode());
		}

		var bodyMap = resp.getBody();

		// safety block 등으로 candidates가 없을 수 있음
		Object candObj = bodyMap.get("candidates");
		if (!(candObj instanceof List<?> candidates) || candidates.isEmpty()) {
			// 에러 메시지 힌트
			Object promptFeedback = bodyMap.get("promptFeedback");
			throw new RuntimeException("Gemini 응답에 candidates 없음. promptFeedback=" + String.valueOf(promptFeedback));
		}

		var first = candidates.get(0);
		if (!(first instanceof Map<?, ?> firstMap))
			return "";

		var content = firstMap.get("content");
		if (!(content instanceof Map<?, ?> contentMap))
			return "";

		var parts = contentMap.get("parts");
		if (!(parts instanceof List<?> partsList) || partsList.isEmpty())
			return "";

		var p0 = partsList.get(0);
		if (!(p0 instanceof Map<?, ?> p0Map))
			return "";

		Object text = p0Map.get("text");
		return text instanceof String ? (String) text : "";
	}
}
