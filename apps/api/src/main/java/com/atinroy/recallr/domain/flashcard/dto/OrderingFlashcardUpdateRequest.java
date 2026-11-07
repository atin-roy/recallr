package com.atinroy.recallr.domain.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderingFlashcardUpdateRequest extends FlashcardUpdateRequest {
    @NotNull
    @Size(min = 2)
    private List<@NotBlank String> items;
}
