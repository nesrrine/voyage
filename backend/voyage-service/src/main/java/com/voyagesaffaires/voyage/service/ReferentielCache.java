package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.voyage.config.CacheConfig;
import com.voyagesaffaires.voyage.dto.VoyageDtos.VilleDto;
import com.voyagesaffaires.voyage.repository.VilleRepository;
import com.voyagesaffaires.voyage.repository.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Lectures frequentes mises en cache (bean separe pour que le proxy Spring intercepte les appels).
 * L'invalidation des programmes est faite par VoyageService (@CacheEvict) a chaque ecriture.
 */
@Component
@RequiredArgsConstructor
public class ReferentielCache {

    private final VilleRepository villeRepository;
    private final VoyageRepository voyageRepository;

    @Cacheable(CacheConfig.VILLES)
    @Transactional(readOnly = true)
    public List<VilleDto> villes() {
        return villeRepository.findAllByOrderByPaysAscNomAsc().stream().map(VilleDto::from).toList();
    }

    /** Voyages publics a venir ; la cle est le jour, pour que le statut (a venir / en cours) reste juste. */
    @Cacheable(cacheNames = CacheConfig.PROGRAMMES, key = "#jour")
    @Transactional(readOnly = true)
    public List<VoyageDto> programmesAVenir(LocalDate jour) {
        return voyageRepository.findProgrammes(jour).stream().map(VoyageMapper::toDto).toList();
    }
}
