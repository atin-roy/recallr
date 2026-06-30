package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.note.dto.NoteRequest;
import com.atinroy.recallr.domain.note.dto.NoteResponse;
import com.atinroy.recallr.domain.note.dto.NoteUpdateResponse;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public Note toEntity(NoteRequest request, Notebook notebook) {
        Note note = new Note();
        note.setUser(notebook.getUser());
        note.setNotebook(notebook);
        note.setTitle(request.title());
        note.setContent(request.content());
        return note;
    }

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent(),
                note.getNotebook().getId().toString()
        );
    }

    public NoteUpdateResponse toUpdateResponse(Note note) {
        return new NoteUpdateResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent(),
                note.getNotebook().getId().toString()
        );
    }
}
