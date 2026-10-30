package com.voyagesaffaires.user.service;

import com.voyagesaffaires.common.dto.StatsDtos.Serie;
import com.voyagesaffaires.common.dto.StatsDtos.UserStats;
import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.user.client.VoyageClient;
import com.voyagesaffaires.user.dto.UserDtos.EntrepriseDto;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.EntrepriseRepository;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import com.voyagesaffaires.user.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Gestion des employes (par leur entreprise), des comptes (par l'admin) et API interne pour les autres services. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final InvitationRepository invitationRepository;
    private final CurrentUserService currentUser;
    private final VoyageClient voyageClient;

    // ---------- Entreprise ----------

    @Transactional(readOnly = true)
    public List<UserDto> mesEmployes() {
        Long entrepriseId = currentUser.get().getEntreprise().getId();
        return userRepository.findByEntrepriseIdAndRoleOrderByNomAsc(entrepriseId, Role.EMPLOYE)
                .stream().map(UserMapper::toDto).toList();
    }

    @Transactional
    public UserDto basculerStatutEmploye(Long employeId) {
        Long entrepriseId = currentUser.get().getEntreprise().getId();
        User emp = userRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Employe introuvable"));
        if (emp.getRole() != Role.EMPLOYE || emp.getEntreprise() == null
                || !emp.getEntreprise().getId().equals(entrepriseId)) {
            throw ApiException.forbidden("Cet employe n'appartient pas a votre entreprise");
        }
        emp.setActif(!emp.isActif());
        synchroniserVoyages(emp);
        return UserMapper.toDto(emp);
    }

    // ---------- Administrateur ----------

    @Transactional(readOnly = true)
    public List<UserDto> tousLesUtilisateurs() {
        return userRepository.findAllByOrderByDateCreationDesc().stream().map(UserMapper::toDto).toList();
    }

    @Transactional
    public UserDto basculerStatut(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (u.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Impossible de desactiver un administrateur");
        }
        u.setActif(!u.isActif());
        synchroniserVoyages(u);
        return UserMapper.toDto(u);
    }

    @Transactional(readOnly = true)
    public List<EntrepriseDto> toutesLesEntreprises() {
        return entrepriseRepository.findAll().stream()
                .map(e -> EntrepriseDto.from(e, userRepository.countByEntrepriseIdAndRole(e.getId(), Role.EMPLOYE)))
                .toList();
    }

    // ---------- API interne (appelee par les autres microservices) ----------

    /** Fiche d'un utilisateur : l'admin, l'utilisateur lui-meme ou l'entreprise de l'employe. */
    @Transactional(readOnly = true)
    public UserDto utilisateur(Long id) {
        User appelant = currentUser.get();
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        boolean memeEntreprise = appelant.getRole() == Role.ENTREPRISE && u.getEntreprise() != null
                && u.getEntreprise().getId().equals(appelant.getEntreprise().getId());
        if (appelant.getRole() != Role.ADMIN && !appelant.getId().equals(id) && !memeEntreprise) {
            throw ApiException.forbidden("Acces refuse a cet utilisateur");
        }
        return UserMapper.toDto(u);
    }

    /** Indicateurs utilises par le tableau de bord du voyage-service. */
    @Transactional(readOnly = true)
    public UserStats statistiques() {
        User u = currentUser.get();
        return switch (u.getRole()) {
            case ADMIN -> {
                List<String> labels = new ArrayList<>();
                List<Number> data = new ArrayList<>();
                for (Object[] row : entrepriseRepository.countByDomaine()) {
                    labels.add(String.valueOf(row[0]));
                    data.add((Number) row[1]);
                }
                yield new UserStats(entrepriseRepository.count(), userRepository.countByRole(Role.EMPLOYE), 0,
                        new Serie(labels, data));
            }
            case ENTREPRISE -> {
                Long eid = u.getEntreprise().getId();
                yield new UserStats(1, userRepository.countByEntrepriseIdAndRole(eid, Role.EMPLOYE),
                        invitationRepository.countByEntrepriseIdAndStatut(eid, StatutInvitation.EN_ATTENTE), Serie.vide());
            }
            case EMPLOYE -> UserStats.vide();
        };
    }

    /**
     * Previent le voyage-service (OpenFeign + Eureka) qu'un employe est active / desactive.
     * Si le service est indisponible, on ne bloque pas l'operation.
     */
    void synchroniserVoyages(User u) {
        if (u.getRole() != Role.EMPLOYE) {
            return;
        }
        try {
            voyageClient.majEmployeActif(u.getId(), u.isActif());
        } catch (RuntimeException e) {
            log.warn("voyage-service injoignable, voyages de l'employe {} non synchronises : {}", u.getId(), e.getMessage());
        }
    }
}
