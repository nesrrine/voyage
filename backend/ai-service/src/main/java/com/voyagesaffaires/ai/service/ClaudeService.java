package com.voyagesaffaires.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.voyagesaffaires.ai.dto.AiDtos.ChatMessage;
import com.voyagesaffaires.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Acces bas niveau a l'API Claude (SDK officiel Anthropic).
 * La cle API vient de la variable d'environnement ANTHROPIC_API_KEY : elle reste cote serveur.
 */
@Service
@Slf4j
public class ClaudeService {

    private static final String REGLES_COMMUNES = """
            Tu es l'assistant de "Voyages & Affaires", une plateforme qui met en relation des employes \
            de differentes entreprises partant en voyage d'affaires vers les memes destinations.
            Reponds toujours en francais, de facon concise et professionnelle.
            Ecris en texte simple : pas de Markdown (pas de #, pas de **), les listes avec des tirets sont autorisees.
            """;

    private final AnthropicClient client;
    private final String model;

    public ClaudeService(@Value("${app.ai.api-key:}") String apiKey,
                         @Value("${app.ai.model:claude-opus-5-5}") String model) {
        this.model = model;
        this.client = apiKey == null || apiKey.isBlank()
                ? null
                : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        log.info(client == null
                ? "IA desactivee : variable d'environnement ANTHROPIC_API_KEY absente"
                : "IA activee avec le modele " + model);
    }

    public boolean estConfigure() {
        return client != null;
    }

    /** Une question, une reponse. */
    public String generer(String consignes, String demande, OutputConfig.Effort effort) {
        return converser(consignes, List.of(new ChatMessage("user", demande)), effort);
    }

    /** Conversation multi-tours (historique envoye par le frontend). */
    public String converser(String consignes, List<ChatMessage> historique, OutputConfig.Effort effort) {
        if (client == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "IA non configuree : definissez la variable d'environnement ANTHROPIC_API_KEY puis redemarrez l'ai-service");
        }
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(REGLES_COMMUNES + "\n" + consignes)
                .outputConfig(OutputConfig.builder().effort(effort).build());
        for (ChatMessage m : historique) {
            if ("assistant".equals(m.role())) {
                params.addAssistantMessage(m.content());
            } else {
                params.addUserMessage(m.content());
            }
        }
        try {
            Message reponse = client.messages().create(params.build());
            if (reponse.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                return "Je ne peux pas repondre a cette demande.";
            }
            return reponse.content().stream()
                    .flatMap(bloc -> bloc.text().stream())
                    .map(t -> t.text())
                    .collect(Collectors.joining("\n"))
                    .trim();
        } catch (RateLimitException e) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trop de requetes vers l'IA, reessayez dans un instant");
        } catch (AnthropicServiceException e) {
            log.error("Erreur API Claude ({}) : {}", e.statusCode(), e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Le service IA a renvoye une erreur (" + e.statusCode() + ")");
        } catch (RuntimeException e) {
            log.error("Appel IA impossible", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Service IA injoignable");
        }
    }
}
