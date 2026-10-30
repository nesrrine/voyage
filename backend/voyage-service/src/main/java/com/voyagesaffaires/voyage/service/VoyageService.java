package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.SecurityUtils;
import com.voyagesaffaires.voyage.client.UserClient;
import com.voyagesaffaires.voyage.config.CacheConfig;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VoyageRequest;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VoyageResultat;
import com.voyagesaffaires.voyage.entity.Voyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import com.voyagesaffaires.voyage.repository.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VoyageService {

    private final VoyageRepository voyageRepository;
    private final MatchVoyageRepository matchRepository;
    private final MatchingService matchingService;
    private final MatchService matchService;
    private final UserClient userClient;
    private final ReferentielCache referentielCache;

    /** Planning : voyages de l'employe, ou de tous les employes pour une entreprise, ou tous pour l'admin. */
    @Transactional(readOnly = true)
    public List<VoyageDto> monPlanning() {
        AuthUser u = SecurityUtils.currentUser();
        List<Voyage> voyages = switch (u.role()) {
            case EMPLOYE -> voyageRepository.findByEmployeIdOrderByDateDebutDesc(u.id());
            case ENTREPRISE -> voyageRepository.findByEntrepriseIdOrderByDateDebutDesc(u.entrepriseId());
            case ADMIN -> voyageRepository.findAll();
        };
        return voyages.stream().map(VoyageMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VoyageDto consulter(Long id) {
        Voyage v = getVoyage(id);
        AuthUser u = SecurityUtils.currentUser();
        if (!v.isVisible() && !peutModifier(u, v) && u.role() != Role.ADMIN) {
            throw ApiException.forbidden("Ce voyage est prive");
        }
        return VoyageMapper.toDto(v);
    }

    /** Programmes de voyages publics (consultation + filtres), lus depuis le cache Caffeine. */
    public List<VoyageDto> programmes(String destination, ObjetVoyage objet, DomaineActivite domaine,
                                      LocalDate du, LocalDate au) {
        String dest = destination == null ? null : destination.trim().toLowerCase();
        return referentielCache.programmesAVenir(LocalDate.now()).stream()
                .filter(v -> dest == null || dest.isEmpty()
                        || v.villeDestination().toLowerCase().contains(dest)
                        || v.paysDestination().toLowerCase().contains(dest))
                .filter(v -> objet == null || v.objet() == objet)
                .filter(v -> domaine == null || v.entrepriseDomaine() == domaine)
                .filter(v -> du == null || !v.dateFin().isBefore(du))
                .filter(v -> au == null || !v.dateDebut().isAfter(au))
                .toList();
    }

    @CacheEvict(cacheNames = CacheConfig.PROGRAMMES, allEntries = true)
    @Transactional
    public VoyageResultat creer(VoyageRequest req) {
        AuthUser u = SecurityUtils.currentUser();
        valider(req);
        Voyage v = new Voyage();
        attribuerEmploye(v, u, req.employeId());
        appliquer(v, req);
        v = voyageRepository.save(v);
        int matchs = matchingService.lancer(v);
        return new VoyageResultat(VoyageMapper.toDto(v), matchs);
    }

    @CacheEvict(cacheNames = CacheConfig.PROGRAMMES, allEntries = true)
    @Transactional
    public VoyageResultat modifier(Long id, VoyageRequest req) {
        AuthUser u = SecurityUtils.currentUser();
        Voyage v = getVoyage(id);
        if (!peutModifier(u, v)) {
            throw ApiException.forbidden("Vous ne pouvez pas modifier ce voyage");
        }
        valider(req);
        appliquer(v, req);
        // Les invitations non encore traitees sont recalculees avec les nouvelles informations
        matchRepository.deleteByVoyageAndStatut(v.getId(), StatutMatch.EN_ATTENTE);
        int matchs = matchingService.lancer(v);
        return new VoyageResultat(VoyageMapper.toDto(v), matchs);
    }

    @CacheEvict(cacheNames = CacheConfig.PROGRAMMES, allEntries = true)
    @Transactional
    public void supprimer(Long id) {
        AuthUser u = SecurityUtils.currentUser();
        Voyage v = getVoyage(id);
        if (!peutModifier(u, v) && u.role() != Role.ADMIN) {
            throw ApiException.forbidden("Vous ne pouvez pas supprimer ce voyage");
        }
        matchRepository.deleteByVoyage(v.getId());
        voyageRepository.delete(v);
    }

    @Transactional
    public int relancerMatching(Long id) {
        AuthUser u = SecurityUtils.currentUser();
        Voyage v = getVoyage(id);
        if (!peutModifier(u, v)) {
            throw ApiException.forbidden("Vous ne pouvez pas lancer le matching pour ce voyage");
        }
        return matchingService.lancer(v);
    }

    /** Partenaires trouves pour un voyage (invitations envoyees et recues). */
    @Transactional(readOnly = true)
    public List<MatchDto> matchsDuVoyage(Long id) {
        AuthUser u = SecurityUtils.currentUser();
        Voyage v = getVoyage(id);
        if (!peutModifier(u, v) && u.role() != Role.ADMIN) {
            throw ApiException.forbidden("Acces refuse");
        }
        AuthUser proprietaire = new AuthUser(v.getEmployeId(), null, Role.EMPLOYE, null, null,
                v.getEntrepriseId(), null, null, null, null);
        return matchRepository.findByVoyage(id).stream().map(m -> matchService.toDto(m, proprietaire)).toList();
    }

    /**
     * Appele par le user-service quand un employe est active / desactive :
     * ses voyages sortent (ou reviennent) dans les programmes et le matching.
     */
    @CacheEvict(cacheNames = CacheConfig.PROGRAMMES, allEntries = true)
    @Transactional
    public int majEmployeActif(Long employeId, boolean actif) {
        AuthUser u = SecurityUtils.currentUser();
        return switch (u.role()) {
            case ADMIN -> voyageRepository.majEmployeActif(employeId, actif, null);
            case ENTREPRISE -> voyageRepository.majEmployeActif(employeId, actif, u.entrepriseId());
            case EMPLOYE -> {
                if (!u.id().equals(employeId)) {
                    throw ApiException.forbidden("Acces refuse");
                }
                yield voyageRepository.majEmployeActif(employeId, actif, null);
            }
        };
    }

    // ---------------------------------------------------------------------

    /**
     * L'employe vient du jeton JWT (role EMPLOYE) ou du user-service via OpenFeign
     * (role ENTREPRISE qui planifie le voyage d'un de ses employes).
     */
    private void attribuerEmploye(Voyage v, AuthUser u, Long employeId) {
        switch (u.role()) {
            case EMPLOYE -> {
                v.setEmployeId(u.id());
                v.setEmployeNom(u.nomComplet());
                v.setEmployePoste(u.poste());
                v.setEmployeEmail(u.email());
                v.setEmployeTelephone(u.telephone());
                v.setEntrepriseId(u.entrepriseId());
                v.setEntrepriseNom(u.entrepriseNom());
                v.setEntrepriseDomaine(u.domaine());
            }
            case ENTREPRISE -> {
                if (employeId == null) {
                    throw ApiException.badRequest("Veuillez choisir l'employe concerne par ce voyage");
                }
                UserDto emp = userClient.utilisateur(employeId);
                if (!Objects.equals(emp.entrepriseId(), u.entrepriseId()) || emp.role() != Role.EMPLOYE) {
                    throw ApiException.forbidden("Cet employe n'appartient pas a votre entreprise");
                }
                if (!emp.actif()) {
                    throw ApiException.badRequest("Cet employe est desactive");
                }
                v.setEmployeId(emp.id());
                v.setEmployeNom(emp.nomComplet());
                v.setEmployePoste(emp.poste());
                v.setEmployeEmail(emp.email());
                v.setEmployeTelephone(emp.telephone());
                v.setEntrepriseId(emp.entrepriseId());
                v.setEntrepriseNom(emp.entrepriseNom());
                v.setEntrepriseDomaine(emp.domaineActivite());
            }
            case ADMIN -> throw ApiException.forbidden("L'administrateur ne peut pas planifier de voyage");
        }
    }

    private boolean peutModifier(AuthUser u, Voyage v) {
        if (Objects.equals(v.getEmployeId(), u.id())) {
            return true;
        }
        return u.role() == Role.ENTREPRISE && Objects.equals(v.getEntrepriseId(), u.entrepriseId());
    }

    private void valider(VoyageRequest req) {
        if (req.dateFin().isBefore(req.dateDebut())) {
            throw ApiException.badRequest("La date de fin doit etre posterieure a la date de debut");
        }
        if (req.dateFin().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Impossible de planifier un voyage entierement dans le passe");
        }
    }

    private void appliquer(Voyage v, VoyageRequest req) {
        v.setTitre(req.titre());
        v.setVilleDestination(req.villeDestination());
        v.setPaysDestination(req.paysDestination());
        v.setLatitude(req.latitude());
        v.setLongitude(req.longitude());
        v.setRayonKm(req.rayonKm() == null ? 50 : req.rayonKm());
        v.setDateDebut(req.dateDebut());
        v.setDateFin(req.dateFin());
        v.setObjet(req.objet());
        v.setDescription(req.description());
        v.setDomainesRecherches(req.domainesRecherches() == null ? new HashSet<>() : new HashSet<>(req.domainesRecherches()));
        v.setVisible(req.visible() == null || req.visible());
    }

    private Voyage getVoyage(Long id) {
        return voyageRepository.findById(id).orElseThrow(() -> ApiException.notFound("Voyage introuvable"));
    }
}
