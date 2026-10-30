package com.voyagesaffaires.user.repository;

import com.voyagesaffaires.user.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    boolean existsByNomIgnoreCase(String nom);

    @Query("select e.domaineActivite, count(e) from Entreprise e group by e.domaineActivite order by count(e) desc")
    List<Object[]> countByDomaine();
}
