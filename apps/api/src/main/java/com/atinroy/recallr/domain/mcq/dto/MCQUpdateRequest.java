package com.atinroy.recallr.domain.mcq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record MCQUpdateRequest(
        @NotBlank String question,
        @NotEmpty List<String> options,
        @PositiveOrZero int correctOptionIndex,
        String explanation,
        UUID topicId
) {}
