package com.voyagesaffaires.ai.service;

import com.anthropic.models.messages.OutputConfig;
import com.voyagesaffaires.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeServiceTest {

    @Test
    void sansCleApiLIaEstDesactiveeAvecUnMessageClair() {
        ClaudeService service = new ClaudeService("", "claude-opus-5-5");

        assertThat(service.estConfigure()).isFalse();
        assertThatThrownBy(() -> service.generer("consignes", "question", OutputConfig.Effort.LOW))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY")
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void avecUneCleLIaEstConfiguree() {
        // Aucun appel reseau : le client est seulement construit
        assertThat(new ClaudeService("cle-de-test", "claude-opus-5-5").estConfigure()).isTrue();
    }
}
