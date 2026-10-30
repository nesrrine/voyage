package com.voyagesaffaires.voyage.repository;

import com.voyagesaffaires.voyage.entity.Ville;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VilleRepository extends JpaRepository<Ville, Long> {

    List<Ville> findAllByOrderByPaysAscNomAsc();
}
