package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.JwtService;
import com.voyagesaffaires.user.dto.AuthDtos.AuthResponse;
import com.voyagesaffaires.user.dto.AuthDtos.RegisterEmployeRequest;
import com.voyagesaffaires.user.dto.AuthDtos.RegisterEntrepriseRequest;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.Invitation;
import com.voyagesaffaires.user.repository.EntrepriseRepository;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock InvitationRepository invitationRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private final Entreprise techsoft = Entreprise.builder().id(1L).nom("TechSoft")
            .domaineActivite(DomaineActivite.INFORMATIQUE).build();

    private RegisterEntrepriseRequest demandeEntreprise(String email) {
        return new RegisterEntrepriseRequest("TechSoft", DomaineActivite.INFORMATIQUE, "Tunisie", "Tunis",
                null, null, null, null, "Mansour", "Hichem", email, "secret123");
    }

    private Invitation invitation(StatutInvitation statut, LocalDateTime expiration) {
        return Invitation.builder().id(1L).email("nouveau@techsoft.tn").poste("Developpeur").token("tok")
                .entreprise(techsoft).statut(statut).dateExpiration(expiration).build();
    }

    @Test
    void inscriptionEntrepriseRefuseeSiEmailDejaUtilise() {
        when(userRepository.existsByEmailIgnoreCase("deja@pris.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerEntreprise(demandeEntreprise("deja@pris.com")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(entrepriseRepository, never()).save(any());
    }

    @Test
    void inscriptionEntrepriseCreeLeCompteEtUnJetonAvecLesInformationsDeLEntreprise() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(entrepriseRepository.existsByNomIgnoreCase("TechSoft")).thenReturn(false);
        when(entrepriseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(jwtService.generateToken(any())).thenReturn("jwt");

        AuthResponse res = authService.registerEntreprise(demandeEntreprise("  Contact@TechSoft.tn "));

        assertThat(res.token()).isEqualTo("jwt");
        assertThat(res.user().role()).isEqualTo(Role.ENTREPRISE);
        assertThat(res.user().email()).isEqualTo("contact@techsoft.tn");
        ArgumentCaptor<AuthUser> jeton = ArgumentCaptor.forClass(AuthUser.class);
        verify(jwtService).generateToken(jeton.capture());
        assertThat(jeton.getValue().entrepriseNom()).isEqualTo("TechSoft");
        assertThat(jeton.getValue().domaine()).isEqualTo(DomaineActivite.INFORMATIQUE);
    }

    @Test
    void inscriptionEmployeRefuseeSiInvitationExpiree() {
        when(invitationRepository.findByToken("tok"))
                .thenReturn(Optional.of(invitation(StatutInvitation.EN_ATTENTE, LocalDateTime.now().minusDays(1))));

        assertThatThrownBy(() -> authService.registerEmploye(
                new RegisterEmployeRequest("tok", "Ali", "Sami", null, null, "secret123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expire");
    }

    @Test
    void inscriptionEmployeRefuseeSiInvitationAnnulee() {
        when(invitationRepository.findByToken("tok"))
                .thenReturn(Optional.of(invitation(StatutInvitation.ANNULEE, LocalDateTime.now().plusDays(3))));

        assertThatThrownBy(() -> authService.invitationInfo("tok"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("plus valide");
    }

    @Test
    void inscriptionEmployeRattacheALEntrepriseEtConsommeLInvitation() {
        Invitation inv = invitation(StatutInvitation.EN_ATTENTE, LocalDateTime.now().plusDays(3));
        when(invitationRepository.findByToken("tok")).thenReturn(Optional.of(inv));
        when(userRepository.existsByEmailIgnoreCase("nouveau@techsoft.tn")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtService.generateToken(any())).thenReturn("jwt");

        AuthResponse res = authService.registerEmploye(
                new RegisterEmployeRequest("tok", "Ali", "Sami", null, "", "secret123"));

        assertThat(res.user().role()).isEqualTo(Role.EMPLOYE);
        assertThat(res.user().email()).isEqualTo("nouveau@techsoft.tn");
        assertThat(res.user().entrepriseId()).isEqualTo(1L);
        assertThat(res.user().poste()).as("poste repris de l'invitation").isEqualTo("Developpeur");
        assertThat(inv.getStatut()).isEqualTo(StatutInvitation.ACCEPTEE);
    }
}
