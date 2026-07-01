package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.domain.note.dto.NoteLinkRequest;
import com.atinroy.recallr.domain.note.dto.NoteLinkResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteLinkService {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final NoteRepository noteRepository;
    private final NoteLinkRepository noteLinkRepository;
    private final NoteLinkMapper noteLinkMapper;

    @Transactional
    public NoteLinkResponse createNoteLink(NoteLinkRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        NoteLink link = new NoteLink();
        return applyNotesAndSave(link, request, userId);
    }

    @Transactional
    public NoteLinkResponse updateNoteLink(UUID id, NoteLinkRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        NoteLink link = resolveLink(id, userId);
        return applyNotesAndSave(link, request, userId);
    }

    @Transactional
    public void deleteNoteLink(UUID id) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        NoteLink link = resolveLink(id, userId);
        noteLinkRepository.delete(link);
    }

    private NoteLinkResponse applyNotesAndSave(NoteLink link, NoteLinkRequest request, UUID userId) {
        link.setSource(findNoteForUser(parseUuid(request.sourceId()), userId));
        link.setTarget(findNoteForUser(parseUuid(request.targetId()), userId));
        return noteLinkMapper.toResponse(noteLinkRepository.save(link));
    }

    private NoteLink resolveLink(UUID id, UUID userId) {
        return noteLinkRepository.findByIdAndSourceUserId(id, userId)
                .orElseThrow(() -> new NoteLinkNotFoundException("Note link not found"));
    }

    private Note findNoteForUser(UUID noteId, UUID userId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid note ID");
        }
    }
}
