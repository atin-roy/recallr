package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookRepository;
import com.atinroy.recallr.domain.note.dto.NoteRequest;
import com.atinroy.recallr.domain.note.dto.NoteResponse;
import com.atinroy.recallr.domain.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.domain.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final NotebookRepository notebookRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public NoteResponse createNote(UUID notebookId, NoteRequest request) {
        Notebook notebook = resolveNotebook(notebookId);
        Note saved = noteRepository.save(noteMapper.toEntity(request, notebook));
        return noteMapper.toResponse(saved);
    }

    public NoteResponse getNoteById(UUID notebookId, UUID noteId) {
        resolveNotebook(notebookId);
        Note note = noteRepository.findByIdAndNotebookId(noteId, notebookId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        return noteMapper.toResponse(note);
    }

    @Transactional
    public NoteUpdateResponse updateNote(UUID notebookId, UUID noteId, NoteUpdateRequest request) {
        resolveNotebook(notebookId);
        Note note = noteRepository.findByIdAndNotebookId(noteId, notebookId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        note.setTitle(request.title());
        note.setContent(request.content());
        return noteMapper.toUpdateResponse(note);
    }

    @Transactional
    public void deleteNote(UUID notebookId, UUID noteId) {
        resolveNotebook(notebookId);
        Note note = noteRepository.findByIdAndNotebookId(noteId, notebookId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        noteRepository.delete(note);
    }

    private Notebook resolveNotebook(UUID notebookId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotebookNotFoundException(notebookId));
    }
}
