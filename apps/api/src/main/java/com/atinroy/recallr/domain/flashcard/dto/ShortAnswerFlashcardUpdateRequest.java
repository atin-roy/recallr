package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortAnswerFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private String modelAnswer;
}
