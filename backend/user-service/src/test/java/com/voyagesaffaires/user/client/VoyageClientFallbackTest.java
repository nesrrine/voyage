package com.voyagesaffaires.user.client;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThatCode;

class VoyageClientFallbackTest {

    @Test
    void siLeVoyageServiceEstArreteLaDesactivationNEchouePas() {
        VoyageClient client = new VoyageClientFallback().create(new RuntimeException(new ConnectException("refused")));

        assertThatCode(() -> client.majEmployeActif(6L, false)).doesNotThrowAnyException();
    }
}
