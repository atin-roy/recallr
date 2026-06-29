package com.atinroy.recallr.mcq;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.mcq.dto.MCQUpdateRequest;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MCQServiceTest {

    @Mock MCQRepository mcqRepository;
    @Mock MCQMapper mcqMapper;
    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks MCQService mcqService;

    private User user;
    private Subject subject;
    private Topic topic;
    private MCQ mcq;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
        subject.setUser(user);
        topic = new Topic();
        topic.setSubject(subject);
        mcq = new MCQ();
        mcq.setSubject(subject);
    }

    @Test
    void createMCQ_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.createMCQ(subjectId, new MCQCreateRequest("Q", List.of("A", "B"), 0, null, null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void createMCQ_withTopicNotInSubject_throwsBadRequestException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.createMCQ(subject.getId(), new MCQCreateRequest("Q", List.of("A", "B"), 0, null, topicId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createMCQ_withoutTopic_returnsResponse() {
        MCQCreateRequest request = new MCQCreateRequest("Q?", List.of("A", "B"), 0, "Exp", null);
        MCQResponse expected = new MCQResponse(mcq.getId().toString(), "Q?", List.of("A", "B"), 0, "Exp", subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqMapper.toEntity(request, subject, null)).thenReturn(mcq);
        when(mcqRepository.save(mcq)).thenReturn(mcq);
        when(mcqMapper.toResponse(mcq)).thenReturn(expected);

        assertThat(mcqService.createMCQ(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getMCQById_whenNotFound_throwsMCQNotFoundException() {
        UUID mcqId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcqId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.getMCQById(subject.getId(), mcqId))
                .isInstanceOf(MCQNotFoundException.class);
    }

    @Test
    void getMCQById_whenFound_returnsResponse() {
        MCQResponse expected = new MCQResponse(mcq.getId().toString(), "Q", List.of("A"), 0, null, subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcq.getId(), subject.getId())).thenReturn(Optional.of(mcq));
        when(mcqMapper.toResponse(mcq)).thenReturn(expected);

        assertThat(mcqService.getMCQById(subject.getId(), mcq.getId())).isEqualTo(expected);
    }

    @Test
    void deleteMCQ_whenFound_deletesMCQ() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcq.getId(), subject.getId())).thenReturn(Optional.of(mcq));

        mcqService.deleteMCQ(subject.getId(), mcq.getId());

        verify(mcqRepository).delete(mcq);
    }

    @Test
    void deleteMCQ_whenNotFound_throwsMCQNotFoundException() {
        UUID mcqId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcqId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.deleteMCQ(subject.getId(), mcqId))
                .isInstanceOf(MCQNotFoundException.class);
    }

    @Test
    void updateMCQ_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.updateMCQ(subjectId, mcq.getId(),
                new MCQUpdateRequest("Q?", List.of("A", "B"), 0, null, null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void updateMCQ_whenMCQNotFound_throwsMCQNotFoundException() {
        UUID mcqId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcqId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.updateMCQ(subject.getId(), mcqId,
                new MCQUpdateRequest("Q?", List.of("A", "B"), 0, null, null)))
                .isInstanceOf(MCQNotFoundException.class);
    }

    @Test
    void updateMCQ_withTopicNotInSubject_throwsBadRequestException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcq.getId(), subject.getId())).thenReturn(Optional.of(mcq));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.updateMCQ(subject.getId(), mcq.getId(),
                new MCQUpdateRequest("Q?", List.of("A", "B"), 0, null, topicId)))
                .isInstanceOf(BadRequestException.class);
    }
}
