package com.atinroy.recallr.domain.note.dto;

import java.util.UUID;

public record NoteUpdateRequest(
        String title,
        String content,
        UUID topicId
) {}
