package com.atinroy.recallr.note.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteUpdateRequest(
        @NotBlank String id,
        String title,
        String content
) {
}
