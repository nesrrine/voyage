package com.voyagesaffaires.forum.repository;

import com.voyagesaffaires.forum.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByDateCreationDesc();

    @Query("select p.categorie, count(p) from Post p group by p.categorie order by count(p) desc")
    List<Object[]> countByCategorie();
}
