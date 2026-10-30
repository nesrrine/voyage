package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.voyage.entity.MatchVoyage;
import com.voyagesaffaires.voyage.entity.Voyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import com.voyagesaffaires.voyage.repository.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

/**
 * Algorithme de matching des voyages d'affaires.
 *
 * <p>Deux voyages sont compatibles s'ils appartiennent a deux entreprises differentes et que :
 * <ol>
 *   <li><b>Date</b> : leurs periodes se chevauchent d'au moins un jour ;</li>
 *   <li><b>Destination &amp; perimetre</b> : la distance (formule de Haversine) entre les deux
 *       destinations est inferieure au plus grand des deux perimetres ;</li>
 *   <li><b>Domaine d'activite</b> : l'entreprise de l'un correspond aux domaines recherches par l'autre
 *       (ou les deux entreprises sont du meme domaine).</li>
 * </ol>
 *
 * <p>Un score sur 100 est ensuite calcule :
 * {@code 35% domaine + 30% chevauchement des dates + 25% proximite + 10% meme objet de voyage}.
 * Si le score atteint le seuil configure, une invitation de voyage est envoyee au partenaire.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final double POIDS_DOMAINE = 0.35;
    private static final double POIDS_DATE = 0.30;
    private static final double POIDS_DISTANCE = 0.25;
    private static final double POIDS_OBJET = 0.10;
    private static final double RAYON_TERRE_KM = 6371.0;

    private final VoyageRepository voyageRepository;
    private final MatchVoyageRepository matchRepository;

    @Value("${app.matching.seuil:40}")
    private double seuil;

    /**
     * Lance la recherche de partenaires pour un voyage et cree les invitations correspondantes.
     *
     * @return le nombre de nouvelles invitations de voyage creees
     */
    @Transactional
    public int lancer(Voyage voyage) {
        if (!voyage.isVisible() || !voyage.isEmployeActif() || voyage.getDateFin().isBefore(LocalDate.now())) {
            return 0;
        }
        int crees = 0;
        for (Voyage candidat : voyageRepository.findCandidats(voyage.getId(), voyage.getEntrepriseId(),
                voyage.getDateDebut(), voyage.getDateFin())) {
            if (matchRepository.existsBetween(voyage.getId(), candidat.getId())) {
                continue;
            }
            Optional<MatchVoyage> match = evaluer(voyage, candidat);
            if (match.isPresent() && match.get().getScore() >= seuil) {
                matchRepository.save(match.get());
                crees++;
            }
        }
        return crees;
    }

    /** Calcule la compatibilite entre deux voyages (sans persister). Vide si un critere obligatoire echoue. */
    public Optional<MatchVoyage> evaluer(Voyage a, Voyage b) {
        // 1. Dates
        long joursCommuns = joursCommuns(a.getDateDebut(), a.getDateFin(), b.getDateDebut(), b.getDateFin());
        if (joursCommuns <= 0) {
            return Optional.empty();
        }
        double scoreDate = (double) joursCommuns / Math.min(a.getDureeJours(), b.getDureeJours());

        // 2. Destination & perimetre
        double distance = distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        double perimetre = Math.max(a.getRayonKm(), b.getRayonKm());
        if (distance > perimetre) {
            return Optional.empty();
        }
        double scoreDistance = perimetre == 0 ? 1 : 1 - distance / perimetre;

        // 3. Domaine d'activite
        double scoreDomaine = scoreDomaine(a, b);
        if (scoreDomaine <= 0) {
            return Optional.empty();
        }

        // 4. Objet du voyage (bonus)
        double scoreObjet = a.getObjet() == b.getObjet() ? 1 : 0;

        double score = 100 * (POIDS_DOMAINE * scoreDomaine + POIDS_DATE * scoreDate
                + POIDS_DISTANCE * scoreDistance + POIDS_OBJET * scoreObjet);

        return Optional.of(MatchVoyage.builder()
                .voyageSource(a)
                .voyageCible(b)
                .score(arrondi(score))
                .scoreDomaine(arrondi(scoreDomaine * 100))
                .scoreDate(arrondi(scoreDate * 100))
                .scoreDistance(arrondi(scoreDistance * 100))
                .scoreObjet(scoreObjet * 100)
                .distanceKm(arrondi(distance))
                .joursCommuns(joursCommuns)
                .build());
    }

    /**
     * 1.0 : interet mutuel (chacun recherche le domaine de l'autre)<br>
     * 0.7 : interet dans un seul sens<br>
     * 0.5 : meme domaine d'activite, ou aucun domaine precise (ouvert a tous)<br>
     * 0.0 : aucune affinite
     */
    double scoreDomaine(Voyage a, Voyage b) {
        DomaineActivite domA = a.getEntrepriseDomaine();
        DomaineActivite domB = b.getEntrepriseDomaine();
        Set<DomaineActivite> rechA = a.getDomainesRecherches();
        Set<DomaineActivite> rechB = b.getDomainesRecherches();

        boolean aVeutB = rechA.contains(domB);
        boolean bVeutA = rechB.contains(domA);

        if (aVeutB && bVeutA) {
            return 1.0;
        }
        if (aVeutB || bVeutA) {
            return 0.7;
        }
        if (domA == domB || (rechA.isEmpty() && rechB.isEmpty())) {
            return 0.5;
        }
        return 0.0;
    }

    static long joursCommuns(LocalDate debutA, LocalDate finA, LocalDate debutB, LocalDate finB) {
        LocalDate debut = debutA.isAfter(debutB) ? debutA : debutB;
        LocalDate fin = finA.isBefore(finB) ? finA : finB;
        return fin.isBefore(debut) ? 0 : ChronoUnit.DAYS.between(debut, fin) + 1;
    }

    /** Distance a vol d'oiseau entre deux points GPS (formule de Haversine). */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.sqrt(h));
    }

    private static double arrondi(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
