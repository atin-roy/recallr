package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasicFlashcardRequest extends FlashcardRequest {
    private String answer;
    private boolean reverse;
}
