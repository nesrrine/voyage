package com.voyagesaffaires.voyage.client;

import com.voyagesaffaires.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reponses de repli du circuit breaker Resilience4j quand un microservice ne repond pas. */
class FallbacksTest {

    private final Throwable panne = new RuntimeException(new ConnectException("Connection refused"));

    @Test
    void tableauDeBordDisponibleMemeSiLeUserServiceEstArrete() {
        var stats = new UserClientFallback().create(panne).statistiques();

        assertThat(stats.employes()).isZero();
        assertThat(stats.entreprisesParDomaine().labels()).isEmpty();
    }

    @Test
    void tableauDeBordDisponibleMemeSiLeForumServiceEstArrete() {
        assertThat(new ForumClientFallback().create(panne).statistiques().publications()).isZero();
    }

    @Test
    void sansLeUserServiceImpossibleDePlanifierPourUnEmployeErreur503() {
        UserClient client = new UserClientFallback().create(panne);

        assertThatThrownBy(() -> client.utilisateur(7L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void unRefusMetierDuUserServiceEstPropageTelQuel() {
        UserClient client = new UserClientFallback().create(ApiException.forbidden("Acces refuse a cet utilisateur"));

        assertThatThrownBy(() -> client.utilisateur(9L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Acces refuse a cet utilisateur")
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
