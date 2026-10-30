package com.voyagesaffaires.forum.repository;

import com.voyagesaffaires.forum.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {
}
