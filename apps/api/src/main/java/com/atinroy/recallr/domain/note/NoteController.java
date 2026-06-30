package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.domain.note.dto.NoteRequest;
import com.atinroy.recallr.domain.note.dto.NoteResponse;
import com.atinroy.recallr.domain.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.domain.note.dto.NoteUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notebooks/{notebookId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public NoteResponse createNote(@PathVariable UUID notebookId,
                                   @RequestBody @Valid NoteRequest noteRequest) {
        return noteService.createNote(notebookId, noteRequest);
    }

    @GetMapping("/{noteId}")
    public NoteResponse getNoteById(@PathVariable UUID notebookId,
                                    @PathVariable UUID noteId) {
        return noteService.getNoteById(notebookId, noteId);
    }

    @PutMapping("/{noteId}")
    public NoteUpdateResponse updateNote(@PathVariable UUID notebookId,
                                         @PathVariable UUID noteId,
                                         @RequestBody @Valid NoteUpdateRequest noteUpdateRequest) {
        return noteService.updateNote(notebookId, noteId, noteUpdateRequest);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{noteId}")
    public void deleteNote(@PathVariable UUID notebookId,
                           @PathVariable UUID noteId) {
        noteService.deleteNote(notebookId, noteId);
    }
}
