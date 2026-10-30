package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.voyage.entity.Voyage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest {

    private final MatchingService service = new MatchingService(null, null);
    private final LocalDate t = LocalDate.of(2026, 6, 1);

    private Voyage voyage(DomaineActivite domaineEntreprise, Set<DomaineActivite> recherches,
                          double lat, double lng, LocalDate debut, LocalDate fin, ObjetVoyage objet) {
        return Voyage.builder().employeId(1L).entrepriseId((long) domaineEntreprise.ordinal())
                .entrepriseDomaine(domaineEntreprise).latitude(lat).longitude(lng).rayonKm(50)
                .dateDebut(debut).dateFin(fin).objet(objet).domainesRecherches(new HashSet<>(recherches)).build();
    }

    @Test
    void distanceParisVersaillesEnviron17km() {
        double d = MatchingService.distanceKm(48.8566, 2.3522, 48.8049, 2.1204);
        assertThat(d).isBetween(15.0, 20.0);
    }

    @Test
    void interetMutuelMemesDatesMemeVilleDonne100() {
        Voyage a = voyage(DomaineActivite.INFORMATIQUE, Set.of(DomaineActivite.FINANCE), 48.85, 2.35, t, t.plusDays(3), ObjetVoyage.SALON);
        Voyage b = voyage(DomaineActivite.FINANCE, Set.of(DomaineActivite.INFORMATIQUE), 48.85, 2.35, t, t.plusDays(3), ObjetVoyage.SALON);
        assertThat(service.evaluer(a, b)).hasValueSatisfying(m -> assertThat(m.getScore()).isEqualTo(100.0));
    }

    @Test
    void datesSansChevauchementRejetees() {
        Voyage a = voyage(DomaineActivite.INFORMATIQUE, Set.of(), 48.85, 2.35, t, t.plusDays(2), ObjetVoyage.SALON);
        Voyage b = voyage(DomaineActivite.INFORMATIQUE, Set.of(), 48.85, 2.35, t.plusDays(5), t.plusDays(7), ObjetVoyage.SALON);
        assertThat(service.evaluer(a, b)).isEmpty();
    }

    @Test
    void horsPerimetreRejete() {
        Voyage paris = voyage(DomaineActivite.INFORMATIQUE, Set.of(), 48.8566, 2.3522, t, t.plusDays(2), ObjetVoyage.SALON);
        Voyage lyon = voyage(DomaineActivite.INFORMATIQUE, Set.of(), 45.7640, 4.8357, t, t.plusDays(2), ObjetVoyage.SALON);
        assertThat(service.evaluer(paris, lyon)).isEmpty();
    }

    @Test
    void domainesIncompatiblesRejetes() {
        Voyage a = voyage(DomaineActivite.INFORMATIQUE, Set.of(DomaineActivite.FINANCE), 48.85, 2.35, t, t.plusDays(2), ObjetVoyage.SALON);
        Voyage b = voyage(DomaineActivite.AGRICULTURE, Set.of(DomaineActivite.SANTE), 48.85, 2.35, t, t.plusDays(2), ObjetVoyage.SALON);
        assertThat(service.evaluer(a, b)).isEmpty();
    }

    @Test
    void interetDansUnSeulSensDonne70PourcentSurLeDomaine() {
        Voyage a = voyage(DomaineActivite.INFORMATIQUE, Set.of(DomaineActivite.COMMERCE), 48.85, 2.35, t, t.plusDays(2), ObjetVoyage.SALON);
        Voyage b = voyage(DomaineActivite.COMMERCE, Set.of(DomaineActivite.SANTE), 48.85, 2.35, t, t.plusDays(2), ObjetVoyage.SALON);
        assertThat(service.evaluer(a, b)).hasValueSatisfying(m -> assertThat(m.getScoreDomaine()).isEqualTo(70.0));
    }
}
