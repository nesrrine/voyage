package com.voyagesaffaires.forum.service;

import com.voyagesaffaires.common.dto.ForumDtos.CommentaireDto;
import com.voyagesaffaires.common.dto.ForumDtos.PostDto;
import com.voyagesaffaires.common.dto.StatsDtos.ForumStats;
import com.voyagesaffaires.common.dto.StatsDtos.Serie;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.TypeReaction;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.common.security.SecurityUtils;
import com.voyagesaffaires.forum.dto.ForumRequests.CommentaireRequest;
import com.voyagesaffaires.forum.dto.ForumRequests.PostRequest;
import com.voyagesaffaires.forum.entity.Commentaire;
import com.voyagesaffaires.forum.entity.Post;
import com.voyagesaffaires.forum.entity.Reaction;
import com.voyagesaffaires.forum.repository.CommentaireRepository;
import com.voyagesaffaires.forum.repository.PostRepository;
import com.voyagesaffaires.forum.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumService {

    public static final List<String> CATEGORIES = List.of(
            "Retour d'experience", "Conseils voyage", "Opportunites d'affaires", "Evenements", "General");

    private final PostRepository postRepository;
    private final CommentaireRepository commentaireRepository;
    private final ReactionRepository reactionRepository;

    /**
     * @param tri "recent" (defaut), "populaire" (likes - dislikes) ou "commentes"
     */
    @Transactional(readOnly = true)
    public List<PostDto> lister(String recherche, String categorie, String tri) {
        AuthUser u = SecurityUtils.currentUser();
        String q = recherche == null ? "" : recherche.trim().toLowerCase();
        Comparator<PostDto> ordre = switch (tri == null ? "recent" : tri) {
            case "populaire" -> Comparator.comparingLong((PostDto p) -> p.likes() - p.dislikes()).reversed();
            case "commentes" -> Comparator.comparingInt(PostDto::nbCommentaires).reversed();
            default -> Comparator.comparing(PostDto::dateCreation).reversed();
        };
        return postRepository.findAllByOrderByDateCreationDesc().stream()
                .filter(p -> categorie == null || categorie.isBlank() || categorie.equals(p.getCategorie()))
                .filter(p -> q.isEmpty() || p.getTitre().toLowerCase().contains(q) || p.getContenu().toLowerCase().contains(q))
                .map(p -> toDto(p, u, false))
                .sorted(ordre)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostDto consulter(Long id) {
        return toDto(getPost(id), SecurityUtils.currentUser(), true);
    }

    @Transactional
    public PostDto creer(PostRequest req) {
        AuthUser u = SecurityUtils.currentUser();
        Post p = postRepository.save(Post.builder()
                .titre(req.titre())
                .contenu(req.contenu())
                .categorie(categorieValide(req.categorie()))
                .auteurId(u.id())
                .auteurNom(u.nomComplet())
                .auteurEntreprise(nomEntreprise(u))
                .build());
        return toDto(p, u, true);
    }

    @Transactional
    public PostDto modifier(Long id, PostRequest req) {
        AuthUser u = SecurityUtils.currentUser();
        Post p = getPost(id);
        if (!p.getAuteurId().equals(u.id())) {
            throw ApiException.forbidden("Vous ne pouvez modifier que vos propres publications");
        }
        p.setTitre(req.titre());
        p.setContenu(req.contenu());
        p.setCategorie(categorieValide(req.categorie()));
        return toDto(p, u, true);
    }

    @Transactional
    public void supprimer(Long id) {
        AuthUser u = SecurityUtils.currentUser();
        Post p = getPost(id);
        if (!p.getAuteurId().equals(u.id()) && u.role() != Role.ADMIN) {
            throw ApiException.forbidden("Vous ne pouvez supprimer que vos propres publications");
        }
        postRepository.delete(p);
    }

    @Transactional
    public PostDto commenter(Long postId, CommentaireRequest req) {
        AuthUser u = SecurityUtils.currentUser();
        Post p = getPost(postId);
        Commentaire c = Commentaire.builder().contenu(req.contenu()).post(p)
                .auteurId(u.id()).auteurNom(u.nomComplet()).auteurEntreprise(nomEntreprise(u)).build();
        p.getCommentaires().add(c);
        commentaireRepository.save(c);
        return toDto(p, u, true);
    }

    @Transactional
    public PostDto supprimerCommentaire(Long commentaireId) {
        AuthUser u = SecurityUtils.currentUser();
        Commentaire c = commentaireRepository.findById(commentaireId)
                .orElseThrow(() -> ApiException.notFound("Commentaire introuvable"));
        if (!c.getAuteurId().equals(u.id()) && u.role() != Role.ADMIN) {
            throw ApiException.forbidden("Vous ne pouvez supprimer que vos propres commentaires");
        }
        Post p = c.getPost();
        p.getCommentaires().remove(c);
        return toDto(p, u, true);
    }

    /**
     * Like / Dislike : cliquer sur la meme reaction l'annule, cliquer sur l'autre la remplace.
     */
    @Transactional
    public PostDto reagir(Long postId, TypeReaction type) {
        AuthUser u = SecurityUtils.currentUser();
        Post p = getPost(postId);
        reactionRepository.findByPostIdAndUserId(postId, u.id()).ifPresentOrElse(r -> {
            if (r.getType() == type) {
                p.getReactions().remove(r);
            } else {
                r.setType(type);
            }
        }, () -> p.getReactions().add(reactionRepository.save(
                Reaction.builder().post(p).userId(u.id()).type(type).build())));
        return toDto(p, u, false);
    }

    /** Indicateurs pour le tableau de bord (appel du voyage-service). */
    @Transactional(readOnly = true)
    public ForumStats statistiques() {
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        for (Object[] row : postRepository.countByCategorie()) {
            labels.add(String.valueOf(row[0]));
            data.add((Number) row[1]);
        }
        return new ForumStats(postRepository.count(), new Serie(labels, data));
    }

    // ---------------------------------------------------------------------

    private String categorieValide(String categorie) {
        return categorie != null && CATEGORIES.contains(categorie) ? categorie : "General";
    }

    private Post getPost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> ApiException.notFound("Publication introuvable"));
    }

    private static String nomEntreprise(AuthUser u) {
        return u.entrepriseNom() == null ? "Administration" : u.entrepriseNom();
    }

    private PostDto toDto(Post p, AuthUser viewer, boolean avecCommentaires) {
        long likes = p.getReactions().stream().filter(r -> r.getType() == TypeReaction.LIKE).count();
        long dislikes = p.getReactions().size() - likes;
        TypeReaction maReaction = p.getReactions().stream()
                .filter(r -> r.getUserId().equals(viewer.id()))
                .map(Reaction::getType)
                .findFirst().orElse(null);
        List<CommentaireDto> commentaires = avecCommentaires
                ? p.getCommentaires().stream().map(c -> new CommentaireDto(c.getId(), c.getContenu(),
                        c.getAuteurId(), c.getAuteurNom(), c.getAuteurEntreprise(), c.getDateCreation())).toList()
                : null;
        return new PostDto(p.getId(), p.getTitre(), p.getContenu(), p.getCategorie(),
                p.getAuteurId(), p.getAuteurNom(), p.getAuteurEntreprise(), p.getDateCreation(),
                likes, dislikes, maReaction, p.getCommentaires().size(), commentaires);
    }
}
