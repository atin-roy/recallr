package com.atinroy.recallr.domain.flashcard;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "flashcards")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class Flashcard extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column
    private String question;
}
