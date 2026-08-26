package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortAnswerFlashcardResponse extends FlashcardResponse {
    private String modelAnswer;
}
