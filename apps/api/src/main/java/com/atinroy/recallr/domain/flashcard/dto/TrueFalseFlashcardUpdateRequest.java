package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrueFalseFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private boolean correctAnswer;
    private String explanation;
}
