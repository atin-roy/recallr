package com.atinroy.recallr.domain.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClozeFlashcardUpdateRequest extends FlashcardUpdateRequest {
    @NotEmpty
    private List<@NotBlank String> answers;
}
