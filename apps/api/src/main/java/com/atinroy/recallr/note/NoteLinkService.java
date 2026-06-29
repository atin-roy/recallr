package com.atinroy.recallr.note;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.note.dto.NoteLinkRequest;
import com.atinroy.recallr.note.dto.NoteLinkResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.user.User;
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

    @Transactional
    public NoteLinkResponse createNoteLink(NoteLinkRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        Note source = findNoteForUser(parseUuid(request.sourceId()), user.getId());
        Note target = findNoteForUser(parseUuid(request.targetId()), user.getId());

        NoteLink link = new NoteLink();
        link.setSource(source);
        link.setTarget(target);

        NoteLink saved = noteLinkRepository.save(link);
        return toResponse(saved);
    }

    @Transactional
    public NoteLinkResponse updateNoteLink(UUID id, NoteLinkRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        NoteLink link = noteLinkRepository.findByIdAndSourceUserId(id, user.getId())
                .orElseThrow(() -> new NoteLinkNotFoundException("Note link not found"));

        Note source = findNoteForUser(parseUuid(request.sourceId()), user.getId());
        Note target = findNoteForUser(parseUuid(request.targetId()), user.getId());

        link.setSource(source);
        link.setTarget(target);

        NoteLink saved = noteLinkRepository.save(link);
        return toResponse(saved);
    }

    @Transactional
    public void deleteNoteLink(UUID id) {
        User user = authenticatedUserProvider.getCurrentUser();

        NoteLink link = noteLinkRepository.findByIdAndSourceUserId(id, user.getId())
                .orElseThrow(() -> new NoteLinkNotFoundException("Note link not found"));

        noteLinkRepository.delete(link);
    }

    private Note findNoteForUser(UUID noteId, UUID userId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
    }

    private NoteLinkResponse toResponse(NoteLink link) {
        return new NoteLinkResponse(
                link.getId(),
                link.getSource().getId(),
                link.getTarget().getId()
        );
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid note ID");
        }
    }
}
