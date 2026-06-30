package com.atinroy.recallr.domain.topic;

import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.domain.subject.Subject;
import com.atinroy.recallr.domain.subject.SubjectNotFoundException;
import com.atinroy.recallr.domain.subject.SubjectRepository;
import com.atinroy.recallr.domain.topic.dto.TopicRequest;
import com.atinroy.recallr.domain.topic.dto.TopicResponse;
import com.atinroy.recallr.domain.topic.dto.TopicUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;
    private final SubjectRepository subjectRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public TopicResponse createTopic(UUID subjectId, TopicRequest request) {
        Subject subject = resolveSubject(subjectId);
        Topic saved = topicRepository.save(topicMapper.toEntity(request, subject));
        return topicMapper.toResponse(saved);
    }

    public TopicResponse getTopicById(UUID subjectId, UUID topicId) {
        resolveSubject(subjectId);
        Topic topic = topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new TopicNotFoundException("Topic not found"));
        return topicMapper.toResponse(topic);
    }

    @Transactional
    public TopicResponse updateTopic(UUID subjectId, UUID topicId, TopicUpdateRequest request) {
        resolveSubject(subjectId);
        Topic topic = topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new TopicNotFoundException("Topic not found"));
        topic.setName(request.name());
        topic.setDescription(request.description());
        return topicMapper.toResponse(topic);
    }

    @Transactional
    public void deleteTopic(UUID subjectId, UUID topicId) {
        resolveSubject(subjectId);
        Topic topic = topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new TopicNotFoundException("Topic not found"));
        topicRepository.delete(topic);
    }

    private Subject resolveSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
    }
}
