package com.voyagesaffaires.ai.security;

import com.voyagesaffaires.common.security.JwtAuthenticationFilter;
import com.voyagesaffaires.common.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return SecurityUtils.configurationDeBase(http, jwtFilter)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
