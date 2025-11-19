// src/main/java/lx/project/dementia_care/config/RestTemplateConfig.java
package lx.project.dementia_care.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 타임아웃 간단 설정 (필요 없으면 new RestTemplate()만 써도 됨)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초 연결 타임아웃
        factory.setReadTimeout(15000);   // 15초 응답 대기

        return new RestTemplate(factory);
    }
}
