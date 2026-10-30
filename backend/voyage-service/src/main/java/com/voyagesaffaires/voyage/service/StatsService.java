package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.StatsDtos.*;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.SecurityUtils;
import com.voyagesaffaires.voyage.client.ForumClient;
import com.voyagesaffaires.voyage.client.UserClient;
import com.voyagesaffaires.voyage.entity.MatchVoyage;
import com.voyagesaffaires.voyage.entity.Voyage;
import com.voyagesaffaires.voyage.repository.MatchVoyageRepository;
import com.voyagesaffaires.voyage.repository.VoyageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * Tableau de bord d'aide a la decision. Les donnees des voyages sont locales ;
 * les indicateurs utilisateurs et forum sont demandes au user-service et au forum-service
 * via OpenFeign. Si un service est indisponible, le tableau de bord s'affiche quand meme.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MM/yyyy");

    private final VoyageRepository voyageRepository;
    private final MatchVoyageRepository matchRepository;
    private final UserClient userClient;
    private final ForumClient forumClient;

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        AuthUser u = SecurityUtils.currentUser();
        return switch (u.role()) {
            case ADMIN -> admin();
            case ENTREPRISE -> entreprise(u.entrepriseId());
            case EMPLOYE -> employe(u.id());
        };
    }

    private DashboardDto admin() {
        UserStats users = appel("user-service", userClient::statistiques, UserStats.vide());
        ForumStats forum = appel("forum-service", forumClient::statistiques, ForumStats.vide());
        List<CarteStat> cartes = List.of(
                new CarteStat("Entreprises", users.entreprises(), "business"),
                new CarteStat("Employes", users.employes(), "groups"),
                new CarteStat("Voyages", voyageRepository.count(), "flight_takeoff"),
                new CarteStat("Partenariats", matchRepository.countByStatut(StatutMatch.ACCEPTEE), "handshake"),
                new CarteStat("Publications", forum.publications(), "forum"));
        Map<String, Serie> g = new LinkedHashMap<>();
        g.put("voyagesParMois", parMois(voyageRepository.findAll()));
        g.put("topDestinations", serie(voyageRepository.topDestinations(), 6));
        g.put("voyagesParObjet", serie(voyageRepository.countByObjet(), 0));
        g.put("matchsParStatut", serie(matchRepository.countGroupByStatut(), 0));
        g.put("entreprisesParDomaine", users.entreprisesParDomaine());
        g.put("forumParCategorie", forum.parCategorie());
        return new DashboardDto(cartes, g);
    }

    private DashboardDto entreprise(Long eid) {
        UserStats users = appel("user-service", userClient::statistiques, UserStats.vide());
        List<MatchVoyage> acceptes = matchRepository.findAcceptesEntreprise(eid);
        Map<String, Long> partenaires = new TreeMap<>();
        Set<Long> entreprisesPartenaires = new HashSet<>();
        for (MatchVoyage m : acceptes) {
            Voyage autre = m.getVoyageSource().getEntrepriseId().equals(eid) ? m.getVoyageCible() : m.getVoyageSource();
            if (entreprisesPartenaires.add(autre.getEntrepriseId()) && autre.getEntrepriseDomaine() != null) {
                partenaires.merge(autre.getEntrepriseDomaine().name(), 1L, Long::sum);
            }
        }
        List<CarteStat> cartes = List.of(
                new CarteStat("Employes", users.employes(), "groups"),
                new CarteStat("Invitations en attente", users.invitationsEnAttente(), "mail"),
                new CarteStat("Voyages", voyageRepository.countByEntrepriseId(eid), "flight_takeoff"),
                new CarteStat("Voyages communs acceptes", acceptes.size(), "handshake"),
                new CarteStat("Entreprises partenaires", entreprisesPartenaires.size(), "business"));
        Map<String, Serie> g = new LinkedHashMap<>();
        g.put("voyagesParMois", parMois(voyageRepository.findByEntrepriseIdOrderByDateDebutDesc(eid)));
        g.put("topDestinations", serie(voyageRepository.topDestinationsEntreprise(eid), 6));
        g.put("voyagesParObjet", serie(voyageRepository.countByObjetEntreprise(eid), 0));
        g.put("matchsParStatut", serie(matchRepository.countGroupByStatutEntreprise(eid), 0));
        g.put("partenairesParDomaine", new Serie(new ArrayList<>(partenaires.keySet()), new ArrayList<>(partenaires.values())));
        return new DashboardDto(cartes, g);
    }

    private DashboardDto employe(Long uid) {
        List<Voyage> voyages = voyageRepository.findByEmployeIdOrderByDateDebutDesc(uid);
        LocalDate today = LocalDate.now();
        long aVenir = voyages.stream().filter(v -> !v.getDateFin().isBefore(today)).count();
        long recuesEnAttente = matchRepository.findByVoyageCibleEmployeIdOrderByDateCreationDesc(uid).stream()
                .filter(m -> m.getStatut() == StatutMatch.EN_ATTENTE).count();
        Map<String, Long> statuts = new LinkedHashMap<>();
        for (Object[] row : matchRepository.countGroupByStatutEmploye(uid)) {
            statuts.put(String.valueOf(row[0]), (Long) row[1]);
        }
        Map<String, Long> objets = new TreeMap<>();
        voyages.forEach(v -> objets.merge(v.getObjet().name(), 1L, Long::sum));

        List<CarteStat> cartes = List.of(
                new CarteStat("Mes voyages", voyages.size(), "flight_takeoff"),
                new CarteStat("Voyages a venir", aVenir, "event"),
                new CarteStat("Invitations a traiter", recuesEnAttente, "mail"),
                new CarteStat("Partenaires de voyage", statuts.getOrDefault("ACCEPTEE", 0L), "handshake"));
        Map<String, Serie> g = new LinkedHashMap<>();
        g.put("voyagesParMois", parMois(voyages));
        g.put("matchsParStatut", new Serie(new ArrayList<>(statuts.keySet()), new ArrayList<>(statuts.values())));
        g.put("voyagesParObjet", new Serie(new ArrayList<>(objets.keySet()), new ArrayList<>(objets.values())));
        return new DashboardDto(cartes, g);
    }

    /** Appel d'un autre microservice avec valeur de repli s'il ne repond pas. */
    private <T> T appel(String service, Supplier<T> appel, T repli) {
        try {
            return appel.get();
        } catch (RuntimeException e) {
            log.warn("{} indisponible pour le tableau de bord : {}", service, e.getMessage());
            return repli;
        }
    }

    /** Nombre de voyages par mois de depart, sur 12 mois glissants (5 mois passes, mois courant, 6 a venir). */
    private Serie parMois(List<Voyage> voyages) {
        YearMonth debut = YearMonth.now().minusMonths(5);
        Map<YearMonth, Long> compte = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            compte.put(debut.plusMonths(i), 0L);
        }
        voyages.forEach(v -> compte.computeIfPresent(YearMonth.from(v.getDateDebut()), (k, n) -> n + 1));
        List<String> labels = compte.keySet().stream().map(MOIS::format).toList();
        return new Serie(labels, new ArrayList<>(compte.values()));
    }

    private Serie serie(List<Object[]> rows, int limite) {
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        for (Object[] row : rows) {
            if (limite > 0 && labels.size() >= limite) {
                break;
            }
            labels.add(String.valueOf(row[0]));
            data.add((Number) row[1]);
        }
        return new Serie(labels, data);
    }
}
