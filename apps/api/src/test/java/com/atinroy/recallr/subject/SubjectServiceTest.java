package com.atinroy.recallr.subject;

import com.atinroy.recallr.domain.subject.*;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.domain.subject.dto.SubjectRequest;
import com.atinroy.recallr.domain.subject.dto.SubjectResponse;
import com.atinroy.recallr.domain.subject.dto.SubjectUpdateRequest;
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
class SubjectServiceTest {

    @Mock
    SubjectRepository subjectRepository;
    @Mock
    SubjectMapper subjectMapper;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    SubjectService subjectService;

    private User user;
    private Subject subject;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
    }

    @Test
    void createSubject_withValidRequest_returnsResponse() {
        SubjectRequest request = new SubjectRequest("Math", "Mathematics");
        SubjectResponse expected = new SubjectResponse(subject.getId().toString(), "Math", "Mathematics");
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectMapper.toEntity(request, user)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(subjectMapper.toResponse(subject)).thenReturn(expected);

        SubjectResponse result = subjectService.createSubject(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getSubjectById_whenFound_returnsResponse() {
        SubjectResponse expected = new SubjectResponse(subject.getId().toString(), "Math", null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(expected);

        assertThat(subjectService.getSubjectById(subject.getId())).isEqualTo(expected);
    }

    @Test
    void getSubjectById_whenNotFound_throwsSubjectNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getSubjectById(id))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void updateSubject_whenFound_updatesFieldsAndReturnsResponse() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(new SubjectResponse(subject.getId().toString(), "New", "Desc"));

        subjectService.updateSubject(subject.getId(), new SubjectUpdateRequest("New", "Desc"));

        assertThat(subject.getName()).isEqualTo("New");
        assertThat(subject.getDescription()).isEqualTo("Desc");
    }

    @Test
    void updateSubject_whenNotFound_throwsSubjectNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.updateSubject(id, new SubjectUpdateRequest("X", null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void deleteSubject_whenFound_deletesSubject() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));

        subjectService.deleteSubject(subject.getId());

        verify(subjectRepository).delete(subject);
    }

    @Test
    void deleteSubject_whenNotFound_throwsSubjectNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.deleteSubject(id))
                .isInstanceOf(SubjectNotFoundException.class);
    }
}
