package com.atinroy.recallr.mcq;

import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.mcq.dto.MCQUpdateRequest;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MCQService {

    private final MCQRepository mcqRepository;
    private final MCQMapper mcqMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public MCQResponse createMCQ(MCQCreateRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        MCQ saved = mcqRepository.save(mcqMapper.toEntity(request, user));
        return mcqMapper.toResponse(saved);
    }

    public MCQResponse getMCQById(UUID mcqId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        MCQ mcq = mcqRepository.findByIdAndUserId(mcqId, userId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        return mcqMapper.toResponse(mcq);
    }

    @Transactional
    public MCQResponse updateMCQ(UUID mcqId, MCQUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        MCQ mcq = mcqRepository.findByIdAndUserId(mcqId, userId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));

        mcq.setQuestion(request.question());
        mcq.setOptions(request.options());
        mcq.setCorrectOptionIndex(request.correctOptionIndex());
        mcq.setExplanation(request.explanation());

        return mcqMapper.toResponse(mcq);
    }

    @Transactional
    public void deleteMCQ(UUID mcqId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        MCQ mcq = mcqRepository.findByIdAndUserId(mcqId, userId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        mcqRepository.delete(mcq);
    }
}
