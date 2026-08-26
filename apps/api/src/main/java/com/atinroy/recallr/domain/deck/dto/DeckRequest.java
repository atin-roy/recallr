package com.atinroy.recallr.domain.deck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeckRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
