package com.voyagesaffaires.forum.service;

import com.voyagesaffaires.common.dto.ForumDtos.PostDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.common.enums.TypeReaction;
import com.voyagesaffaires.common.exception.ApiException;
import com.voyagesaffaires.common.security.AuthUser;
import com.voyagesaffaires.forum.dto.ForumRequests.PostRequest;
import com.voyagesaffaires.forum.entity.Post;
import com.voyagesaffaires.forum.entity.Reaction;
import com.voyagesaffaires.forum.repository.CommentaireRepository;
import com.voyagesaffaires.forum.repository.PostRepository;
import com.voyagesaffaires.forum.repository.ReactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Module Forum : publications et Like / Dislike. */
@ExtendWith(MockitoExtension.class)
class ForumServiceTest {

    @Mock PostRepository postRepository;
    @Mock CommentaireRepository commentaireRepository;
    @Mock ReactionRepository reactionRepository;
    @InjectMocks ForumService forumService;

    private final AuthUser amine = new AuthUser(6L, "amine@techsoft.tn", Role.EMPLOYE, "Amine Ben Ali", "Amine",
            1L, "TechSoft", DomaineActivite.INFORMATIQUE, "Chef de projet", null);
    private final AuthUser admin = new AuthUser(1L, "admin@test.com", Role.ADMIN, "Admin", "Admin",
            null, null, null, null, null);
    private Post post;

    @BeforeEach
    void setUp() {
        post = Post.builder().id(3L).titre("Conseils Paris").contenu("Bonjour").categorie("General")
                .auteurId(7L).auteurNom("Sarra Trabelsi").auteurEntreprise("TechSoft").build();
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(AuthUser u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.authorities()));
    }

    private void reactionExistante(TypeReaction type) {
        Reaction r = Reaction.builder().id(9L).post(post).userId(6L).type(type).build();
        post.getReactions().add(r);
        when(reactionRepository.findByPostIdAndUserId(3L, 6L)).thenReturn(Optional.of(r));
    }

    @Test
    void premierLikeAjouteUneReaction() {
        connecter(amine);
        when(postRepository.findById(3L)).thenReturn(Optional.of(post));
        when(reactionRepository.findByPostIdAndUserId(3L, 6L)).thenReturn(Optional.empty());
        when(reactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PostDto dto = forumService.reagir(3L, TypeReaction.LIKE);

        assertThat(dto.likes()).isEqualTo(1);
        assertThat(dto.dislikes()).isZero();
        assertThat(dto.maReaction()).isEqualTo(TypeReaction.LIKE);
    }

    @Test
    void recliquerSurLikeAnnuleLaReaction() {
        connecter(amine);
        when(postRepository.findById(3L)).thenReturn(Optional.of(post));
        reactionExistante(TypeReaction.LIKE);

        PostDto dto = forumService.reagir(3L, TypeReaction.LIKE);

        assertThat(dto.likes()).isZero();
        assertThat(dto.maReaction()).isNull();
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void passerDeLikeADislike() {
        connecter(amine);
        when(postRepository.findById(3L)).thenReturn(Optional.of(post));
        reactionExistante(TypeReaction.LIKE);

        PostDto dto = forumService.reagir(3L, TypeReaction.DISLIKE);

        assertThat(dto.likes()).isZero();
        assertThat(dto.dislikes()).isEqualTo(1);
        assertThat(dto.maReaction()).isEqualTo(TypeReaction.DISLIKE);
    }

    @Test
    void lAuteurEstReprisDuJetonEtLaCategorieInconnueDevientGeneral() {
        connecter(amine);
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PostDto dto = forumService.creer(new PostRequest("Titre", "Contenu", "Inexistante"));

        assertThat(dto.categorie()).isEqualTo("General");
        assertThat(dto.auteurId()).isEqualTo(6L);
        assertThat(dto.auteurNom()).isEqualTo("Amine Ben Ali");
        assertThat(dto.auteurEntreprise()).isEqualTo("TechSoft");
    }

    @Test
    void seulLAuteurPeutSupprimer() {
        connecter(amine);
        when(postRepository.findById(3L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> forumService.supprimer(3L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(postRepository, never()).delete(any());
    }

    @Test
    void lAdministrateurPeutSupprimerToutePublication() {
        connecter(admin);
        when(postRepository.findById(3L)).thenReturn(Optional.of(post));

        forumService.supprimer(3L);

        verify(postRepository).delete(post);
    }

    @Test
    void triParPopularite() {
        Post populaire = Post.builder().id(4L).titre("Populaire").contenu("x").categorie("General").auteurId(7L)
                .dateCreation(LocalDateTime.now().minusDays(3)).build();
        populaire.getReactions().add(Reaction.builder().post(populaire).userId(8L).type(TypeReaction.LIKE).build());
        connecter(amine);
        when(postRepository.findAllByOrderByDateCreationDesc()).thenReturn(List.of(post, populaire));

        List<PostDto> res = forumService.lister("", null, "populaire");

        assertThat(res).extracting(PostDto::id).containsExactly(4L, 3L);
    }

    @Test
    void statistiquesParCategorie() {
        when(postRepository.countByCategorie()).thenReturn(List.<Object[]>of(new Object[]{"General", 2L}));
        when(postRepository.count()).thenReturn(2L);

        var stats = forumService.statistiques();

        assertThat(stats.publications()).isEqualTo(2);
        assertThat(stats.parCategorie().labels()).containsExactly("General");
    }
}
