package com.voyagesaffaires.forum.controller;

import com.voyagesaffaires.common.dto.ForumDtos.PostDto;
import com.voyagesaffaires.common.dto.StatsDtos.ForumStats;
import com.voyagesaffaires.forum.dto.ForumRequests.*;
import com.voyagesaffaires.forum.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/categories")
    public List<String> categories() {
        return ForumService.CATEGORIES;
    }

    @GetMapping("/posts")
    public List<PostDto> lister(@RequestParam(required = false) String q,
                                @RequestParam(required = false) String categorie,
                                @RequestParam(required = false, defaultValue = "recent") String tri) {
        return forumService.lister(q, categorie, tri);
    }

    @GetMapping("/posts/{id}")
    public PostDto consulter(@PathVariable Long id) {
        return forumService.consulter(id);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto creer(@Valid @RequestBody PostRequest req) {
        return forumService.creer(req);
    }

    @PutMapping("/posts/{id}")
    public PostDto modifier(@PathVariable Long id, @Valid @RequestBody PostRequest req) {
        return forumService.modifier(id, req);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        forumService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/commentaires")
    public PostDto commenter(@PathVariable Long id, @Valid @RequestBody CommentaireRequest req) {
        return forumService.commenter(id, req);
    }

    @DeleteMapping("/commentaires/{id}")
    public PostDto supprimerCommentaire(@PathVariable Long id) {
        return forumService.supprimerCommentaire(id);
    }

    /** Like / Dislike (bascule). */
    @PostMapping("/posts/{id}/reaction")
    public PostDto reagir(@PathVariable Long id, @Valid @RequestBody ReactionRequest req) {
        return forumService.reagir(id, req.type());
    }

    /** API inter-services : indicateurs pour le tableau de bord. */
    @GetMapping("/stats")
    public ForumStats statistiques() {
        return forumService.statistiques();
    }
}
