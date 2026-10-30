package com.voyagesaffaires.voyage;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.voyage.entity.MatchVoyage;
import com.voyagesaffaires.voyage.entity.Voyage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Donnees de test du voyage-service. */
public final class TestData {

    private TestData() {
    }

    public static AuthUser employe(long id, String prenom, long entrepriseId, String entreprise, DomaineActivite domaine) {
        return new AuthUser(id, prenom.toLowerCase() + "@test.com", Role.EMPLOYE, prenom + " Test", prenom,
                entrepriseId, entreprise, domaine, "Ingenieur", "+216 00 00 00 " + id);
    }

    public static AuthUser entreprise(long id, long entrepriseId, String nom, DomaineActivite domaine) {
        return new AuthUser(id, "contact@" + nom.toLowerCase() + ".com", Role.ENTREPRISE, "Resp " + nom, "Resp",
                entrepriseId, nom, domaine, "Responsable", null);
    }

    public static AuthUser admin() {
        return new AuthUser(1L, "admin@test.com", Role.ADMIN, "Admin", "Admin", null, null, null, null, null);
    }

    /** Simule l'utilisateur authentifie par le filtre JWT. */
    public static void connecter(AuthUser u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.authorities()));
    }

    public static Voyage voyage(long id, AuthUser e, String ville, LocalDate debut, LocalDate fin) {
        return Voyage.builder().id(id).titre("Voyage " + id)
                .employeId(e.id()).employeNom(e.nomComplet()).employePoste(e.poste())
                .employeEmail(e.email()).employeTelephone(e.telephone())
                .entrepriseId(e.entrepriseId()).entrepriseNom(e.entrepriseNom()).entrepriseDomaine(e.domaine())
                .villeDestination(ville).paysDestination("France").latitude(48.85).longitude(2.35).rayonKm(50)
                .dateDebut(debut).dateFin(fin).objet(ObjetVoyage.SALON)
                .domainesRecherches(new HashSet<>(Set.of(DomaineActivite.FINANCE))).build();
    }

    public static MatchVoyage match(long id, Voyage source, Voyage cible) {
        return MatchVoyage.builder().id(id).voyageSource(source).voyageCible(cible).score(80).scoreDomaine(100)
                .scoreDate(75).scoreDistance(90).scoreObjet(100).distanceKm(5).joursCommuns(3).build();
    }
}
