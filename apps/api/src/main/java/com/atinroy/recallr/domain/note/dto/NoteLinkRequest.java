package com.atinroy.recallr.domain.note.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteLinkRequest(
        @NotBlank String sourceId,
        @NotBlank String targetId
) {
}
