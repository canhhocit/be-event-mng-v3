package com.sa.event_mng.shared.infrastructure.config;

import com.sa.event_mng.task.EventStatusRefreshInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final EventStatusRefreshInterceptor eventStatusRefreshInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(eventStatusRefreshInterceptor)
                .excludePathPatterns("/ping", "/api/v1/ping", "/actuator/**", "/images/**",
                        "/swagger-ui/**", "/v3/api-docs/**", "/error");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                //.allowedOrigins("http://localhost:3000", "http://localhost:5173") 
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = new java.io.File("uploads").getAbsolutePath();
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
