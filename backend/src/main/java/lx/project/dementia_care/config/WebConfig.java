package lx.project.dementia_care.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // classpath를 사용하는 경로로 직접 지정해줍니다.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/uploads/images/");
    }
}