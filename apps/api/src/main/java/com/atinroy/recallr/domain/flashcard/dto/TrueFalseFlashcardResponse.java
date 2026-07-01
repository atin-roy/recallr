package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrueFalseFlashcardResponse extends FlashcardResponse {
    private boolean correctAnswer;
    private String explanation;
}
