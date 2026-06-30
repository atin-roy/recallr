package com.atinroy.recallr.domain.mcq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MCQCreateRequest(
        @NotNull @NotBlank String question,
        @NotEmpty List<String> options,
        @PositiveOrZero int correctOptionIndex,
        String explanation,
        UUID topicId
) implements Serializable {}
