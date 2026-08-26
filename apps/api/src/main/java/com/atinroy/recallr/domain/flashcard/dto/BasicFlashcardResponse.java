package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasicFlashcardResponse extends FlashcardResponse {
    private String answer;
    private boolean reverse;
}
