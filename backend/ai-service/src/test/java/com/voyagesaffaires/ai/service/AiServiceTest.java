package com.voyagesaffaires.ai.service;

import com.voyagesaffaires.ai.client.ForumClient;
import com.voyagesaffaires.ai.client.VoyageClient;
import com.voyagesaffaires.ai.dto.AiDtos.*;
import com.voyagesaffaires.common.dto.ForumDtos.CommentaireDto;
import com.voyagesaffaires.common.dto.ForumDtos.PostDto;
import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.*;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Fonctionnalites IA : Claude et les autres microservices (OpenFeign) sont simules ;
 * on teste les prompts, le decoupage des reponses et la propagation des erreurs.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock ClaudeService claude;
    @Mock VoyageClient voyageClient;
    @Mock ForumClient forumClient;
    @InjectMocks AiService aiService;

    private final LocalDate t = LocalDate.now();
    private final AuthUser youssef = new AuthUser(8L, "youssef@greenpower.tn", Role.EMPLOYE, "Youssef Gharbi", "Youssef",
            2L, "GreenPower", DomaineActivite.ENERGIE, "Ingenieur", null);

    @BeforeEach
    void connecter() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(youssef, null, youssef.authorities()));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private VoyageDto voyage(long id, String employe, String entreprise, DomaineActivite domaine) {
        return new VoyageDto(id, "Salon " + id, "Paris", "France", 48.85, 2.35, 50, t.plusDays(10), t.plusDays(12), 3,
                ObjetVoyage.SALON, null, Set.of(DomaineActivite.INFORMATIQUE), true, id, employe, "Poste",
                id, entreprise, domaine, StatutTemporel.A_VENIR, null);
    }

    private MatchDto match() {
        return new MatchDto(7L, 92, 100, 75, 100, 100, 0, 3, StatutMatch.EN_ATTENTE, null, null, true,
                voyage(2, "Youssef Gharbi", "GreenPower", DomaineActivite.ENERGIE),
                voyage(1, "Amine Ben Ali", "TechSoft", DomaineActivite.INFORMATIQUE),
                "Amine Ben Ali", "Chef de projet", "TechSoft", DomaineActivite.INFORMATIQUE, null, null);
    }

    @Test
    void moderationAccepteUnMessageCorrect() {
        when(claude.generer(anyString(), anyString(), any())).thenReturn("ACCEPTABLE");

        ModerationResponse r = aiService.moderer(new ModerationRequest("Salon", "Qui vient au salon ?"));

        assertThat(r.acceptable()).isTrue();
        assertThat(r.raison()).isNull();
    }

    @Test
    void moderationRefuseEtDonneLaRaison() {
        when(claude.generer(anyString(), anyString(), any())).thenReturn("REFUSE: propos insultants");

        ModerationResponse r = aiService.moderer(new ModerationRequest("Titre", "contenu insultant"));

        assertThat(r.acceptable()).isFalse();
        assertThat(r.raison()).isEqualTo("propos insultants");
    }

    @Test
    void explicationDecoupeeEnAnalyseEtMessageDeContact() {
        when(voyageClient.match(7L)).thenReturn(match());
        when(claude.generer(anyString(), anyString(), any()))
                .thenReturn("Vos domaines sont complementaires.\n---MESSAGE---\nBonjour Amine, ...\nYoussef");

        ExplicationMatch e = aiService.expliquerMatch(7L);

        assertThat(e.explication()).isEqualTo("Vos domaines sont complementaires.");
        assertThat(e.messageContact()).startsWith("Bonjour Amine").endsWith("Youssef");
    }

    @Test
    void lePromptContientLesDeuxVoyagesEtLePrenomDuDemandeur() {
        when(voyageClient.match(7L)).thenReturn(match());
        when(claude.generer(anyString(), anyString(), any())).thenReturn("texte sans separateur");

        ExplicationMatch e = aiService.expliquerMatch(7L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(claude).generer(anyString(), prompt.capture(), any());
        assertThat(prompt.getValue()).contains("TechSoft", "GreenPower", "Signe avec mon prenom : Youssef");
        assertThat(e.messageContact()).isEmpty();
    }

    @Test
    void unRefusDuVoyageServiceEstPropageSansAppelerClaude() {
        when(voyageClient.match(7L)).thenThrow(ApiException.forbidden("Vous ne participez pas a ce voyage commun"));

        assertThatThrownBy(() -> aiService.expliquerMatch(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(claude);
    }

    @Test
    void programmeRefuseSiDatesInversees() {
        ProgrammeRequest req = new ProgrammeRequest("Salon", "Paris", "France", t.plusDays(5), t.plusDays(2),
                ObjetVoyage.SALON, null);

        assertThatThrownBy(() -> aiService.genererProgramme(req)).isInstanceOf(ApiException.class);
        verifyNoInteractions(claude);
    }

    @Test
    void resumeDuForumInclutLesCommentaires() {
        when(forumClient.post(3L)).thenReturn(new PostDto(3L, "Dubai", "Quels quartiers ?", "General", 9L,
                "Claire Martin", "FinConseil", null, 0, 0, null, 1,
                List.of(new CommentaireDto(1L, "DIFC et Business Bay", 10L, "Karim Haddad", "FinConseil", null))));
        when(claude.generer(anyString(), anyString(), any())).thenReturn("- resume");

        assertThat(aiService.resumerPost(3L).texte()).isEqualTo("- resume");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(claude).generer(anyString(), prompt.capture(), any());
        assertThat(prompt.getValue()).contains("Quels quartiers ?", "Karim Haddad : DIFC et Business Bay");
    }

    @Test
    void lAssistantEnvoieLeContexteDeLUtilisateurAClaude() {
        when(voyageClient.planning()).thenReturn(List.of(voyage(2, "Youssef Gharbi", "GreenPower", DomaineActivite.ENERGIE)));
        when(voyageClient.invitationsRecues()).thenReturn(List.of(match()));
        when(voyageClient.programmes()).thenReturn(List.of());
        when(claude.converser(anyString(), any(), any())).thenReturn("Bonjour !");

        TexteResponse r = aiService.chat(new ChatRequest(List.of(new ChatMessage("user", "Bonjour"))));

        assertThat(r.texte()).isEqualTo("Bonjour !");
        ArgumentCaptor<String> consignes = ArgumentCaptor.forClass(String.class);
        verify(claude).converser(consignes.capture(), any(), any());
        assertThat(consignes.getValue()).contains("Youssef Gharbi", "GreenPower", "1 dont 1 en attente");
    }
}
