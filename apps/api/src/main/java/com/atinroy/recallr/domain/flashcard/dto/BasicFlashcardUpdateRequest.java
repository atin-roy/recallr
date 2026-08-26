package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasicFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private String answer;
    private boolean reverse;
}
