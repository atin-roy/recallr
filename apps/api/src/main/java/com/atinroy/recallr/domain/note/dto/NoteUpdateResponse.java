package com.atinroy.recallr.domain.note.dto;

public record NoteUpdateResponse(
        String id,
        String title,
        String content,
        String subjectId,
        String topicId
) {}
