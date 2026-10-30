package com.voyagesaffaires.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Premier niveau de securite : controle des jetons et des roles par la gateway. */
class JwtGatewayFilterTest {

    private static final String SECRET =
            "dm95YWdlcy1hZmZhaXJlcy1zZWNyZXQta2V5LXBvdXItbGUtcHJvamV0LWp3dC0yMDI2LWFuZ3VsYXItc3ByaW5n";
    private static final String AUTRE_SECRET =
            "YXV0cmUtY2xlLXNlY3JldGUtcXVpLW5lLWNvcnJlc3BvbmQtcGFzLWEtY2VsbGUtZGUtbGEtcGxhdGVmb3JtZQ==";

    private final JwtGatewayFilter filtre = new JwtGatewayFilter(SECRET);

    /** Chaine qui memorise la requete transmise au microservice (null si la gateway a bloque). */
    private final AtomicReference<ServerWebExchange> transmise = new AtomicReference<>();
    private final GatewayFilterChain chaine = ex -> {
        transmise.set(ex);
        return Mono.empty();
    };

    private static String jeton(String secret, String role, long uid, long dureeMs) {
        return Jwts.builder().subject("u@test.com").claim("uid", uid).claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + dureeMs))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
    }

    private MockServerWebExchange appel(MockServerHttpRequest.BaseBuilder<?> requete) {
        MockServerWebExchange exchange = MockServerWebExchange.from(requete);
        filtre.filter(exchange, chaine).block();
        return exchange;
    }

    @Test
    void laConnexionEstPublique() {
        appel(MockServerHttpRequest.post("/api/auth/login"));
        assertThat(transmise.get()).isNotNull();
    }

    @Test
    void lesReferentielsSontPublics() {
        appel(MockServerHttpRequest.get("/api/meta/villes"));
        assertThat(transmise.get()).isNotNull();
    }

    @Test
    void sansJetonLaRequeteEstBloqueeAvecUne401() {
        MockServerWebExchange ex = appel(MockServerHttpRequest.get("/api/voyages"));

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(transmise.get()).as("la requete n'atteint pas le microservice").isNull();
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRefuse() {
        MockServerWebExchange ex = appel(MockServerHttpRequest.get("/api/voyages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton(AUTRE_SECRET, "EMPLOYE", 6, 60_000)));

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(transmise.get()).isNull();
    }

    @Test
    void unJetonExpireEstRefuse() {
        MockServerWebExchange ex = appel(MockServerHttpRequest.get("/api/voyages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton(SECRET, "EMPLOYE", 6, -60_000)));

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unEmployeNAccedePasALAdministration() {
        MockServerWebExchange ex = appel(MockServerHttpRequest.get("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton(SECRET, "EMPLOYE", 6, 60_000)));

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(transmise.get()).isNull();
    }

    @Test
    void unAdministrateurNeRepondPasAuxInvitationsDeVoyage() {
        MockServerWebExchange ex = appel(MockServerHttpRequest.get("/api/matchs/recues")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton(SECRET, "ADMIN", 1, 60_000)));

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void seuleLEntrepriseGereLesInvitationsDesEmployes() {
        appel(MockServerHttpRequest.get("/api/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton(SECRET, "ENTREPRISE", 2, 60_000)));
        assertThat(transmise.get()).isNotNull();
    }

    @Test
    void unJetonValideEstTransmisAvecLIdentiteDeLUtilisateur() {
        appel(MockServerHttpRequest.get("/api/voyages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton(SECRET, "EMPLOYE", 6, 60_000)));

        HttpHeaders headers = transmise.get().getRequest().getHeaders();
        assertThat(headers.getFirst(JwtGatewayFilter.HEADER_USER_ID)).isEqualTo("6");
        assertThat(headers.getFirst(JwtGatewayFilter.HEADER_USER_ROLE)).isEqualTo("EMPLOYE");
    }

    @Test
    void lesEnTetesDIdentiteEnvoyesParLeClientSontSupprimes() {
        appel(MockServerHttpRequest.post("/api/auth/login").header(JwtGatewayFilter.HEADER_USER_ROLE, "ADMIN"));

        assertThat(transmise.get().getRequest().getHeaders().containsKey(JwtGatewayFilter.HEADER_USER_ROLE)).isFalse();
    }
}
