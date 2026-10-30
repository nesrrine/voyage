package com.voyagesaffaires.ai.client;

import com.voyagesaffaires.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Repli du circuit breaker : l'IA garde un comportement previsible quand un service est en panne. */
class FallbacksTest {

    private final Throwable panne = new RuntimeException(new ConnectException("Connection refused"));

    @Test
    void sansVoyageServiceLAssistantRecoitUnContexteVide() {
        VoyageClient client = new VoyageClientFallback().create(panne);

        assertThat(client.planning()).isEmpty();
        assertThat(client.programmes()).isEmpty();
        assertThat(client.invitationsRecues()).isEmpty();
    }

    @Test
    void sansVoyageServiceLAnalyseDUneRencontreRenvoieUne503() {
        VoyageClient client = new VoyageClientFallback().create(panne);

        assertThatThrownBy(() -> client.match(7L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void unRefusDAccesAUnMatchEstPropageEtNonMasque() {
        VoyageClient client = new VoyageClientFallback().create(ApiException.forbidden("Vous ne participez pas a ce voyage commun"));

        assertThatThrownBy(() -> client.match(7L))
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sansForumServiceLeResumeRenvoieUne503() {
        assertThatThrownBy(() -> new ForumClientFallback().create(panne).post(3L))
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
