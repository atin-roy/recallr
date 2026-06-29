package com.atinroy.recallr.note.dto;

import java.util.UUID;

public record NoteResponse(
        String id,
        String title,
        String content,
        String subjectId,
        String topicId
) {}
