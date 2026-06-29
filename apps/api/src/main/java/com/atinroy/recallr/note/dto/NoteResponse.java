package com.atinroy.recallr.note.dto;

public record NoteResponse(
        String id,
        String title,
        String content,
        String subjectId,
        String topicId
) {}
