package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.user.client.VoyageClient;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.EntrepriseRepository;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import com.voyagesaffaires.user.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Gestion des employes et communication avec le voyage-service (OpenFeign simule). */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock InvitationRepository invitationRepository;
    @Mock CurrentUserService currentUser;
    @Mock VoyageClient voyageClient;
    @InjectMocks UserAdminService service;

    private final Entreprise techsoft = Entreprise.builder().id(1L).nom("TechSoft").domaineActivite(DomaineActivite.INFORMATIQUE).build();
    private final Entreprise autre = Entreprise.builder().id(2L).nom("Autre").domaineActivite(DomaineActivite.FINANCE).build();
    private final User compteTechsoft = User.builder().id(2L).email("c@t.tn").password("x").role(Role.ENTREPRISE).entreprise(techsoft).build();

    private User employe(long id, Entreprise e) {
        return User.builder().id(id).prenom("E" + id).nom("Test").email("e" + id + "@t.tn").password("x")
                .role(Role.EMPLOYE).entreprise(e).build();
    }

    @Test
    void desactiverUnEmployePrevientLeVoyageService() {
        User amine = employe(6, techsoft);
        when(currentUser.get()).thenReturn(compteTechsoft);
        when(userRepository.findById(6L)).thenReturn(Optional.of(amine));

        UserDto dto = service.basculerStatutEmploye(6L);

        assertThat(dto.actif()).isFalse();
        verify(voyageClient).majEmployeActif(6L, false);
    }


    @Test
    void uneEntrepriseNePeutPasDesactiverLEmployeDUneAutre() {
        when(currentUser.get()).thenReturn(compteTechsoft);
        when(userRepository.findById(9L)).thenReturn(Optional.of(employe(9, autre)));

        assertThatThrownBy(() -> service.basculerStatutEmploye(9L)).isInstanceOf(ApiException.class);
        verifyNoInteractions(voyageClient);
    }

    @Test
    void ficheUtilisateurAccessibleALEntrepriseDeLEmployeSeulement() {
        when(currentUser.get()).thenReturn(compteTechsoft);
        when(userRepository.findById(6L)).thenReturn(Optional.of(employe(6, techsoft)));
        when(userRepository.findById(9L)).thenReturn(Optional.of(employe(9, autre)));

        assertThat(service.utilisateur(6L).entrepriseNom()).isEqualTo("TechSoft");
        assertThatThrownBy(() -> service.utilisateur(9L)).isInstanceOf(ApiException.class);
    }
}
