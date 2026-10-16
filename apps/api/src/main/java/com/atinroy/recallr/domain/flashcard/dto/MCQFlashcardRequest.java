package com.atinroy.recallr.domain.flashcard.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MCQFlashcardRequest extends FlashcardRequest {
    @NotNull
    @Size(min = 2)
    private List<@NotBlank String> options;
    @Min(0)
    private int correctOptionIndex;
    private String explanation;

    @AssertTrue(message = "correctOptionIndex must be less than the number of options")
    public boolean isCorrectOptionIndexInRange() {
        return options == null || correctOptionIndex < options.size();
    }
}
