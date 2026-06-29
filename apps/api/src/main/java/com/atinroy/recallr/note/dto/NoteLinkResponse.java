package com.atinroy.recallr.note.dto;

import com.atinroy.recallr.note.NoteLink;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link NoteLink}
 */
public record NoteLinkResponse(UUID id, UUID sourceId, UUID targetId) implements Serializable {
}