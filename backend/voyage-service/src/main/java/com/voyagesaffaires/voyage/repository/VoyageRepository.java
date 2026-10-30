package com.voyagesaffaires.voyage.repository;

import com.voyagesaffaires.voyage.entity.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VoyageRepository extends JpaRepository<Voyage, Long> {

    List<Voyage> findByEmployeIdOrderByDateDebutDesc(Long employeId);

    List<Voyage> findByEntrepriseIdOrderByDateDebutDesc(Long entrepriseId);

    long countByEntrepriseId(Long entrepriseId);

    /** Pre-selection des candidats au matching : dates qui se chevauchent, autre entreprise, voyage visible. */
    @Query("""
            select v from Voyage v
            where v.id <> :voyageId
              and v.visible = true
              and v.employeActif = true
              and v.entrepriseId <> :entrepriseId
              and v.dateDebut <= :fin
              and v.dateFin >= :debut
            """)
    List<Voyage> findCandidats(@Param("voyageId") Long voyageId,
                               @Param("entrepriseId") Long entrepriseId,
                               @Param("debut") LocalDate debut,
                               @Param("fin") LocalDate fin);

    /** Programmes de voyages publics a venir (ou en cours). */
    @Query("select v from Voyage v where v.visible = true and v.employeActif = true and v.dateFin >= :today order by v.dateDebut asc")
    List<Voyage> findProgrammes(@Param("today") LocalDate today);

    /** Activation / desactivation d'un employe (appel du user-service). eid = null : toutes entreprises (admin). */
    @Modifying
    @Query("update Voyage v set v.employeActif = :actif where v.employeId = :employeId and (:eid is null or v.entrepriseId = :eid)")
    int majEmployeActif(@Param("employeId") Long employeId, @Param("actif") boolean actif, @Param("eid") Long entrepriseId);

    @Query("select v.villeDestination, count(v) from Voyage v group by v.villeDestination order by count(v) desc")
    List<Object[]> topDestinations();

    @Query("select v.villeDestination, count(v) from Voyage v where v.entrepriseId = :eid group by v.villeDestination order by count(v) desc")
    List<Object[]> topDestinationsEntreprise(@Param("eid") Long entrepriseId);

    @Query("select v.objet, count(v) from Voyage v group by v.objet")
    List<Object[]> countByObjet();

    @Query("select v.objet, count(v) from Voyage v where v.entrepriseId = :eid group by v.objet")
    List<Object[]> countByObjetEntreprise(@Param("eid") Long entrepriseId);
}
