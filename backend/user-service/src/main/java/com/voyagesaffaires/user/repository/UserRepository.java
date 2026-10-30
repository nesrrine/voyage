package com.voyagesaffaires.user.repository;

import com.voyagesaffaires.common.enums.Role;
import com.voyagesaffaires.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByEntrepriseIdAndRoleOrderByNomAsc(Long entrepriseId, Role role);

    List<User> findAllByOrderByDateCreationDesc();

    long countByRole(Role role);

    long countByEntrepriseIdAndRole(Long entrepriseId, Role role);
}
