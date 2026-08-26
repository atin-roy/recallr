package com.atinroy.recallr.domain.note.dto;

public record NoteUpdateRequest(
        String title,
        String content
) {}
