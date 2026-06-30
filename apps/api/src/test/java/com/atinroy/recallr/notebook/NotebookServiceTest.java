package com.atinroy.recallr.notebook;

import com.atinroy.recallr.domain.notebook.*;
import com.atinroy.recallr.domain.notebook.dto.NotebookRequest;
import com.atinroy.recallr.domain.notebook.dto.NotebookResponse;
import com.atinroy.recallr.domain.notebook.dto.NotebookUpdateRequest;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
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
class NotebookServiceTest {

    @Mock
    NotebookRepository notebookRepository;
    @Mock
    NotebookMapper notebookMapper;
    @Mock
    AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    NotebookService notebookService;

    private User user;
    private Notebook notebook;

    @BeforeEach
    void setUp() {
        user = new User();
        notebook = new Notebook();
    }

    @Test
    void createNotebook_withValidRequest_returnsResponse() {
        NotebookRequest request = new NotebookRequest("My Notebook", "A description");
        NotebookResponse expected = new NotebookResponse(notebook.getId(), "My Notebook", "A description", null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookMapper.toEntity(request, user)).thenReturn(notebook);
        when(notebookRepository.save(notebook)).thenReturn(notebook);
        when(notebookMapper.toResponse(notebook)).thenReturn(expected);

        NotebookResponse result = notebookService.createNotebook(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getNotebookById_whenFound_returnsResponse() {
        NotebookResponse expected = new NotebookResponse(notebook.getId(), "My Notebook", null, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(notebookMapper.toResponse(notebook)).thenReturn(expected);

        assertThat(notebookService.getNotebookById(notebook.getId())).isEqualTo(expected);
    }

    @Test
    void getNotebookById_whenNotFound_throwsNotebookNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notebookService.getNotebookById(id))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void updateNotebook_whenFound_updatesFieldsAndReturnsResponse() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(notebookMapper.toResponse(notebook)).thenReturn(new NotebookResponse(notebook.getId(), "New", "Desc", null, null));

        notebookService.updateNotebook(notebook.getId(), new NotebookUpdateRequest("New", "Desc"));

        assertThat(notebook.getName()).isEqualTo("New");
        assertThat(notebook.getDescription()).isEqualTo("Desc");
    }

    @Test
    void updateNotebook_whenNotFound_throwsNotebookNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notebookService.updateNotebook(id, new NotebookUpdateRequest("X", null)))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void deleteNotebook_whenFound_deletesNotebook() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));

        notebookService.deleteNotebook(notebook.getId());

        verify(notebookRepository).delete(notebook);
    }

    @Test
    void deleteNotebook_whenNotFound_throwsNotebookNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notebookService.deleteNotebook(id))
                .isInstanceOf(NotebookNotFoundException.class);
    }
}
