package com.voyagesaffaires.user.repository;

import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.user.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByEntrepriseIdOrderByDateCreationDesc(Long entrepriseId);

    boolean existsByEmailIgnoreCaseAndEntrepriseIdAndStatut(String email, Long entrepriseId, StatutInvitation statut);

    long countByEntrepriseIdAndStatut(Long entrepriseId, StatutInvitation statut);
}
