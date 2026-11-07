package com.atinroy.recallr.domain.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortAnswerFlashcardRequest extends FlashcardRequest {
    @NotBlank
    private String modelAnswer;
}
