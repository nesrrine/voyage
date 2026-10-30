package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.voyage.entity.MatchVoyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static com.voyagesaffaires.voyage.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Processus d'invitation aux voyages : acceptation, refus et mise en contact. */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchVoyageRepository matchRepository;
    @InjectMocks MatchService matchService;

    private final LocalDate t = LocalDate.now();
    private final AuthUser amine = employe(6, "Amine", 1, "TechSoft", DomaineActivite.INFORMATIQUE);
    private final AuthUser youssef = employe(8, "Youssef", 2, "GreenPower", DomaineActivite.ENERGIE);
    private MatchVoyage match;

    @BeforeEach
    void setUp() {
        // Le voyage d'Amine (source) a trouve celui de Youssef (cible) : Youssef recoit l'invitation
        match = match(7, voyage(1, amine, "Paris", t.plusDays(10), t.plusDays(12)),
                voyage(2, youssef, "Paris", t.plusDays(11), t.plusDays(13)));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void seulLeDestinatairePeutRepondre() {
        connecter(amine);
        when(matchRepository.findById(7L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.accepter(7L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(match.getStatut()).isEqualTo(StatutMatch.EN_ATTENTE);
    }

    @Test
    void accepterMetEnContactLesVoyageurs() {
        connecter(youssef);
        when(matchRepository.findById(7L)).thenReturn(Optional.of(match));

        MatchDto dto = matchService.accepter(7L);

        assertThat(dto.statut()).isEqualTo(StatutMatch.ACCEPTEE);
        assertThat(dto.recue()).isTrue();
        assertThat(dto.partenaireNom()).isEqualTo("Amine Test");
        assertThat(dto.partenaireEmail()).isEqualTo("amine@test.com");
        assertThat(dto.partenaireTelephone()).isNotNull();
        assertThat(match.getDateReponse()).isNotNull();
    }

    @Test
    void refuserNeDevoilePasLesCoordonnees() {
        connecter(youssef);
        when(matchRepository.findById(7L)).thenReturn(Optional.of(match));

        MatchDto dto = matchService.refuser(7L);

        assertThat(dto.statut()).isEqualTo(StatutMatch.REFUSEE);
        assertThat(dto.partenaireEmail()).isNull();
        assertThat(dto.partenaireTelephone()).isNull();
    }

    @Test
    void impossibleDeRepondreDeuxFois() {
        match.setStatut(StatutMatch.REFUSEE);
        connecter(youssef);
        when(matchRepository.findById(7L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.accepter(7L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("deja repondu");
    }

    @Test
    void consultationReserveeAuxParticipants() {
        connecter(employe(9, "Claire", 3, "FinConseil", DomaineActivite.FINANCE));
        when(matchRepository.findById(7L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.consulter(7L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void vueDeLExpediteurQuandLInvitationEstEnAttente() {
        MatchDto dto = matchService.toDto(match, amine);

        assertThat(dto.recue()).isFalse();
        assertThat(dto.monVoyage().employeId()).isEqualTo(6L);
        assertThat(dto.voyagePartenaire().employeId()).isEqualTo(8L);
        assertThat(dto.partenaireEmail()).as("coordonnees cachees avant acceptation").isNull();
    }

    @Test
    void lEntrepriseDuDestinataireVoitLInvitationCommeRecue() {
        MatchDto dto = matchService.toDto(match, entreprise(3, 2, "GreenPower", DomaineActivite.ENERGIE));

        assertThat(dto.recue()).isTrue();
        assertThat(dto.partenaireEntreprise()).isEqualTo("TechSoft");
    }
}
