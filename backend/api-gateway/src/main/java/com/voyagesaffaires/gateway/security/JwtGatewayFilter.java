package com.voyagesaffaires.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Premier niveau de securite : la gateway rejette, avant tout routage, les requetes sans jeton valide
 * (401) ou dont le role ne permet pas d'acceder au chemin demande (403).
 * Le second niveau (regles fines et appartenance des donnees) est assure par chaque microservice.
 */
@Component
@Slf4j
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    /** En-tetes ajoutes pour les services en aval (et supprimes s'ils sont envoyes par le client). */
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    /** Evaluees dans l'ordre : la premiere regle qui correspond s'applique. */
    static final List<RegleAcces> REGLES = List.of(
            // Acces publics
            RegleAcces.publique(HttpMethod.POST, "/api/auth/login"),
            RegleAcces.publique(HttpMethod.POST, "/api/auth/register-entreprise"),
            RegleAcces.publique(HttpMethod.POST, "/api/auth/register-employe"),
            RegleAcces.publique(HttpMethod.GET, "/api/auth/invitation/**"),
            RegleAcces.publique(HttpMethod.GET, "/api/meta/**"),
            // Acces par role
            RegleAcces.roles("/api/admin/**", "ADMIN"),
            RegleAcces.roles("/api/invitations/**", "ENTREPRISE"),
            RegleAcces.roles("/api/entreprise/**", "ENTREPRISE"),
            RegleAcces.roles("/api/matchs/**", "EMPLOYE", "ENTREPRISE"),
            // Tout le reste de l'API : utilisateur connecte, quel que soit son role
            RegleAcces.roles("/api/**", "ADMIN", "ENTREPRISE", "EMPLOYE"));

    private final SecretKey key;

    public JwtGatewayFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest requete = exchange.getRequest();
        // Le client ne doit jamais pouvoir fournir lui-meme ces en-tetes d'identite
        ServerHttpRequest nettoyee = requete.mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_USER_ROLE);
                })
                .build();
        HttpMethod methode = requete.getMethod();
        String chemin = requete.getPath().value();

        if (HttpMethod.OPTIONS.equals(methode)) {
            return chain.filter(exchange.mutate().request(nettoyee).build());
        }
        RegleAcces regle = REGLES.stream().filter(r -> r.correspond(methode, chemin)).findFirst().orElse(null);
        if (regle == null || regle.estPublique()) {
            return chain.filter(exchange.mutate().request(nettoyee).build());
        }

        String header = requete.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return refuser(exchange, HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Jeton refuse par la gateway ({} {}) : {}", methode, chemin, e.getMessage());
            return refuser(exchange, HttpStatus.UNAUTHORIZED, "Jeton invalide ou expire");
        }
        String role = claims.get("role", String.class);
        if (role == null || !regle.roles().contains(role)) {
            log.warn("Acces refuse par la gateway : role {} sur {} {}", role, methode, chemin);
            return refuser(exchange, HttpStatus.FORBIDDEN, "Acces refuse : droits insuffisants");
        }
        Object uid = claims.get("uid");
        ServerHttpRequest enrichie = nettoyee.mutate()
                .header(HEADER_USER_ID, String.valueOf(uid))
                .header(HEADER_USER_ROLE, role)
                .build();
        return chain.filter(exchange.mutate().request(enrichie).build());
    }

    /** Avant le routage (le filtre de routage a l'ordre Ordered.LOWEST_PRECEDENCE). */
    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> refuser(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse reponse = exchange.getResponse();
        reponse.setStatusCode(status);
        reponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":" + status.value() + ",\"message\":\"" + message + "\",\"source\":\"api-gateway\"}";
        DataBuffer buffer = reponse.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return reponse.writeWith(Mono.just(buffer));
    }
}
