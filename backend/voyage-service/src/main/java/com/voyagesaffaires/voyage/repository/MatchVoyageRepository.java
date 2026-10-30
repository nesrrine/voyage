package com.voyagesaffaires.voyage.repository;

import com.voyagesaffaires.common.enums.StatutMatch;
import com.voyagesaffaires.voyage.entity.MatchVoyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchVoyageRepository extends JpaRepository<MatchVoyage, Long> {

    /** Invitations de voyage recues par un employe. */
    List<MatchVoyage> findByVoyageCibleEmployeIdOrderByDateCreationDesc(Long employeId);

    /** Invitations de voyage envoyees (automatiquement) au nom d'un employe. */
    List<MatchVoyage> findByVoyageSourceEmployeIdOrderByDateCreationDesc(Long employeId);

    @Query("select m from MatchVoyage m where m.voyageSource.id = :vid or m.voyageCible.id = :vid order by m.score desc")
    List<MatchVoyage> findByVoyage(@Param("vid") Long voyageId);

    @Query("""
            select count(m) > 0 from MatchVoyage m
            where (m.voyageSource.id = :a and m.voyageCible.id = :b)
               or (m.voyageSource.id = :b and m.voyageCible.id = :a)
            """)
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    @Modifying
    @Query("delete from MatchVoyage m where m.voyageSource.id = :vid or m.voyageCible.id = :vid")
    void deleteByVoyage(@Param("vid") Long voyageId);

    @Modifying
    @Query("delete from MatchVoyage m where (m.voyageSource.id = :vid or m.voyageCible.id = :vid) and m.statut = :statut")
    void deleteByVoyageAndStatut(@Param("vid") Long voyageId, @Param("statut") StatutMatch statut);

    long countByStatut(StatutMatch statut);

    @Query("select m.statut, count(m) from MatchVoyage m group by m.statut")
    List<Object[]> countGroupByStatut();

    @Query("""
            select m.statut, count(m) from MatchVoyage m
            where m.voyageSource.entrepriseId = :eid or m.voyageCible.entrepriseId = :eid
            group by m.statut
            """)
    List<Object[]> countGroupByStatutEntreprise(@Param("eid") Long entrepriseId);

    @Query("""
            select m.statut, count(m) from MatchVoyage m
            where m.voyageSource.employeId = :uid or m.voyageCible.employeId = :uid
            group by m.statut
            """)
    List<Object[]> countGroupByStatutEmploye(@Param("uid") Long employeId);

    /** Voyages communs acceptes impliquant une entreprise. */
    @Query("""
            select m from MatchVoyage m
            where m.statut = com.voyagesaffaires.common.enums.StatutMatch.ACCEPTEE
              and (m.voyageSource.entrepriseId = :eid or m.voyageCible.entrepriseId = :eid)
            """)
    List<MatchVoyage> findAcceptesEntreprise(@Param("eid") Long entrepriseId);
}
