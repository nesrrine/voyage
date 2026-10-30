package com.voyagesaffaires.ai.service;

import com.anthropic.models.messages.OutputConfig;
import com.voyagesaffaires.ai.client.ForumClient;
import com.voyagesaffaires.ai.client.VoyageClient;
import com.voyagesaffaires.ai.dto.AiDtos.*;
import com.voyagesaffaires.common.dto.ForumDtos.PostDto;
import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Les 5 fonctionnalites IA de la plateforme :
 * assistant conversationnel, explication des matchs, programme de voyage,
 * forum intelligent (resume + moderation) et recommandations.
 * Le contexte (voyages, invitations, publications) est demande aux autres microservices via OpenFeign,
 * proteges par un circuit breaker (voir VoyageClientFallback / ForumClientFallback).
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private static final String SEPARATEUR = "---MESSAGE---";

    private final ClaudeService claude;
    private final VoyageClient voyageClient;
    private final ForumClient forumClient;

    public StatutIa statut() {
        return new StatutIa(claude.estConfigure());
    }

    // ---------- 1. Assistant IA de voyage ----------

    public TexteResponse chat(ChatRequest req) {
        AuthUser u = SecurityUtils.currentUser();
        String consignes = """
                Tu aides l'utilisateur connecte a preparer ses voyages d'affaires, a trouver des partenaires \
                et a utiliser la plateforme (planning des voyages, programmes, invitations de voyage, forum).
                Tu peux donner des conseils pratiques sur les destinations (quartiers d'affaires, transports, \
                usages professionnels locaux). N'invente jamais de voyages ni de personnes absents du contexte.
                Pour agir (creer un voyage, accepter une invitation), indique a l'utilisateur la page a utiliser.

                CONTEXTE DE L'UTILISATEUR
                """ + contexteUtilisateur(u) + "\n\nVOYAGES PUBLICS A VENIR SUR LA PLATEFORME\n" + programmesAVenir(30);
        return new TexteResponse(claude.converser(consignes, req.messages(), OutputConfig.Effort.LOW));
    }

    // ---------- 2. Explication d'un match + message de premier contact ----------

    public ExplicationMatch expliquerMatch(Long matchId) {
        AuthUser u = SecurityUtils.currentUser();
        // Le voyage-service verifie que l'utilisateur participe a ce voyage commun (sinon 403 / 404)
        MatchDto m = voyageClient.match(matchId);
        String demande = """
                Deux voyages d'affaires ont ete rapproches par notre algorithme de matching.

                MON VOYAGE : %s
                VOYAGE DU PARTENAIRE : %s

                Scores (sur 100) : global %.0f, domaine d'activite %.0f, dates %.0f (%d jours communs), \
                proximite %.0f (%.1f km), meme objet %.0f.

                Redige deux parties separees par une ligne contenant exactement %s :
                1. En 4 a 6 phrases, explique pourquoi cette rencontre est interessante pour les deux entreprises \
                (complementarite des domaines, opportunites de partenariat concretes, moments propices pendant le sejour).
                2. Un court message de premier contact (5 a 7 lignes), cordial et professionnel, que je pourrai \
                envoyer au partenaire. Signe avec mon prenom : %s.
                """.formatted(decrire(m.monVoyage()), decrire(m.voyagePartenaire()), m.score(), m.scoreDomaine(),
                m.scoreDate(), m.joursCommuns(), m.scoreDistance(), m.distanceKm(), m.scoreObjet(), SEPARATEUR,
                u.role() == Role.EMPLOYE ? u.prenom() : premierMot(m.monVoyage().employeNom()));
        String texte = claude.generer("Tu es un conseiller en developpement commercial.", demande, OutputConfig.Effort.LOW);
        int i = texte.indexOf(SEPARATEUR);
        return i < 0
                ? new ExplicationMatch(texte, "")
                : new ExplicationMatch(texte.substring(0, i).trim(), texte.substring(i + SEPARATEUR.length()).trim());
    }

    // ---------- 3. Programme de voyage genere ----------

    public TexteResponse genererProgramme(ProgrammeRequest req) {
        if (req.dateFin().isBefore(req.dateDebut())) {
            throw ApiException.badRequest("La date de fin doit etre posterieure a la date de debut");
        }
        String domaines = req.domainesRecherches() == null || req.domainesRecherches().isEmpty()
                ? "non precises"
                : req.domainesRecherches().stream().map(Enum::name).collect(Collectors.joining(", "));
        String demande = """
                Propose un programme jour par jour pour ce voyage d'affaires :
                - Titre : %s
                - Destination : %s, %s
                - Du %s au %s (%d jours)
                - Objet : %s
                - Entreprises partenaires recherchees (domaines) : %s

                Pour chaque jour : une ligne "Jour N (date) :" puis 2 a 4 tirets (rendez-vous, temps de networking, \
                deplacements). Termine par 3 conseils pratiques propres a la destination. 250 mots maximum.
                """.formatted(req.titre(), req.villeDestination(), req.paysDestination() == null ? "" : req.paysDestination(),
                req.dateDebut(), req.dateFin(), ChronoUnit.DAYS.between(req.dateDebut(), req.dateFin()) + 1,
                req.objet(), domaines);
        return new TexteResponse(claude.generer("Tu es un organisateur de voyages d'affaires.", demande, OutputConfig.Effort.LOW));
    }

    // ---------- 4. Forum intelligent ----------

    public TexteResponse resumerPost(Long postId) {
        PostDto p = forumClient.post(postId);
        String commentaires = p.commentaires() == null || p.commentaires().isEmpty()
                ? "(aucun commentaire)"
                : p.commentaires().stream()
                        .map(c -> "- " + c.auteurNom() + " : " + c.contenu())
                        .collect(Collectors.joining("\n"));
        String demande = """
                Resume cette discussion du forum en 3 a 5 tirets : la question ou l'idee de depart, \
                les reponses et conseils importants, et les eventuelles opportunites d'affaires.

                SUJET : %s (par %s)
                %s

                COMMENTAIRES :
                %s
                """.formatted(p.titre(), p.auteurNom(), p.contenu(), commentaires);
        return new TexteResponse(claude.generer("Tu resumes des discussions professionnelles.", demande, OutputConfig.Effort.LOW));
    }

    public ModerationResponse moderer(ModerationRequest req) {
        String demande = """
                Verifie si cette publication peut etre publiee sur un forum professionnel.
                Refuse uniquement : insultes, harcelement, propos discriminatoires, contenu sexuel, spam publicitaire \
                sans rapport avec les voyages d'affaires, arnaques, donnees personnelles sensibles d'autrui.
                Reponds sur une seule ligne : "ACCEPTABLE" ou "REFUSE: <raison courte destinee a l'auteur>".

                TITRE : %s
                MESSAGE : %s
                """.formatted(req.titre(), req.contenu());
        String verdict = claude.generer("Tu es moderateur de forum.", demande, OutputConfig.Effort.LOW).trim();
        if (verdict.toUpperCase().startsWith("REFUSE")) {
            String raison = verdict.contains(":") ? verdict.substring(verdict.indexOf(':') + 1).trim() : "Contenu inapproprie";
            return new ModerationResponse(false, raison);
        }
        return new ModerationResponse(true, null);
    }

    // ---------- 5. Recommandations ----------

    public TexteResponse recommandations() {
        AuthUser u = SecurityUtils.currentUser();
        String cible = switch (u.role()) {
            case EMPLOYE -> "pour cet employe : destinations et evenements a envisager, types d'entreprises a rencontrer, " +
                    "et comment ameliorer ses chances de matching (domaines recherches, perimetre, dates).";
            case ENTREPRISE -> "pour cette entreprise : destinations strategiques, domaines d'activite partenaires a cibler, " +
                    "employes a mobiliser, et actions pour developper son reseau sur la plateforme.";
            case ADMIN -> "pour l'administrateur de la plateforme : tendances observees et actions pour augmenter " +
                    "le nombre de mises en relation.";
        };
        String demande = """
                A partir des donnees ci-dessous, donne 4 a 6 recommandations concretes et justifiees %s
                Chaque recommandation : un tiret, une phrase d'action, puis la justification tiree des donnees.

                CONTEXTE
                %s

                VOYAGES PUBLICS A VENIR SUR LA PLATEFORME
                %s
                """.formatted(cible, contexteUtilisateur(u), programmesAVenir(50));
        return new TexteResponse(claude.generer("Tu es un analyste en developpement commercial.", demande, OutputConfig.Effort.MEDIUM));
    }

    // ---------------------------------------------------------------------

    private String contexteUtilisateur(AuthUser u) {
        StringBuilder sb = new StringBuilder();
        sb.append("Utilisateur : ").append(u.nomComplet()).append(" (role ").append(u.role()).append(")");
        if (u.poste() != null) {
            sb.append(", poste : ").append(u.poste());
        }
        if (u.entrepriseNom() != null) {
            sb.append("\nEntreprise : ").append(u.entrepriseNom()).append(", domaine ").append(u.domaine());
        }
        List<VoyageDto> voyages = u.role() == Role.ADMIN ? List.of() : voyageClient.planning();
        sb.append("\nSes voyages (").append(voyages.size()).append(") :\n");
        voyages.stream().limit(20).forEach(v -> sb.append("- ").append(decrire(v)).append('\n'));
        if (u.role() == Role.EMPLOYE) {
            List<MatchDto> recues = voyageClient.invitationsRecues();
            long enAttente = recues.stream().filter(m -> m.statut() == StatutMatch.EN_ATTENTE).count();
            sb.append("Invitations de voyage recues : ").append(recues.size()).append(" dont ").append(enAttente).append(" en attente\n");
        }
        return sb.toString();
    }

    private String programmesAVenir(int limite) {
        String liste = voyageClient.programmes().stream()
                .limit(limite)
                .map(v -> "- " + decrire(v))
                .collect(Collectors.joining("\n"));
        return liste.isEmpty() ? "(aucun)" : liste;
    }


    /** Description d'un voyage pour le modele (sans coordonnees personnelles). */
    static String decrire(VoyageDto v) {
        return "\"%s\" a %s (%s) du %s au %s, objet %s, entreprise %s (%s), poste %s, domaines recherches %s"
                .formatted(v.titre(), v.villeDestination(), v.paysDestination(), v.dateDebut(), v.dateFin(),
                        v.objet(), v.entrepriseNom(), v.entrepriseDomaine(), v.employePoste(),
                        v.domainesRecherches() == null || v.domainesRecherches().isEmpty() ? "tous" : v.domainesRecherches());
    }

    private static String premierMot(String s) {
        return s == null ? "" : s.split(" ")[0];
    }
}
