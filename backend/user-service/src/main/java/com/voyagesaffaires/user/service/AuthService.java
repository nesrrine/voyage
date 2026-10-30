package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.JwtService;
import com.voyagesaffaires.user.dto.AuthDtos.*;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.Invitation;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.EntrepriseRepository;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email ou mot de passe incorrect"));
        return reponse(user);
    }

    @Transactional
    public AuthResponse registerEntreprise(RegisterEntrepriseRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe deja avec cet email");
        }
        if (entrepriseRepository.existsByNomIgnoreCase(req.nomEntreprise().trim())) {
            throw ApiException.conflict("Une entreprise portant ce nom est deja inscrite");
        }
        Entreprise entreprise = entrepriseRepository.save(Entreprise.builder()
                .nom(req.nomEntreprise().trim())
                .domaineActivite(req.domaineActivite())
                .pays(req.pays())
                .ville(req.ville())
                .adresse(req.adresse())
                .telephone(req.telephone())
                .siteWeb(req.siteWeb())
                .description(req.description())
                .build());
        User user = userRepository.save(User.builder()
                .nom(req.nom())
                .prenom(req.prenom())
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .telephone(req.telephone())
                .poste("Responsable")
                .role(Role.ENTREPRISE)
                .entreprise(entreprise)
                .build());
        return reponse(user);
    }

    @Transactional(readOnly = true)
    public InvitationInfo invitationInfo(String token) {
        Invitation inv = invitationValide(token);
        return new InvitationInfo(inv.getEmail(), inv.getEntreprise().getNom(), inv.getPoste());
    }

    @Transactional
    public AuthResponse registerEmploye(RegisterEmployeRequest req) {
        Invitation inv = invitationValide(req.token());
        if (userRepository.existsByEmailIgnoreCase(inv.getEmail())) {
            throw ApiException.conflict("Un compte existe deja avec cet email");
        }
        User user = userRepository.save(User.builder()
                .nom(req.nom())
                .prenom(req.prenom())
                .email(inv.getEmail().toLowerCase())
                .password(passwordEncoder.encode(req.password()))
                .telephone(req.telephone())
                .poste(req.poste() != null && !req.poste().isBlank() ? req.poste() : inv.getPoste())
                .role(Role.EMPLOYE)
                .entreprise(inv.getEntreprise())
                .build());
        inv.setStatut(StatutInvitation.ACCEPTEE);
        return reponse(user);
    }

    /** Jeton + profil ; aussi utilise apres une modification du profil pour rafraichir le jeton. */
    public AuthResponse reponse(User user) {
        return new AuthResponse(jwtService.generateToken(UserMapper.toAuthUser(user)), UserMapper.toDto(user));
    }

    private Invitation invitationValide(String token) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> ApiException.notFound("Invitation introuvable"));
        if (inv.getStatut() != StatutInvitation.EN_ATTENTE) {
            throw ApiException.badRequest("Cette invitation n'est plus valide");
        }
        if (inv.isExpiree()) {
            throw ApiException.badRequest("Cette invitation a expire, demandez-en une nouvelle a votre entreprise");
        }
        return inv;
    }
}
