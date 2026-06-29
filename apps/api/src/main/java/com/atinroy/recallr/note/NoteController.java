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
@RequestMapping("/subjects/{subjectId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public NoteResponse createNote(@PathVariable UUID subjectId,
                                   @RequestBody @Valid NoteRequest noteRequest) {
        return noteService.createNote(subjectId, noteRequest);
    }

    @GetMapping("/{noteId}")
    public NoteResponse getNoteById(@PathVariable UUID subjectId,
                                    @PathVariable UUID noteId) {
        return noteService.getNoteById(subjectId, noteId);
    }

    @PutMapping("/{noteId}")
    public NoteUpdateResponse updateNote(@PathVariable UUID subjectId,
                                         @PathVariable UUID noteId,
                                         @RequestBody @Valid NoteUpdateRequest noteUpdateRequest) {
        return noteService.updateNote(subjectId, noteId, noteUpdateRequest);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{noteId}")
    public void deleteNote(@PathVariable UUID subjectId,
                           @PathVariable UUID noteId) {
        noteService.deleteNote(subjectId, noteId);
    }
}
