package com.atinroy.recallr.topic;

import com.atinroy.recallr.domain.topic.*;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.domain.subject.Subject;
import com.atinroy.recallr.domain.subject.SubjectNotFoundException;
import com.atinroy.recallr.domain.subject.SubjectRepository;
import com.atinroy.recallr.domain.topic.dto.TopicRequest;
import com.atinroy.recallr.domain.topic.dto.TopicResponse;
import com.atinroy.recallr.domain.topic.dto.TopicUpdateRequest;
import com.atinroy.recallr.domain.user.User;
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
class TopicServiceTest {

    @Mock
    TopicRepository topicRepository;
    @Mock
    TopicMapper topicMapper;
    @Mock SubjectRepository subjectRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    TopicService topicService;

    private User user;
    private Subject subject;
    private Topic topic;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
        subject.setUser(user);
        topic = new Topic();
        topic.setSubject(subject);
    }

    @Test
    void createTopic_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.createTopic(subjectId, new TopicRequest("Algebra", null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void createTopic_withValidSubject_returnsResponse() {
        TopicRequest request = new TopicRequest("Algebra", null);
        TopicResponse expected = new TopicResponse(topic.getId().toString(), "Algebra", null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicMapper.toEntity(request, subject)).thenReturn(topic);
        when(topicRepository.save(topic)).thenReturn(topic);
        when(topicMapper.toResponse(topic)).thenReturn(expected);

        assertThat(topicService.createTopic(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getTopicById_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getTopicById(subjectId, UUID.randomUUID()))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void getTopicById_whenTopicNotFound_throwsTopicNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getTopicById(subject.getId(), topicId))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void getTopicById_whenFound_returnsResponse() {
        TopicResponse expected = new TopicResponse(topic.getId().toString(), "Algebra", null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));
        when(topicMapper.toResponse(topic)).thenReturn(expected);

        assertThat(topicService.getTopicById(subject.getId(), topic.getId())).isEqualTo(expected);
    }

    @Test
    void updateTopic_whenFound_updatesFieldsAndReturnsResponse() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));
        when(topicMapper.toResponse(topic)).thenReturn(new TopicResponse(topic.getId().toString(), "New", "Desc"));

        topicService.updateTopic(subject.getId(), topic.getId(), new TopicUpdateRequest("New", "Desc"));

        assertThat(topic.getName()).isEqualTo("New");
        assertThat(topic.getDescription()).isEqualTo("Desc");
    }

    @Test
    void updateTopic_whenTopicNotFound_throwsTopicNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.updateTopic(subject.getId(), topicId, new TopicUpdateRequest("X", null)))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void deleteTopic_whenFound_deletesTopic() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));

        topicService.deleteTopic(subject.getId(), topic.getId());

        verify(topicRepository).delete(topic);
    }

    @Test
    void deleteTopic_whenTopicNotFound_throwsTopicNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.deleteTopic(subject.getId(), topicId))
                .isInstanceOf(TopicNotFoundException.class);
    }
}
