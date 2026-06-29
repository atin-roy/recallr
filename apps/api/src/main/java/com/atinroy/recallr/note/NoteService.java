package com.atinroy.recallr.note;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.topic.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public NoteResponse createNote(UUID subjectId, NoteRequest request) {
        Subject subject = resolveSubject(subjectId);
        Topic topic = resolveTopic(request.topicId(), subjectId);
        Note saved = noteRepository.save(noteMapper.toEntity(request, subject, topic));
        return noteMapper.toResponse(saved);
    }

    public NoteResponse getNoteById(UUID subjectId, UUID noteId) {
        resolveSubject(subjectId);
        Note note = noteRepository.findByIdAndSubjectId(noteId, subjectId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        return noteMapper.toResponse(note);
    }

    @Transactional
    public NoteUpdateResponse updateNote(UUID subjectId, UUID noteId, NoteUpdateRequest request) {
        resolveSubject(subjectId);
        Note note = noteRepository.findByIdAndSubjectId(noteId, subjectId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setTopic(resolveTopic(request.topicId(), subjectId));
        return noteMapper.toUpdateResponse(note);
    }

    @Transactional
    public void deleteNote(UUID subjectId, UUID noteId) {
        resolveSubject(subjectId);
        Note note = noteRepository.findByIdAndSubjectId(noteId, subjectId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        noteRepository.delete(note);
    }

    private Subject resolveSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
    }

    private Topic resolveTopic(UUID topicId, UUID subjectId) {
        if (topicId == null) return null;
        return topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new BadRequestException("Topic not found in subject"));
    }
}
