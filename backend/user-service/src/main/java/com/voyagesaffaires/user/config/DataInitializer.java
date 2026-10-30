package com.voyagesaffaires.user.config;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.user.entity.Entreprise;
import com.voyagesaffaires.user.entity.Invitation;
import com.voyagesaffaires.user.entity.User;
import com.voyagesaffaires.user.repository.EntrepriseRepository;
import com.voyagesaffaires.user.repository.InvitationRepository;
import com.voyagesaffaires.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Jeu de demonstration du user-service. Les identifiants crees (admin = 1, employes 6 a 11)
 * sont repris par les jeux de demonstration du voyage-service et du forum-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String MDP_DEMO = "password123";

    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-data:true}")
    private boolean demoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(User.builder()
                .nom("Plateforme").prenom("Admin")
                .email("admin@voyages-affaires.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());
        if (!demoData) {
            return;
        }
        Entreprise techsoft = entreprise("TechSoft", DomaineActivite.INFORMATIQUE, "Tunisie", "Tunis",
                "Editeur de logiciels de gestion pour PME.");
        Entreprise green = entreprise("GreenPower", DomaineActivite.ENERGIE, "Tunisie", "Sfax",
                "Installation de centrales solaires.");
        Entreprise fin = entreprise("FinConseil", DomaineActivite.FINANCE, "France", "Paris",
                "Cabinet de conseil en financement de projets.");
        Entreprise med = entreprise("MedTrade", DomaineActivite.COMMERCE, "Maroc", "Casablanca",
                "Import-export dans le bassin mediterraneen.");

        compte(techsoft, Role.ENTREPRISE, "Mansour", "Hichem", "contact@techsoft.tn", "Directeur general", "+216 70 000 001");
        compte(green, Role.ENTREPRISE, "Kallel", "Mouna", "contact@greenpower.tn", "Directrice", "+216 70 000 002");
        compte(fin, Role.ENTREPRISE, "Durand", "Paul", "contact@finconseil.fr", "Associe", "+33 1 00 00 00 03");
        compte(med, Role.ENTREPRISE, "Bennani", "Omar", "contact@medtrade.ma", "Gerant", "+212 5 00 00 00 04");

        compte(techsoft, Role.EMPLOYE, "Ben Ali", "Amine", "amine@techsoft.tn", "Chef de projet", "+216 70 000 005");
        compte(techsoft, Role.EMPLOYE, "Trabelsi", "Sarra", "sarra@techsoft.tn", "Responsable commerciale", "+216 70 000 006");
        compte(green, Role.EMPLOYE, "Gharbi", "Youssef", "youssef@greenpower.tn", "Ingenieur", "+216 70 000 007");
        compte(fin, Role.EMPLOYE, "Martin", "Claire", "claire@finconseil.fr", "Consultante senior", "+33 6 00 00 00 08");
        compte(fin, Role.EMPLOYE, "Haddad", "Karim", "karim@finconseil.fr", "Analyste", "+33 6 00 00 00 09");
        compte(med, Role.EMPLOYE, "Alaoui", "Nadia", "nadia@medtrade.ma", "Acheteuse", "+212 6 00 00 00 10");

        invitationRepository.save(Invitation.builder()
                .email("nouveau@techsoft.tn").poste("Developpeur")
                .token(UUID.randomUUID().toString())
                .entreprise(techsoft)
                .dateExpiration(LocalDateTime.now().plusDays(7))
                .build());
        log.info("Donnees de demonstration du user-service chargees (mot de passe : {})", MDP_DEMO);
    }

    private Entreprise entreprise(String nom, DomaineActivite domaine, String pays, String ville, String desc) {
        return entrepriseRepository.save(Entreprise.builder()
                .nom(nom).domaineActivite(domaine).pays(pays).ville(ville).description(desc)
                .siteWeb("https://www." + nom.toLowerCase() + ".com")
                .build());
    }

    private void compte(Entreprise e, Role role, String nom, String prenom, String email, String poste, String tel) {
        userRepository.save(User.builder()
                .nom(nom).prenom(prenom).email(email).poste(poste).telephone(tel)
                .password(passwordEncoder.encode(MDP_DEMO))
                .role(role).entreprise(e)
                .build());
    }
}
