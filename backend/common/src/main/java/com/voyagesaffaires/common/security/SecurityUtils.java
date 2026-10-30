package com.voyagesaffaires.common.security;

import com.voyagesaffaires.common.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/** Outils de securite communs aux microservices. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Utilisateur de la requete courante (lu dans le jeton JWT). */
    public static AuthUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw ApiException.unauthorized("Authentification requise");
        }
        return user;
    }

    /**
     * Configuration commune : API REST sans etat, JWT, erreurs 401/403 en JSON.
     * Le CORS est gere par l'API Gateway, pas par les services.
     */
    public static HttpSecurity configurationDeBase(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> ecrireErreur(res, HttpStatus.UNAUTHORIZED,
                                "Authentification requise"))
                        .accessDeniedHandler((req, res, ex) -> ecrireErreur(res, HttpStatus.FORBIDDEN,
                                "Acces refuse : droits insuffisants")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }

    private static void ecrireErreur(HttpServletResponse res, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}");
    }
}
