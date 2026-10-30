package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.user.dto.UserDtos.InvitationDto;
import com.voyagesaffaires.user.dto.UserDtos.InvitationRequest;
import com.voyagesaffaires.user.entity.Invitation;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import com.voyagesaffaires.user.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Invitations envoyees par une entreprise a ses employes pour rejoindre la plateforme. */
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final int VALIDITE_JOURS = 7;

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public InvitationDto inviter(InvitationRequest req) {
        User entrepriseUser = currentUser.get();
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un utilisateur est deja inscrit avec cet email");
        }
        Long entrepriseId = entrepriseUser.getEntreprise().getId();
        if (invitationRepository.existsByEmailIgnoreCaseAndEntrepriseIdAndStatut(email, entrepriseId, StatutInvitation.EN_ATTENTE)) {
            throw ApiException.conflict("Une invitation est deja en attente pour cet email");
        }
        Invitation inv = invitationRepository.save(Invitation.builder()
                .email(email)
                .poste(req.poste())
                .token(UUID.randomUUID().toString())
                .entreprise(entrepriseUser.getEntreprise())
                .dateExpiration(LocalDateTime.now().plusDays(VALIDITE_JOURS))
                .build());
        return toDto(inv);
    }

    @Transactional
    public List<InvitationDto> lister() {
        Long entrepriseId = currentUser.get().getEntreprise().getId();
        List<Invitation> invitations = invitationRepository.findByEntrepriseIdOrderByDateCreationDesc(entrepriseId);
        invitations.stream()
                .filter(i -> i.getStatut() == StatutInvitation.EN_ATTENTE && i.isExpiree())
                .forEach(i -> i.setStatut(StatutInvitation.EXPIREE));
        return invitations.stream().map(this::toDto).toList();
    }

    @Transactional
    public void annuler(Long id) {
        Invitation inv = getMienne(id);
        if (inv.getStatut() == StatutInvitation.ACCEPTEE) {
            throw ApiException.badRequest("Impossible d'annuler une invitation deja acceptee");
        }
        inv.setStatut(StatutInvitation.ANNULEE);
    }

    @Transactional
    public InvitationDto renouveler(Long id) {
        Invitation inv = getMienne(id);
        if (inv.getStatut() == StatutInvitation.ACCEPTEE) {
            throw ApiException.badRequest("Cette invitation a deja ete acceptee");
        }
        inv.setToken(UUID.randomUUID().toString());
        inv.setStatut(StatutInvitation.EN_ATTENTE);
        inv.setDateExpiration(LocalDateTime.now().plusDays(VALIDITE_JOURS));
        return toDto(inv);
    }

    private Invitation getMienne(Long id) {
        Long entrepriseId = currentUser.get().getEntreprise().getId();
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Invitation introuvable"));
        if (!inv.getEntreprise().getId().equals(entrepriseId)) {
            throw ApiException.forbidden("Cette invitation n'appartient pas a votre entreprise");
        }
        return inv;
    }

    private InvitationDto toDto(Invitation i) {
        String lien = i.getStatut() == StatutInvitation.EN_ATTENTE
                ? frontendUrl + "/inscription-employe?token=" + i.getToken()
                : null;
        return new InvitationDto(i.getId(), i.getEmail(), i.getPoste(), i.getStatut(),
                i.getDateCreation(), i.getDateExpiration(), lien);
    }
}
