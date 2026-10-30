package com.voyagesaffaires.forum.config;

import com.voyagesaffaires.common.enums.TypeReaction;
import com.voyagesaffaires.forum.entity.Commentaire;
import com.voyagesaffaires.forum.entity.Post;
import com.voyagesaffaires.forum.entity.Reaction;
import com.voyagesaffaires.forum.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Publications de demonstration (auteurs = employes du jeu de demonstration du user-service). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final long AMINE = 6, SARRA = 7, YOUSSEF = 8, CLAIRE = 9, KARIM = 10, NADIA = 11;

    private final PostRepository postRepository;

    @Value("${app.demo-data:true}")
    private boolean demoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoData || postRepository.count() > 0) {
            return;
        }
        Post p1 = post(AMINE, "Amine Ben Ali", "TechSoft", "Retour d'experience : salon a Paris", "Retour d'experience",
                "J'ai rencontre via la plateforme un ingenieur de GreenPower pendant le salon. "
                        + "Nous travaillons maintenant sur un projet de supervision de centrales solaires !");
        Post p2 = post(CLAIRE, "Claire Martin", "FinConseil", "Quels quartiers d'affaires a Dubai ?", "Conseils voyage",
                "Je pars a Dubai le mois prochain : des conseils sur les quartiers pour les rendez-vous d'affaires ?");
        Post p3 = post(NADIA, "Nadia Alaoui", "MedTrade", "Recherche partenaire logistique au Maghreb", "Opportunites d'affaires",
                "MedTrade cherche un partenaire logistique entre Casablanca et Tunis. Contactez-moi !");
        commentaire(p2, KARIM, "Karim Haddad", "FinConseil", "DIFC et Business Bay sont incontournables.");
        commentaire(p2, SARRA, "Sarra Trabelsi", "TechSoft", "Je serai aussi au GITEX, on pourra se croiser !");
        commentaire(p3, YOUSSEF, "Youssef Gharbi", "GreenPower", "Interessant, je serai a Casablanca dans deux mois.");
        reaction(p1, YOUSSEF, TypeReaction.LIKE);
        reaction(p1, CLAIRE, TypeReaction.LIKE);
        reaction(p1, NADIA, TypeReaction.LIKE);
        reaction(p2, KARIM, TypeReaction.LIKE);
        reaction(p3, AMINE, TypeReaction.DISLIKE);
        reaction(p3, YOUSSEF, TypeReaction.LIKE);
        log.info("Publications de demonstration du forum chargees");
    }

    private Post post(long auteurId, String auteur, String entreprise, String titre, String categorie, String contenu) {
        return postRepository.save(Post.builder().auteurId(auteurId).auteurNom(auteur).auteurEntreprise(entreprise)
                .titre(titre).categorie(categorie).contenu(contenu).build());
    }

    private void commentaire(Post p, long auteurId, String auteur, String entreprise, String contenu) {
        p.getCommentaires().add(Commentaire.builder().post(p).auteurId(auteurId).auteurNom(auteur)
                .auteurEntreprise(entreprise).contenu(contenu).build());
    }

    private void reaction(Post p, long userId, TypeReaction type) {
        p.getReactions().add(Reaction.builder().post(p).userId(userId).type(type).build());
    }
}
