package com.atinroy.recallr.note;

import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public NoteResponse createNote(NoteRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        Note savedNote = noteRepository.save(noteMapper.toEntity(request, user));

        if (savedNote.getId() == null) {
            throw new IllegalStateException("Saved note ID was not generated");
        }

        return noteMapper.toResponse(savedNote);
    }

    @Transactional
    public NoteUpdateResponse updateNote(NoteUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();

        UUID noteId = parseUuid(request.id());
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));

        note.setTitle(request.title());
        note.setContent(request.content());

        return noteMapper.toUpdateResponse(note);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid note ID");
        }
    }
}