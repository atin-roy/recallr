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
import com.atinroy.recallr.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock NoteRepository noteRepository;
    @Mock NoteMapper noteMapper;
    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks NoteService noteService;

    private User user;
    private Subject subject;
    private Topic topic;
    private Note note;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
        subject.setUser(user);
        topic = new Topic();
        topic.setSubject(subject);
        note = new Note();
        note.setSubject(subject);
    }

    @Test
    void createNote_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(subjectId, new NoteRequest("T", null, null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void createNote_withTopicNotInSubject_throwsBadRequestException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(subject.getId(), new NoteRequest("T", null, topicId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createNote_withoutTopic_returnsResponse() {
        NoteRequest request = new NoteRequest("Title", "Content", null);
        NoteResponse expected = new NoteResponse(note.getId().toString(), "Title", "Content", subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteMapper.toEntity(request, subject, null)).thenReturn(note);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.createNote(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void createNote_withValidTopic_assignsTopic() {
        NoteRequest request = new NoteRequest("Title", "Content", topic.getId());
        NoteResponse expected = new NoteResponse(note.getId().toString(), "Title", "Content", subject.getId().toString(), topic.getId().toString());
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));
        when(noteMapper.toEntity(request, subject, topic)).thenReturn(note);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.createNote(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getNoteById_whenNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(noteId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(subject.getId(), noteId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void getNoteById_whenFound_returnsResponse() {
        NoteResponse expected = new NoteResponse(note.getId().toString(), "T", "C", subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(note.getId(), subject.getId())).thenReturn(Optional.of(note));
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.getNoteById(subject.getId(), note.getId())).isEqualTo(expected);
    }

    @Test
    void deleteNote_whenNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(noteId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote(subject.getId(), noteId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void deleteNote_whenFound_deletesNote() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(note.getId(), subject.getId())).thenReturn(Optional.of(note));

        noteService.deleteNote(subject.getId(), note.getId());

        verify(noteRepository).delete(note);
    }
}
