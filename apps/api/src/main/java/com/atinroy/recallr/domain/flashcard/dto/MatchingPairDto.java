package com.atinroy.recallr.domain.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingPairDto {
    @NotBlank
    private String prompt;
    @NotBlank
    private String matchValue;
}
