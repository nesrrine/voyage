package com.voyagesaffaires.voyage.controller;

import com.voyagesaffaires.common.dto.MatchDto;
import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VoyageRequest;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VoyageResultat;
import com.voyagesaffaires.voyage.service.VoyageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voyages")
@RequiredArgsConstructor
public class VoyageController {

    private final VoyageService voyageService;

    /** Planning de l'utilisateur connecte. */
    @GetMapping
    public List<VoyageDto> planning() {
        return voyageService.monPlanning();
    }

    /** Consultation des programmes de voyages publics, avec filtres optionnels. */
    @GetMapping("/programmes")
    public List<VoyageDto> programmes(@RequestParam(required = false) String destination,
                                      @RequestParam(required = false) ObjetVoyage objet,
                                      @RequestParam(required = false) DomaineActivite domaine,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return voyageService.programmes(destination, objet, domaine, du, au);
    }

    @GetMapping("/{id}")
    public VoyageDto consulter(@PathVariable Long id) {
        return voyageService.consulter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYE','ENTREPRISE')")
    public VoyageResultat creer(@Valid @RequestBody VoyageRequest req) {
        return voyageService.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE','ENTREPRISE')")
    public VoyageResultat modifier(@PathVariable Long id, @Valid @RequestBody VoyageRequest req) {
        return voyageService.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        voyageService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/matchs")
    public List<MatchDto> matchs(@PathVariable Long id) {
        return voyageService.matchsDuVoyage(id);
    }

    @PostMapping("/{id}/matching")
    @PreAuthorize("hasAnyRole('EMPLOYE','ENTREPRISE')")
    public Map<String, Integer> relancerMatching(@PathVariable Long id) {
        return Map.of("nouveauxMatchs", voyageService.relancerMatching(id));
    }

    /** API inter-services : appelee par le user-service quand un employe est active / desactive. */
    @PutMapping("/employes/{employeId}/actif")
    public Map<String, Integer> majEmployeActif(@PathVariable Long employeId, @RequestParam boolean actif) {
        return Map.of("voyagesModifies", voyageService.majEmployeActif(employeId, actif));
    }
}
