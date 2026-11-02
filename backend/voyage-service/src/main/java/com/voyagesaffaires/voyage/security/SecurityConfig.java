package com.voyagesaffaires.voyage.security;

import com.voyagesaffaires.common.security.JwtAuthenticationFilter;
import com.voyagesaffaires.common.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return SecurityUtils.configurationDeBase(http, jwtFilter)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/error",
                                "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/meta/**").permitAll()
                        .requestMatchers("/api/matchs/**").hasAnyRole("EMPLOYE", "ENTREPRISE")
                        .anyRequest().authenticated())
                .build();
    }
}
