package com.atinroy.recallr.domain.deck;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "decks")
@Getter
@Setter
public class Deck extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = true)
    @JoinColumn(name = "notebook_id")
    private Notebook notebook;

    @Column(length = 200, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
}
