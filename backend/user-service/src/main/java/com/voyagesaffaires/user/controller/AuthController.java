package com.voyagesaffaires.user.controller;

import com.voyagesaffaires.user.dto.AuthDtos.*;
import com.voyagesaffaires.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register-entreprise")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerEntreprise(@Valid @RequestBody RegisterEntrepriseRequest req) {
        return authService.registerEntreprise(req);
    }

    @GetMapping("/invitation/{token}")
    public InvitationInfo invitation(@PathVariable String token) {
        return authService.invitationInfo(token);
    }

    @PostMapping("/register-employe")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerEmploye(@Valid @RequestBody RegisterEmployeRequest req) {
        return authService.registerEmploye(req);
    }
}
