package com.voyagesaffaires.forum.repository;

import com.voyagesaffaires.forum.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);
}
