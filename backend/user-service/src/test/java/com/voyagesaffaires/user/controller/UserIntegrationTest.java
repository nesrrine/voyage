package com.voyagesaffaires.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test d'integration du user-service (Eureka desactive, base H2 en memoire avec le jeu de demonstration). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usertest;DB_CLOSE_DELAY=-1",
        "eureka.client.enabled=false",
        "app.demo-data=true"
})
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwtService;

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void leJetonContientLesInformationsUtilesAuxAutresServices() throws Exception {
        AuthUser u = jwtService.parse(token("amine@techsoft.tn", "password123"));

        assertThat(u.id()).isEqualTo(6L);
        assertThat(u.nomComplet()).isEqualTo("Amine Ben Ali");
        assertThat(u.entrepriseId()).isEqualTo(1L);
        assertThat(u.entrepriseNom()).isEqualTo("TechSoft");
        assertThat(u.poste()).isEqualTo("Chef de projet");
    }

    @Test
    void mauvaisMotDePasseRefuse() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"amine@techsoft.tn\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void sansJetonAccesRefuse() throws Exception {
        mvc.perform(get("/api/account/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void unEmployeNePeutPasGererLesInvitationsNiAdministrer() throws Exception {
        String employe = "Bearer " + token("amine@techsoft.tn", "password123");
        mvc.perform(get("/api/invitations").header("Authorization", employe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", employe)).andExpect(status().isForbidden());
    }

    @Test
    void lEntrepriseVoitSesEmployesEtLeurFiche() throws Exception {
        String entreprise = "Bearer " + token("contact@techsoft.tn", "password123");
        mvc.perform(get("/api/entreprise/employes").header("Authorization", entreprise))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/users/7").header("Authorization", entreprise))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Sarra"));
        mvc.perform(get("/api/users/9").header("Authorization", entreprise)).andExpect(status().isForbidden());
    }

    @Test
    void lAdministrateurVoitTousLesUtilisateursEtLesStatistiques() throws Exception {
        String admin = "Bearer " + token("admin@voyages-affaires.com", "admin123");
        mvc.perform(get("/api/admin/users").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11));
        mvc.perform(get("/api/users/stats").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entreprises").value(4))
                .andExpect(jsonPath("$.employes").value(6));
    }
}
