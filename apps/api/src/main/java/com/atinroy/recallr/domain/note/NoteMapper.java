package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.domain.note.dto.NoteRequest;
import com.atinroy.recallr.domain.note.dto.NoteResponse;
import com.atinroy.recallr.domain.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.domain.subject.Subject;
import com.atinroy.recallr.domain.topic.Topic;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public Note toEntity(NoteRequest request, Subject subject, Topic topic) {
        Note note = new Note();
        note.setUser(subject.getUser());
        note.setSubject(subject);
        note.setTopic(topic);
        note.setTitle(request.title());
        note.setContent(request.content());
        return note;
    }

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent(),
                note.getSubject().getId().toString(),
                note.getTopic() != null ? note.getTopic().getId().toString() : null
        );
    }

    public NoteUpdateResponse toUpdateResponse(Note note) {
        return new NoteUpdateResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent(),
                note.getSubject().getId().toString(),
                note.getTopic() != null ? note.getTopic().getId().toString() : null
        );
    }
}
