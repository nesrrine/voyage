package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.user.dto.UserDtos.InvitationDto;
import com.voyagesaffaires.user.dto.UserDtos.InvitationRequest;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.Invitation;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import com.voyagesaffaires.user.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock InvitationRepository invitationRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUser;
    @InjectMocks InvitationService invitationService;

    private final Entreprise techsoft = Entreprise.builder().id(1L).nom("TechSoft").domaineActivite(DomaineActivite.INFORMATIQUE).build();
    private final Entreprise autre = Entreprise.builder().id(2L).nom("Autre").domaineActivite(DomaineActivite.FINANCE).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "frontendUrl", "http://localhost:4200");
        when(currentUser.get()).thenReturn(User.builder().id(2L).email("contact@techsoft.tn").password("x")
                .role(Role.ENTREPRISE).entreprise(techsoft).build());
    }

    @Test
    void invitationRefuseeSiUtilisateurDejaInscrit() {
        when(userRepository.existsByEmailIgnoreCase("amine@techsoft.tn")).thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviter(new InvitationRequest("Amine@TechSoft.tn", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void invitationCreeUnLienAvecJetonValable7Jours() {
        when(userRepository.existsByEmailIgnoreCase("new@techsoft.tn")).thenReturn(false);
        when(invitationRepository.existsByEmailIgnoreCaseAndEntrepriseIdAndStatut("new@techsoft.tn", 1L, StatutInvitation.EN_ATTENTE))
                .thenReturn(false);
        when(invitationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InvitationDto dto = invitationService.inviter(new InvitationRequest("new@techsoft.tn", "Dev"));

        assertThat(dto.statut()).isEqualTo(StatutInvitation.EN_ATTENTE);
        assertThat(dto.lien()).startsWith("http://localhost:4200/inscription-employe?token=");
        assertThat(dto.dateExpiration()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void listeMarqueLesInvitationsExpirees() {
        Invitation expiree = Invitation.builder().id(1L).email("a@b.c").token("t").entreprise(techsoft)
                .dateExpiration(LocalDateTime.now().minusHours(1)).build();
        when(invitationRepository.findByEntrepriseIdOrderByDateCreationDesc(1L)).thenReturn(List.of(expiree));

        List<InvitationDto> liste = invitationService.lister();

        assertThat(liste.get(0).statut()).isEqualTo(StatutInvitation.EXPIREE);
        assertThat(liste.get(0).lien()).as("plus de lien pour une invitation expiree").isNull();
    }

    @Test
    void impossibleDAnnulerUneInvitationDUneAutreEntreprise() {
        when(invitationRepository.findById(5L))
                .thenReturn(Optional.of(Invitation.builder().id(5L).email("x@y.z").token("t").entreprise(autre).build()));

        assertThatThrownBy(() -> invitationService.annuler(5L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void impossibleDAnnulerUneInvitationAcceptee() {
        Invitation inv = Invitation.builder().id(5L).email("x@y.z").token("t").entreprise(techsoft)
                .statut(StatutInvitation.ACCEPTEE).build();
        when(invitationRepository.findById(5L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> invitationService.annuler(5L)).isInstanceOf(ApiException.class);
        assertThat(inv.getStatut()).isEqualTo(StatutInvitation.ACCEPTEE);
    }
}
