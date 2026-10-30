package com.voyagesaffaires.common.security;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Utilisateur connecte, reconstruit a partir des informations (claims) du jeton JWT.
 * Chaque microservice connait ainsi l'utilisateur sans interroger la base du user-service.
 */
public record AuthUser(Long id, String email, Role role, String nomComplet, String prenom,
                       Long entrepriseId, String entrepriseNom, DomaineActivite domaine,
                       String poste, String telephone) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean is(Role r) {
        return role == r;
    }
}
