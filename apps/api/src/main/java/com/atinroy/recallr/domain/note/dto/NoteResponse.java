package com.atinroy.recallr.domain.note.dto;

public record NoteResponse(
        String id,
        String title,
        String content,
        String notebookId
) {}
