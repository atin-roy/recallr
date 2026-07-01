package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("TRUE_FALSE")
@Getter
@Setter
public class TrueFalseFlashcard extends Flashcard {

    @Column(name = "correct_answer")
    private boolean correctAnswer;

    @Column(length = 1000)
    private String explanation;
}
