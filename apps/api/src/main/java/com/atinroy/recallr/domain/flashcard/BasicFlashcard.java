package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("BASIC")
@Getter
@Setter
public class BasicFlashcard extends Flashcard {

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column
    private boolean reverse;
}
