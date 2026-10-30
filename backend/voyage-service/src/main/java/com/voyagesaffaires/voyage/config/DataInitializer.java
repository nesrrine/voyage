package com.voyagesaffaires.voyage.config;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.voyage.entity.Ville;
import com.voyagesaffaires.voyage.entity.Voyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import com.voyagesaffaires.voyage.repository.VilleRepository;
import com.voyagesaffaires.voyage.repository.VoyageRepository;
import com.voyagesaffaires.voyage.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Referentiel des villes et voyages de demonstration.
 * Les identifiants des employes et entreprises correspondent au jeu de demonstration du user-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    /** Employe de demonstration (copie des donnees du user-service). */
    private record Emp(long id, String nom, String poste, String email, String tel,
                       long entrepriseId, String entreprise, DomaineActivite domaine) {
    }

    private static final Emp AMINE = new Emp(6, "Amine Ben Ali", "Chef de projet", "amine@techsoft.tn", "+216 70 000 005", 1, "TechSoft", DomaineActivite.INFORMATIQUE);
    private static final Emp SARRA = new Emp(7, "Sarra Trabelsi", "Responsable commerciale", "sarra@techsoft.tn", "+216 70 000 006", 1, "TechSoft", DomaineActivite.INFORMATIQUE);
    private static final Emp YOUSSEF = new Emp(8, "Youssef Gharbi", "Ingenieur", "youssef@greenpower.tn", "+216 70 000 007", 2, "GreenPower", DomaineActivite.ENERGIE);
    private static final Emp CLAIRE = new Emp(9, "Claire Martin", "Consultante senior", "claire@finconseil.fr", "+33 6 00 00 00 08", 3, "FinConseil", DomaineActivite.FINANCE);
    private static final Emp KARIM = new Emp(10, "Karim Haddad", "Analyste", "karim@finconseil.fr", "+33 6 00 00 00 09", 3, "FinConseil", DomaineActivite.FINANCE);
    private static final Emp NADIA = new Emp(11, "Nadia Alaoui", "Acheteuse", "nadia@medtrade.ma", "+212 6 00 00 00 10", 4, "MedTrade", DomaineActivite.COMMERCE);

    private final VilleRepository villeRepository;
    private final VoyageRepository voyageRepository;
    private final MatchVoyageRepository matchRepository;
    private final MatchingService matchingService;

    @Value("${app.demo-data:true}")
    private boolean demoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (villeRepository.count() == 0) {
            chargerVilles();
        }
        if (demoData && voyageRepository.count() == 0) {
            chargerVoyages();
        }
    }

    private void chargerVilles() {
        Object[][] villes = {
                {"Tunis", "Tunisie", 36.8065, 10.1815}, {"Sfax", "Tunisie", 34.7406, 10.7603},
                {"Sousse", "Tunisie", 35.8256, 10.6360}, {"Monastir", "Tunisie", 35.7643, 10.8113},
                {"Nabeul", "Tunisie", 36.4561, 10.7376}, {"Bizerte", "Tunisie", 37.2744, 9.8739},
                {"Paris", "France", 48.8566, 2.3522}, {"Versailles", "France", 48.8049, 2.1204},
                {"Lyon", "France", 45.7640, 4.8357}, {"Marseille", "France", 43.2965, 5.3698},
                {"Toulouse", "France", 43.6047, 1.4442}, {"Londres", "Royaume-Uni", 51.5074, -0.1278},
                {"Berlin", "Allemagne", 52.5200, 13.4050}, {"Munich", "Allemagne", 48.1351, 11.5820},
                {"Francfort", "Allemagne", 50.1109, 8.6821}, {"Madrid", "Espagne", 40.4168, -3.7038},
                {"Barcelone", "Espagne", 41.3874, 2.1686}, {"Rome", "Italie", 41.9028, 12.4964},
                {"Milan", "Italie", 45.4642, 9.1900}, {"Bruxelles", "Belgique", 50.8503, 4.3517},
                {"Amsterdam", "Pays-Bas", 52.3676, 4.9041}, {"Genève", "Suisse", 46.2044, 6.1432},
                {"Zurich", "Suisse", 47.3769, 8.5417}, {"Istanbul", "Turquie", 41.0082, 28.9784},
                {"Dubaï", "Émirats arabes unis", 25.2048, 55.2708}, {"Abu Dhabi", "Émirats arabes unis", 24.4539, 54.3773},
                {"Doha", "Qatar", 25.2854, 51.5310}, {"Riyad", "Arabie saoudite", 24.7136, 46.6753},
                {"Le Caire", "Égypte", 30.0444, 31.2357}, {"Casablanca", "Maroc", 33.5731, -7.5898},
                {"Rabat", "Maroc", 34.0209, -6.8416}, {"Alger", "Algérie", 36.7538, 3.0588},
                {"Dakar", "Sénégal", 14.7167, -17.4677}, {"Abidjan", "Côte d'Ivoire", 5.3600, -4.0083},
                {"Montréal", "Canada", 45.5019, -73.5674}, {"New York", "États-Unis", 40.7128, -74.0060},
                {"San Francisco", "États-Unis", 37.7749, -122.4194}, {"Shanghai", "Chine", 31.2304, 121.4737},
                {"Pékin", "Chine", 39.9042, 116.4074}, {"Tokyo", "Japon", 35.6762, 139.6503},
                {"Singapour", "Singapour", 1.3521, 103.8198}
        };
        for (Object[] v : villes) {
            villeRepository.save(Ville.builder()
                    .nom((String) v[0]).pays((String) v[1])
                    .latitude((Double) v[2]).longitude((Double) v[3])
                    .build());
        }
        log.info("{} villes chargees dans le referentiel", villes.length);
    }

    private void chargerVoyages() {
        LocalDate t = LocalDate.now();
        List<Voyage> voyages = List.of(
                voyage(AMINE, "Salon VivaTech", "Paris", "France", 48.8566, 2.3522, t.plusDays(20), t.plusDays(23),
                        ObjetVoyage.SALON, Set.of(DomaineActivite.FINANCE, DomaineActivite.ENERGIE)),
                voyage(YOUSSEF, "Salon des energies renouvelables", "Paris", "France", 48.8566, 2.3522,
                        t.plusDays(21), t.plusDays(25), ObjetVoyage.SALON, Set.of(DomaineActivite.INFORMATIQUE)),
                voyage(NADIA, "Visite de fournisseurs", "Versailles", "France", 48.8049, 2.1204,
                        t.plusDays(22), t.plusDays(24), ObjetVoyage.VISITE_ENTREPRISE, Set.of(DomaineActivite.INFORMATIQUE)),
                voyage(SARRA, "Conference GITEX", "Dubaï", "Émirats arabes unis", 25.2048, 55.2708,
                        t.plusDays(40), t.plusDays(44), ObjetVoyage.CONFERENCE, Set.of(DomaineActivite.FINANCE)),
                voyage(KARIM, "Forum FinTech Golfe", "Dubaï", "Émirats arabes unis", 25.2048, 55.2708,
                        t.plusDays(41), t.plusDays(43), ObjetVoyage.CONFERENCE, Set.of(DomaineActivite.INFORMATIQUE)),
                voyage(CLAIRE, "Prospection clients", "Tunis", "Tunisie", 36.8065, 10.1815,
                        t.plusDays(10), t.plusDays(12), ObjetVoyage.PROSPECTION,
                        Set.of(DomaineActivite.INFORMATIQUE, DomaineActivite.ENERGIE)),
                voyage(YOUSSEF, "Reunion partenaires", "Casablanca", "Maroc", 33.5731, -7.5898,
                        t.plusDays(60), t.plusDays(62), ObjetVoyage.REUNION, Set.of(DomaineActivite.COMMERCE)),
                voyage(AMINE, "Formation Cloud", "Lyon", "France", 45.7640, 4.8357,
                        t.minusDays(40), t.minusDays(38), ObjetVoyage.FORMATION, Set.of()));
        voyages.forEach(matchingService::lancer);

        // Une invitation deja acceptee pour illustrer la mise en contact
        matchRepository.findByVoyageCibleEmployeIdOrderByDateCreationDesc(KARIM.id()).stream().findFirst()
                .ifPresent(m -> {
                    m.setStatut(StatutMatch.ACCEPTEE);
                    m.setDateReponse(LocalDateTime.now());
                });
        log.info("{} voyages de demonstration charges", voyages.size());
    }

    private Voyage voyage(Emp e, String titre, String ville, String pays, double lat, double lng,
                          LocalDate debut, LocalDate fin, ObjetVoyage objet, Set<DomaineActivite> domaines) {
        return voyageRepository.save(Voyage.builder()
                .employeId(e.id()).employeNom(e.nom()).employePoste(e.poste())
                .employeEmail(e.email()).employeTelephone(e.tel())
                .entrepriseId(e.entrepriseId()).entrepriseNom(e.entreprise()).entrepriseDomaine(e.domaine())
                .titre(titre).villeDestination(ville).paysDestination(pays)
                .latitude(lat).longitude(lng).rayonKm(50)
                .dateDebut(debut).dateFin(fin).objet(objet)
                .description(titre + " - " + ville)
                .domainesRecherches(new HashSet<>(domaines))
                .build());
    }
}
