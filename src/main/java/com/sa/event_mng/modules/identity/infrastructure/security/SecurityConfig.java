package com.sa.event_mng.modules.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final CustomJwtDecoder customJwtDecoder;

  private final String[] PUBLIC_POST_ENDPOINTS = {
      "/auth/login",
      "/auth/register",
      "/auth/logout",
      "/auth/refresh",
      "/auth/introspect",
      "/auth/forgot-password",
      "/auth/reset-password",
      "/api/v1/payments/payos-webhook"
  };

  private final String[] PUBLIC_GET_ENDPOINTS = {
      "/auth/verify",
      "/categories/**",
      "/events/**",
      "/blog/**",
      "/api/v1/payments/payos-webhook",
      "/api/v1/payments/redirect",
      "/images/**",
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/vouchers/**",
      "/error"
  };

  @Value("${application.security.jwt.secret-key}")
  private String signerKey;

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("https://event-mng-v2.vercel.app");
    config.addAllowedOriginPattern("*");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(request -> request
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
            .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
            .anyRequest().authenticated());

    httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(customJwtDecoder)
        .jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return httpSecurity.build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

    return jwtAuthenticationConverter;
  }

}
