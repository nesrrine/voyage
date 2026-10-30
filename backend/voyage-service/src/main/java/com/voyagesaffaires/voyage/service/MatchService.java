package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.SecurityUtils;
import com.voyagesaffaires.voyage.entity.MatchVoyage;
import com.voyagesaffaires.voyage.entity.Voyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Processus d'invitation aux voyages : reception, acceptation, refus et mise en contact. */
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchVoyageRepository matchRepository;

    @Transactional(readOnly = true)
    public List<MatchDto> recues() {
        AuthUser u = SecurityUtils.currentUser();
        return matchRepository.findByVoyageCibleEmployeIdOrderByDateCreationDesc(u.id())
                .stream().map(m -> toDto(m, u)).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchDto> envoyees() {
        AuthUser u = SecurityUtils.currentUser();
        return matchRepository.findByVoyageSourceEmployeIdOrderByDateCreationDesc(u.id())
                .stream().map(m -> toDto(m, u)).toList();
    }

    /** Detail d'une invitation (utilise par l'ai-service) : reserve aux participants et a leurs entreprises. */
    @Transactional(readOnly = true)
    public MatchDto consulter(Long id) {
        AuthUser u = SecurityUtils.currentUser();
        MatchVoyage m = getMatch(id);
        if (!concerne(m.getVoyageCible(), u) && !concerne(m.getVoyageSource(), u)) {
            throw ApiException.forbidden("Vous ne participez pas a ce voyage commun");
        }
        return toDto(m, u);
    }

    @Transactional
    public MatchDto accepter(Long id) {
        return repondre(id, StatutMatch.ACCEPTEE);
    }

    @Transactional
    public MatchDto refuser(Long id) {
        return repondre(id, StatutMatch.REFUSEE);
    }

    private MatchDto repondre(Long id, StatutMatch statut) {
        AuthUser u = SecurityUtils.currentUser();
        MatchVoyage m = getMatch(id);
        if (!m.getVoyageCible().getEmployeId().equals(u.id())) {
            throw ApiException.forbidden("Seul le destinataire peut repondre a cette invitation");
        }
        if (m.getStatut() != StatutMatch.EN_ATTENTE) {
            throw ApiException.badRequest("Vous avez deja repondu a cette invitation");
        }
        m.setStatut(statut);
        m.setDateReponse(LocalDateTime.now());
        return toDto(m, u);
    }

    /** Construit la vue d'une invitation du point de vue d'un utilisateur (son voyage / celui du partenaire). */
    public MatchDto toDto(MatchVoyage m, AuthUser viewer) {
        boolean recue = concerne(m.getVoyageCible(), viewer);
        Voyage mien = recue ? m.getVoyageCible() : m.getVoyageSource();
        Voyage autre = recue ? m.getVoyageSource() : m.getVoyageCible();
        boolean contactVisible = m.getStatut() == StatutMatch.ACCEPTEE;
        return new MatchDto(m.getId(), m.getScore(), m.getScoreDomaine(), m.getScoreDate(), m.getScoreDistance(),
                m.getScoreObjet(), m.getDistanceKm(), m.getJoursCommuns(), m.getStatut(),
                m.getDateCreation(), m.getDateReponse(), recue,
                VoyageMapper.toDto(mien), VoyageMapper.toDto(autre),
                autre.getEmployeNom(), autre.getEmployePoste(), autre.getEntrepriseNom(), autre.getEntrepriseDomaine(),
                contactVisible ? autre.getEmployeEmail() : null,
                contactVisible ? autre.getEmployeTelephone() : null);
    }

    private MatchVoyage getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Invitation de voyage introuvable"));
    }

    private static boolean concerne(Voyage v, AuthUser u) {
        return v.getEmployeId().equals(u.id())
                || (u.role() == Role.ENTREPRISE && Objects.equals(v.getEntrepriseId(), u.entrepriseId()));
    }
}
