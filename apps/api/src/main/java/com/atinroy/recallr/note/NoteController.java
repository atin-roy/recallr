package com.atinroy.recallr.note;

import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @GetMapping("/{noteId}")
    public NoteResponse getNoteById(@PathVariable UUID noteId) {
        return noteService.getNoteById(noteId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public NoteResponse createNote(@RequestBody @Valid NoteRequest noteRequest) {
        return noteService.createNote(noteRequest);
    }

    @PutMapping("/{noteId}")
    public NoteUpdateResponse updateNote(@PathVariable UUID noteId, @RequestBody @Valid NoteUpdateRequest noteUpdateRequest) {
        return noteService.updateNote(noteId, noteUpdateRequest);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{noteId}")
    public void deleteNoteById(@PathVariable UUID noteId) {
        noteService.deleteNote(noteId);
    }
}
