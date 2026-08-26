package com.atinroy.recallr.note;

import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookRepository;
import com.atinroy.recallr.domain.note.*;
import com.atinroy.recallr.domain.note.dto.NoteRequest;
import com.atinroy.recallr.domain.note.dto.NoteResponse;
import com.atinroy.recallr.domain.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    NoteRepository noteRepository;
    @Mock
    NoteMapper noteMapper;
    @Mock
    NotebookRepository notebookRepository;
    @Mock
    AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    NoteService noteService;

    private User user;
    private Notebook notebook;
    private Note note;

    @BeforeEach
    void setUp() {
        user = new User();
        notebook = new Notebook();
        notebook.setUser(user);
        note = new Note();
        note.setNotebook(notebook);
    }

    @Test
    void createNote_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(notebookId, new NoteRequest("T", null)))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void createNote_withValidRequest_returnsResponse() {
        NoteRequest request = new NoteRequest("Title", "Content");
        NoteResponse expected = new NoteResponse(note.getId().toString(), "Title", "Content", notebook.getId().toString());
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(noteMapper.toEntity(request, notebook)).thenReturn(note);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.createNote(notebook.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getNoteById_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(notebookId, UUID.randomUUID()))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void getNoteById_whenNoteNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(noteRepository.findByIdAndNotebookId(noteId, notebook.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(notebook.getId(), noteId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void getNoteById_whenFound_returnsResponse() {
        NoteResponse expected = new NoteResponse(note.getId().toString(), "T", "C", notebook.getId().toString());
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(noteRepository.findByIdAndNotebookId(note.getId(), notebook.getId())).thenReturn(Optional.of(note));
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.getNoteById(notebook.getId(), note.getId())).isEqualTo(expected);
    }

    @Test
    void updateNote_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(notebookId, note.getId(), new NoteUpdateRequest("T", null)))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void updateNote_whenNoteNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(noteRepository.findByIdAndNotebookId(noteId, notebook.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(notebook.getId(), noteId, new NoteUpdateRequest("T", null)))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void deleteNote_whenNoteNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(noteRepository.findByIdAndNotebookId(noteId, notebook.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote(notebook.getId(), noteId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void deleteNote_whenFound_deletesNote() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(noteRepository.findByIdAndNotebookId(note.getId(), notebook.getId())).thenReturn(Optional.of(note));

        noteService.deleteNote(notebook.getId(), note.getId());

        verify(noteRepository).delete(note);
    }
}
