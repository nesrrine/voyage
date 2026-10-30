package com.voyagesaffaires.ai.controller;

import com.voyagesaffaires.ai.dto.AiDtos.*;
import com.voyagesaffaires.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Fonctionnalites d'intelligence artificielle (utilisateurs connectes). */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/statut")
    public StatutIa statut() {
        return aiService.statut();
    }

    @PostMapping("/chat")
    public TexteResponse chat(@Valid @RequestBody ChatRequest req) {
        return aiService.chat(req);
    }

    @GetMapping("/matchs/{id}/explication")
    public ExplicationMatch expliquerMatch(@PathVariable Long id) {
        return aiService.expliquerMatch(id);
    }

    @PostMapping("/programme")
    public TexteResponse programme(@Valid @RequestBody ProgrammeRequest req) {
        return aiService.genererProgramme(req);
    }

    @GetMapping("/forum/posts/{id}/resume")
    public TexteResponse resumer(@PathVariable Long id) {
        return aiService.resumerPost(id);
    }

    @PostMapping("/forum/moderation")
    public ModerationResponse moderer(@Valid @RequestBody ModerationRequest req) {
        return aiService.moderer(req);
    }

    @GetMapping("/recommandations")
    public TexteResponse recommandations() {
        return aiService.recommandations();
    }
}
