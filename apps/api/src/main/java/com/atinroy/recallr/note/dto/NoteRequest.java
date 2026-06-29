package com.atinroy.recallr.note.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteRequest(
        @NotBlank String title,
        String content
) {
}
