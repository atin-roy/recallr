package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("SHORT_ANSWER")
@Getter
@Setter
public class ShortAnswerFlashcard extends Flashcard {

    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;
}
