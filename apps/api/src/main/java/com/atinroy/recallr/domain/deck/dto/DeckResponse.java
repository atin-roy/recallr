package com.atinroy.recallr.domain.deck.dto;

import java.time.Instant;
import java.util.UUID;

public record DeckResponse(
        UUID id,
        UUID notebookId,
        String name,
        String description,
        long cardCount,
        Instant createdAt,
        Instant updatedAt
) {}
