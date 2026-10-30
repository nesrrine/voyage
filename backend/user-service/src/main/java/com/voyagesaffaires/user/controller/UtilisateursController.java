package com.voyagesaffaires.user.controller;

import com.voyagesaffaires.common.dto.StatsDtos.UserStats;
import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.user.dto.UserDtos.EntrepriseDto;
import com.voyagesaffaires.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des employes (ENTREPRISE), administration (ADMIN)
 * et API utilisee par les autres microservices via OpenFeign (/api/users/**).
 */
@RestController
@RequiredArgsConstructor
public class UtilisateursController {

    private final UserAdminService userAdminService;

    // ----- Entreprise (role verifie dans SecurityConfig) -----

    @GetMapping("/api/entreprise/employes")
    public List<UserDto> employes() {
        return userAdminService.mesEmployes();
    }

    @PatchMapping("/api/entreprise/employes/{id}/statut")
    public UserDto basculerStatutEmploye(@PathVariable Long id) {
        return userAdminService.basculerStatutEmploye(id);
    }

    // ----- Administration -----

    @GetMapping("/api/admin/users")
    public List<UserDto> utilisateurs() {
        return userAdminService.tousLesUtilisateurs();
    }

    @PatchMapping("/api/admin/users/{id}/statut")
    public UserDto basculerStatut(@PathVariable Long id) {
        return userAdminService.basculerStatut(id);
    }

    @GetMapping("/api/admin/entreprises")
    public List<EntrepriseDto> entreprises() {
        return userAdminService.toutesLesEntreprises();
    }

    // ----- API inter-services -----

    @GetMapping("/api/users/stats")
    public UserStats statistiques() {
        return userAdminService.statistiques();
    }

    @GetMapping("/api/users/{id}")
    public UserDto utilisateur(@PathVariable Long id) {
        return userAdminService.utilisateur(id);
    }
}
