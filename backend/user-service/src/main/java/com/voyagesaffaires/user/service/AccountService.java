package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.user.dto.AuthDtos.AuthResponse;
import com.voyagesaffaires.user.dto.UserDtos.*;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.UserRepository;
import com.voyagesaffaires.user.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion du compte de l'utilisateur connecte.
 * Apres une modification, un nouveau jeton est renvoye : les autres microservices lisent
 * le nom, le poste et l'entreprise dans le jeton.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final CurrentUserService currentUser;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final UserAdminService userAdminService;

    @Transactional(readOnly = true)
    public UserDto moi() {
        return UserMapper.toDto(currentUser.get());
    }

    @Transactional
    public AuthResponse modifierProfil(UpdateProfilRequest req) {
        User u = currentUser.get();
        u.setNom(req.nom());
        u.setPrenom(req.prenom());
        u.setTelephone(req.telephone());
        u.setPoste(req.poste());
        return authService.reponse(u);
    }

    @Transactional
    public void changerMotDePasse(ChangePasswordRequest req) {
        User u = currentUser.get();
        if (!passwordEncoder.matches(req.ancienMotDePasse(), u.getPassword())) {
            throw ApiException.badRequest("L'ancien mot de passe est incorrect");
        }
        u.setPassword(passwordEncoder.encode(req.nouveauMotDePasse()));
    }

    @Transactional(readOnly = true)
    public EntrepriseDto monEntreprise() {
        Entreprise e = currentUser.get().getEntreprise();
        if (e == null) {
            throw ApiException.notFound("Aucune entreprise associee a ce compte");
        }
        return EntrepriseDto.from(e, userRepository.countByEntrepriseIdAndRole(e.getId(), Role.EMPLOYE));
    }

    @Transactional
    public AuthResponse modifierEntreprise(UpdateEntrepriseRequest req) {
        User u = currentUser.get();
        if (u.getRole() != Role.ENTREPRISE) {
            throw ApiException.forbidden("Seul le compte entreprise peut modifier ces informations");
        }
        Entreprise e = u.getEntreprise();
        e.setNom(req.nom());
        e.setDomaineActivite(req.domaineActivite());
        e.setPays(req.pays());
        e.setVille(req.ville());
        e.setAdresse(req.adresse());
        e.setTelephone(req.telephone());
        e.setSiteWeb(req.siteWeb());
        e.setDescription(req.description());
        return authService.reponse(u);
    }

    @Transactional
    public void desactiverMonCompte() {
        User u = currentUser.get();
        if (u.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Le compte administrateur ne peut pas etre desactive");
        }
        u.setActif(false);
        userAdminService.synchroniserVoyages(u);
    }
}
