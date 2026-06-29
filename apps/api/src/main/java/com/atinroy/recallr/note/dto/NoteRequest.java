package com.atinroy.recallr.note.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record NoteRequest(
        @NotBlank String title,
        String content,
        UUID topicId
) {}
