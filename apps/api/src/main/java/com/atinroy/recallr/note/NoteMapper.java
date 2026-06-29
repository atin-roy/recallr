package com.atinroy.recallr.note;

import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.user.User;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public Note toEntity(NoteRequest request, User user) {
        Note note = new Note();
        note.setUser(user);
        note.setTitle(request.title());
        note.setContent(request.content());
        return note;
    }

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent()
        );
    }

    public NoteUpdateResponse toUpdateResponse(Note note) {
        return new NoteUpdateResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent()
        );
    }
}
