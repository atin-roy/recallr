package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrueFalseFlashcardRequest extends FlashcardRequest {
    private boolean correctAnswer;
    private String explanation;
}
