package com.atinroy.recallr.domain.mcq;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.domain.mcq.dto.MCQResponse;
import com.atinroy.recallr.domain.mcq.dto.MCQUpdateRequest;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.domain.subject.Subject;
import com.atinroy.recallr.domain.subject.SubjectNotFoundException;
import com.atinroy.recallr.domain.subject.SubjectRepository;
import com.atinroy.recallr.domain.topic.Topic;
import com.atinroy.recallr.domain.topic.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MCQService {

    private final MCQRepository mcqRepository;
    private final MCQMapper mcqMapper;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public MCQResponse createMCQ(UUID subjectId, MCQCreateRequest request) {
        Subject subject = resolveSubject(subjectId);
        Topic topic = resolveTopic(request.topicId(), subjectId);
        MCQ saved = mcqRepository.save(mcqMapper.toEntity(request, subject, topic));
        return mcqMapper.toResponse(saved);
    }

    public MCQResponse getMCQById(UUID subjectId, UUID mcqId) {
        resolveSubject(subjectId);
        MCQ mcq = mcqRepository.findByIdAndSubjectId(mcqId, subjectId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        return mcqMapper.toResponse(mcq);
    }

    @Transactional
    public MCQResponse updateMCQ(UUID subjectId, UUID mcqId, MCQUpdateRequest request) {
        resolveSubject(subjectId);
        MCQ mcq = mcqRepository.findByIdAndSubjectId(mcqId, subjectId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        mcq.setQuestion(request.question());
        mcq.setOptions(request.options());
        mcq.setCorrectOptionIndex(request.correctOptionIndex());
        mcq.setExplanation(request.explanation());
        mcq.setTopic(resolveTopic(request.topicId(), subjectId));
        return mcqMapper.toResponse(mcq);
    }

    @Transactional
    public void deleteMCQ(UUID subjectId, UUID mcqId) {
        resolveSubject(subjectId);
        MCQ mcq = mcqRepository.findByIdAndSubjectId(mcqId, subjectId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        mcqRepository.delete(mcq);
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
