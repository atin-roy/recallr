package com.atinroy.recallr.mcq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.util.List;

public record MCQCreateRequest(
        @NotNull @NotEmpty @NotBlank String question,
        @NotEmpty @NotBlank List<String> options,
        @PositiveOrZero int correctOptionIndex,
        String explanation
) implements Serializable {
  }