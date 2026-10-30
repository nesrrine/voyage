package com.voyagesaffaires.voyage.controller;

import com.voyagesaffaires.common.dto.StatsDtos.DashboardDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VilleDto;
import com.voyagesaffaires.voyage.repository.VilleRepository;
import com.voyagesaffaires.voyage.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Referentiels (publics) et tableau de bord statistique (authentifie). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetaController {

    private final VilleRepository villeRepository;
    private final StatsService statsService;

    @GetMapping("/meta/villes")
    public List<VilleDto> villes() {
        return villeRepository.findAllByOrderByPaysAscNomAsc().stream().map(VilleDto::from).toList();
    }

    @GetMapping("/meta/domaines")
    public DomaineActivite[] domaines() {
        return DomaineActivite.values();
    }

    @GetMapping("/meta/objets")
    public ObjetVoyage[] objets() {
        return ObjetVoyage.values();
    }

    @GetMapping("/stats/dashboard")
    public DashboardDto dashboard() {
        return statsService.dashboard();
    }
}
