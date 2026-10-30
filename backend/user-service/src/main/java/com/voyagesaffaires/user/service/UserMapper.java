package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.User;

/** Conversions entite User -> DTO / utilisateur du jeton JWT. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(User u) {
        Entreprise e = u.getEntreprise();
        return new UserDto(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(), u.getPoste(),
                u.getRole(), u.isActif(),
                e == null ? null : e.getId(),
                e == null ? null : e.getNom(),
                e == null ? null : e.getDomaineActivite(),
                u.getDateCreation());
    }

    /** Informations placees dans le jeton : elles permettent aux autres services de connaitre l'utilisateur. */
    public static AuthUser toAuthUser(User u) {
        Entreprise e = u.getEntreprise();
        return new AuthUser(u.getId(), u.getEmail(), u.getRole(), u.getNomComplet(), u.getPrenom(),
                e == null ? null : e.getId(),
                e == null ? null : e.getNom(),
                e == null ? null : e.getDomaineActivite(),
                u.getPoste(), u.getTelephone());
    }
}
