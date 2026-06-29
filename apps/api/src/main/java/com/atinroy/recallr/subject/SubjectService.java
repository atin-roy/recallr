package com.atinroy.recallr.subject;

import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.dto.SubjectRequest;
import com.atinroy.recallr.subject.dto.SubjectResponse;
import com.atinroy.recallr.subject.dto.SubjectUpdateRequest;
import com.atinroy.recallr.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Subject saved = subjectRepository.save(subjectMapper.toEntity(request, user));
        return subjectMapper.toResponse(saved);
    }

    public SubjectResponse getSubjectById(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
        return subjectMapper.toResponse(subject);
    }

    @Transactional
    public SubjectResponse updateSubject(UUID subjectId, SubjectUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
        subject.setName(request.name());
        subject.setDescription(request.description());
        return subjectMapper.toResponse(subject);
    }

    @Transactional
    public void deleteSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
        subjectRepository.delete(subject);
    }
}
