package lx.project.dementia_care.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 이미지 파일 접근 경로 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/");
        
        // 기존 /images/** 매핑 유지 (하위 호환성)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/uploads/images/");
    }
}