package com.atinroy.recallr.note.dto;

import java.util.UUID;

public record NoteUpdateRequest(
        String title,
        String content,
        UUID topicId
) {}
