package com.voyagesaffaires.user.controller;

import com.voyagesaffaires.user.dto.UserDtos.InvitationDto;
import com.voyagesaffaires.user.dto.UserDtos.InvitationRequest;
import com.voyagesaffaires.user.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Invitations des employes (reserve au role ENTREPRISE, cf. SecurityConfig). */
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping
    public List<InvitationDto> lister() {
        return invitationService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationDto inviter(@Valid @RequestBody InvitationRequest req) {
        return invitationService.inviter(req);
    }

    @PostMapping("/{id}/renouveler")
    public InvitationDto renouveler(@PathVariable Long id) {
        return invitationService.renouveler(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annuler(@PathVariable Long id) {
        invitationService.annuler(id);
        return ResponseEntity.noContent().build();
    }
}
