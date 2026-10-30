package com.voyagesaffaires.user.controller;

import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.user.dto.AuthDtos.AuthResponse;
import com.voyagesaffaires.user.dto.UserDtos.*;
import com.voyagesaffaires.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public UserDto moi() {
        return accountService.moi();
    }

    /** Renvoie un nouveau jeton contenant les informations a jour. */
    @PutMapping("/me")
    public AuthResponse modifierProfil(@Valid @RequestBody UpdateProfilRequest req) {
        return accountService.modifierProfil(req);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangePasswordRequest req) {
        accountService.changerMotDePasse(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entreprise")
    @PreAuthorize("hasAnyRole('ENTREPRISE','EMPLOYE')")
    public EntrepriseDto monEntreprise() {
        return accountService.monEntreprise();
    }

    @PutMapping("/entreprise")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public AuthResponse modifierEntreprise(@Valid @RequestBody UpdateEntrepriseRequest req) {
        return accountService.modifierEntreprise(req);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> desactiver() {
        accountService.desactiverMonCompte();
        return ResponseEntity.noContent().build();
    }
}
