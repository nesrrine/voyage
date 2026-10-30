package com.voyagesaffaires.voyage.controller;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.JwtService;
import com.voyagesaffaires.voyage.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration du voyage-service seul (Eureka desactive, base H2 en memoire avec le jeu de demonstration).
 * Les jetons sont signes avec la meme cle que le user-service : c'est ainsi que les services se font confiance.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:voyagetest;DB_CLOSE_DELAY=-1",
        "eureka.client.enabled=false",
        "app.demo-data=true"
})
@AutoConfigureMockMvc
class VoyageIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;

    private final AuthUser sarra = TestData.employe(7, "Sarra", 1, "TechSoft", DomaineActivite.INFORMATIQUE);

    private String bearer(AuthUser u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    @Test
    void sansJetonAccesRefuse() throws Exception {
        mvc.perform(get("/api/voyages")).andExpect(status().isUnauthorized());
    }

    @Test
    void jetonFalsifieRefuse() throws Exception {
        mvc.perform(get("/api/voyages").header("Authorization", "Bearer faux.jeton.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void referentielDesVillesPublic() throws Exception {
        mvc.perform(get("/api/meta/villes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nom").exists());
    }

    @Test
    void lAdministrateurNePeutPasRepondreAuxInvitations() throws Exception {
        mvc.perform(get("/api/matchs/recues").header("Authorization", bearer(TestData.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void creationDUnVoyageLanceLeMatchingAvecLesVoyagesDeDemonstration() throws Exception {
        // Meme destination et dates que le voyage de Youssef (GreenPower, energie) qui recherche l'informatique
        String debut = LocalDate.now().plusDays(21).toString();
        String fin = LocalDate.now().plusDays(23).toString();
        mvc.perform(post("/api/voyages").header("Authorization", bearer(sarra))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Rendez-vous clients","villeDestination":"Paris","paysDestination":"France",
                                 "latitude":48.8566,"longitude":2.3522,"rayonKm":50,
                                 "dateDebut":"%s","dateFin":"%s","objet":"SALON","domainesRecherches":["ENERGIE"]}
                                """.formatted(debut, fin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voyage.employeNom").value("Sarra Test"))
                .andExpect(jsonPath("$.voyage.entrepriseNom").value("TechSoft"))
                .andExpect(jsonPath("$.nouveauxMatchs").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void voyageInvalideRenvoieLesErreursDeValidation() throws Exception {
        mvc.perform(post("/api/voyages").header("Authorization", bearer(sarra))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.titre").exists())
                .andExpect(jsonPath("$.erreurs.dateDebut").exists());
    }

    @Test
    void lesProgrammesNeMontrentPlusLesVoyagesDUnEmployeDesactive() throws Exception {
        AuthUser greenPower = TestData.entreprise(3, 2, "GreenPower", DomaineActivite.ENERGIE);
        mvc.perform(put("/api/voyages/employes/8/actif").param("actif", "false")
                        .header("Authorization", bearer(greenPower)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voyagesModifies").value(2));
        mvc.perform(get("/api/voyages/programmes").param("destination", "Casablanca")
                        .header("Authorization", bearer(sarra)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        // remise en etat pour les autres tests
        mvc.perform(put("/api/voyages/employes/8/actif").param("actif", "true")
                .header("Authorization", bearer(greenPower))).andExpect(status().isOk());
    }
}
