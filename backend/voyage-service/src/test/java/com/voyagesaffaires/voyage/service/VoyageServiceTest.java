package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.voyage.client.UserClient;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VoyageRequest;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VoyageResultat;
import com.voyagesaffaires.voyage.entity.Voyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import com.voyagesaffaires.voyage.repository.VoyageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.voyagesaffaires.voyage.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoyageServiceTest {

    @Mock VoyageRepository voyageRepository;
    @Mock MatchVoyageRepository matchRepository;
    @Mock MatchingService matchingService;
    @Mock MatchService matchService;
    @Mock UserClient userClient;
    @InjectMocks VoyageService voyageService;

    private final LocalDate t = LocalDate.now();
    private final AuthUser amine = employe(6, "Amine", 1, "TechSoft", DomaineActivite.INFORMATIQUE);
    private final AuthUser claire = employe(9, "Claire", 3, "FinConseil", DomaineActivite.FINANCE);
    private final AuthUser techsoft = entreprise(2, 1, "TechSoft", DomaineActivite.INFORMATIQUE);

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private VoyageRequest demande(LocalDate debut, LocalDate fin, Long employeId) {
        return new VoyageRequest("Salon", "Paris", "France", 48.85, 2.35, null, debut, fin,
                ObjetVoyage.SALON, null, Set.of(DomaineActivite.FINANCE), null, employeId);
    }

    private UserDto userDto(long id, String prenom, Long entrepriseId, boolean actif) {
        return new UserDto(id, "Trabelsi", prenom, prenom.toLowerCase() + "@techsoft.tn", "+216", "Commerciale",
                Role.EMPLOYE, actif, entrepriseId, "TechSoft", DomaineActivite.INFORMATIQUE, null);
    }

    private static HttpStatus statut(Throwable e) {
        return ((ApiException) e).getStatus();
    }

    @Test
    void employeCreeSonVoyageAvecLesInformationsDuJetonEtLeMatchingEstLance() {
        connecter(amine);
        when(voyageRepository.save(any())).thenAnswer(i -> {
            Voyage v = i.getArgument(0);
            v.setId(42L);
            return v;
        });
        when(matchingService.lancer(any())).thenReturn(2);

        VoyageResultat res = voyageService.creer(demande(t.plusDays(10), t.plusDays(12), null));

        assertThat(res.nouveauxMatchs()).isEqualTo(2);
        assertThat(res.voyage().employeId()).isEqualTo(6L);
        assertThat(res.voyage().employeNom()).isEqualTo("Amine Test");
        assertThat(res.voyage().entrepriseDomaine()).isEqualTo(DomaineActivite.INFORMATIQUE);
        assertThat(res.voyage().rayonKm()).as("perimetre par defaut").isEqualTo(50);
        assertThat(res.voyage().dureeJours()).isEqualTo(3);
        verifyNoInteractions(userClient);
    }

    @Test
    void dateDeFinAvantDateDeDebutRefusee() {
        connecter(amine);

        assertThatThrownBy(() -> voyageService.creer(demande(t.plusDays(10), t.plusDays(5), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("date de fin");
        verify(voyageRepository, never()).save(any());
    }

    @Test
    void voyageEntierementDansLePasseRefuse() {
        connecter(amine);

        assertThatThrownBy(() -> voyageService.creer(demande(t.minusDays(10), t.minusDays(5), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("passe");
    }

    @Test
    void entrepriseDoitChoisirLEmploye() {
        connecter(techsoft);

        assertThatThrownBy(() -> voyageService.creer(demande(t.plusDays(1), t.plusDays(2), null)))
                .isInstanceOf(ApiException.class)
                .extracting(VoyageServiceTest::statut).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void entrepriseNePeutPasPlanifierPourLEmployeDUneAutreEntreprise() {
        connecter(techsoft);
        when(userClient.utilisateur(9L)).thenReturn(userDto(9, "Claire", 3L, true));

        assertThatThrownBy(() -> voyageService.creer(demande(t.plusDays(1), t.plusDays(2), 9L)))
                .isInstanceOf(ApiException.class)
                .extracting(VoyageServiceTest::statut).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void entreprisePlanifiePourSonEmployeGraceAuUserService() {
        connecter(techsoft);
        when(userClient.utilisateur(7L)).thenReturn(userDto(7, "Sarra", 1L, true));
        when(voyageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VoyageResultat res = voyageService.creer(demande(t.plusDays(1), t.plusDays(2), 7L));

        assertThat(res.voyage().employeId()).isEqualTo(7L);
        assertThat(res.voyage().employeNom()).isEqualTo("Sarra Trabelsi");
        verify(userClient).utilisateur(7L);
    }

    @Test
    void entrepriseNePeutPasPlanifierPourUnEmployeDesactive() {
        connecter(techsoft);
        when(userClient.utilisateur(7L)).thenReturn(userDto(7, "Sarra", 1L, false));

        assertThatThrownBy(() -> voyageService.creer(demande(t.plusDays(1), t.plusDays(2), 7L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("desactive");
    }

    @Test
    void administrateurNePeutPasPlanifier() {
        connecter(admin());

        assertThatThrownBy(() -> voyageService.creer(demande(t.plusDays(1), t.plusDays(2), null)))
                .isInstanceOf(ApiException.class)
                .extracting(VoyageServiceTest::statut).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unEmployeNePeutPasModifierLeVoyageDUnAutre() {
        connecter(amine);
        when(voyageRepository.findById(5L)).thenReturn(Optional.of(voyage(5, claire, "Paris", t.plusDays(1), t.plusDays(3))));

        assertThatThrownBy(() -> voyageService.modifier(5L, demande(t.plusDays(1), t.plusDays(3), null)))
                .isInstanceOf(ApiException.class)
                .extracting(VoyageServiceTest::statut).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void modificationParLEntrepriseRecalculeLesInvitationsEnAttente() {
        Voyage v = voyage(5, amine, "Lyon", t.plusDays(1), t.plusDays(3));
        connecter(techsoft);
        when(voyageRepository.findById(5L)).thenReturn(Optional.of(v));
        when(matchingService.lancer(v)).thenReturn(1);

        VoyageResultat res = voyageService.modifier(5L, demande(t.plusDays(4), t.plusDays(6), null));

        assertThat(v.getVilleDestination()).isEqualTo("Paris");
        assertThat(res.nouveauxMatchs()).isEqualTo(1);
        var ordre = inOrder(matchRepository, matchingService);
        ordre.verify(matchRepository).deleteByVoyageAndStatut(5L, StatutMatch.EN_ATTENTE);
        ordre.verify(matchingService).lancer(v);
    }

    @Test
    void suppressionSupprimeAussiLesInvitationsDeVoyage() {
        Voyage v = voyage(5, amine, "Paris", t.plusDays(1), t.plusDays(3));
        connecter(amine);
        when(voyageRepository.findById(5L)).thenReturn(Optional.of(v));

        voyageService.supprimer(5L);

        verify(matchRepository).deleteByVoyage(5L);
        verify(voyageRepository).delete(v);
    }

    @Test
    void desactivationDUnEmployeLimiteeASonEntreprise() {
        connecter(techsoft);
        when(voyageRepository.majEmployeActif(6L, false, 1L)).thenReturn(2);

        assertThat(voyageService.majEmployeActif(6L, false)).isEqualTo(2);
    }

    @Test
    void unEmployeNePeutPasDesactiverLesVoyagesDUnAutre() {
        connecter(amine);

        assertThatThrownBy(() -> voyageService.majEmployeActif(9L, false))
                .isInstanceOf(ApiException.class)
                .extracting(VoyageServiceTest::statut).isEqualTo(HttpStatus.FORBIDDEN);
        verify(voyageRepository, never()).majEmployeActif(any(), anyBoolean(), any());
    }

    @Test
    void programmesFiltresParDestinationEtDomaine() {
        Voyage paris = voyage(1, amine, "Paris", t.plusDays(1), t.plusDays(3));
        Voyage dubai = voyage(2, claire, "Dubai", t.plusDays(1), t.plusDays(3));
        Voyage parisFinance = voyage(3, claire, "Paris", t.plusDays(5), t.plusDays(6));
        when(voyageRepository.findProgrammes(any())).thenReturn(List.of(paris, dubai, parisFinance));

        List<VoyageDto> res = voyageService.programmes("par", null, DomaineActivite.FINANCE, null, null);

        assertThat(res).extracting(VoyageDto::id).containsExactly(3L);
    }
}
