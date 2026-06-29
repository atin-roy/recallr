package com.atinroy.recallr.note.dto;

public record NoteUpdateRequest(
        String title,
        String content
) {
}
