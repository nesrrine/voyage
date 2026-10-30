package com.voyagesaffaires.common.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyagesaffaires.common.exception.ApiException;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.InputStream;

/**
 * Configuration OpenFeign partagee (active uniquement dans les services qui utilisent Feign) :
 * - relais du jeton JWT de l'utilisateur vers le service appele ;
 * - conversion des erreurs HTTP du service appele en ApiException (meme statut, meme message).
 */
@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignConfig {

    @Bean
    public RequestInterceptor relaisDuJeton() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest req = attrs.getRequest();
                String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null) {
                    template.header(HttpHeaders.AUTHORIZATION, auth);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder decodeurDErreurs(ObjectMapper mapper) {
        return (methode, reponse) -> {
            HttpStatus status = HttpStatus.resolve(reponse.status());
            String message = "Erreur du service distant (" + reponse.status() + ")";
            if (reponse.body() != null) {
                try (InputStream in = reponse.body().asInputStream()) {
                    JsonNode json = mapper.readTree(in);
                    if (json != null && json.hasNonNull("message")) {
                        message = json.get("message").asText();
                    }
                } catch (Exception ignored) {
                    // corps vide ou non JSON : on garde le message par defaut
                }
            }
            return new ApiException(status == null ? HttpStatus.BAD_GATEWAY : status, message);
        };
    }
}
