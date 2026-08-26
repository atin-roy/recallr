package com.atinroy.recallr.domain.notebook.dto;

import java.time.Instant;
import java.util.UUID;

public record NotebookResponse(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        Instant updatedAt
) {}
