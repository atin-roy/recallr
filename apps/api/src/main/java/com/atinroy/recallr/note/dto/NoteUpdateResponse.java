package com.atinroy.recallr.note.dto;

import java.util.UUID;

public record NoteUpdateResponse(
        String id,
        String title,
        String content,
        String subjectId,
        String topicId
) {}
