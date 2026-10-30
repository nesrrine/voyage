package com.voyagesaffaires.forum.entity;

import com.voyagesaffaires.common.enums.TypeReaction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Like / Dislike d'un utilisateur sur un post (une seule reaction par utilisateur et par post). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeReaction type;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
