package com.voyagesaffaires.user.security;

import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.SecurityUtils;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Charge depuis la base l'utilisateur identifie par le jeton (et refuse les comptes desactives). */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User get() {
        User u = userRepository.findById(SecurityUtils.currentUser().id())
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable"));
        if (!u.isActif()) {
            throw ApiException.unauthorized("Ce compte est desactive");
        }
        return u;
    }
}
