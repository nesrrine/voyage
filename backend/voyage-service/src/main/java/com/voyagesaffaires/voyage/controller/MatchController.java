package com.voyagesaffaires.voyage.controller;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.voyage.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Invitations de voyage generees par le matching. */
@RestController
@RequestMapping("/api/matchs")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/recues")
    @PreAuthorize("hasRole('EMPLOYE')")
    public List<MatchDto> recues() {
        return matchService.recues();
    }

    @GetMapping("/envoyees")
    @PreAuthorize("hasRole('EMPLOYE')")
    public List<MatchDto> envoyees() {
        return matchService.envoyees();
    }

    /** Detail d'une invitation (utilise par l'ai-service pour l'analyse de la rencontre). */
    @GetMapping("/{id}")
    public MatchDto consulter(@PathVariable Long id) {
        return matchService.consulter(id);
    }

    @PostMapping("/{id}/accepter")
    @PreAuthorize("hasRole('EMPLOYE')")
    public MatchDto accepter(@PathVariable Long id) {
        return matchService.accepter(id);
    }

    @PostMapping("/{id}/refuser")
    @PreAuthorize("hasRole('EMPLOYE')")
    public MatchDto refuser(@PathVariable Long id) {
        return matchService.refuser(id);
    }
}
