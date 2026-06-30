package com.atinroy.recallr.domain.notebook;

import com.atinroy.recallr.domain.notebook.dto.NotebookRequest;
import com.atinroy.recallr.domain.notebook.dto.NotebookResponse;
import com.atinroy.recallr.domain.notebook.dto.NotebookUpdateRequest;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private final NotebookRepository notebookRepository;
    private final NotebookMapper notebookMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public NotebookResponse createNotebook(NotebookRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Notebook saved = notebookRepository.save(notebookMapper.toEntity(request, user));
        return notebookMapper.toResponse(saved);
    }

    public NotebookResponse getNotebookById(UUID notebookId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Notebook notebook = resolveNotebook(notebookId, userId);
        return notebookMapper.toResponse(notebook);
    }

    @Transactional
    public NotebookResponse updateNotebook(UUID notebookId, NotebookUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Notebook notebook = resolveNotebook(notebookId, userId);
        notebook.setName(request.name());
        notebook.setDescription(request.description());
        return notebookMapper.toResponse(notebook);
    }

    @Transactional
    public void deleteNotebook(UUID notebookId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Notebook notebook = resolveNotebook(notebookId, userId);
        notebookRepository.delete(notebook);
    }

    private Notebook resolveNotebook(UUID notebookId, UUID userId) {
        return notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotebookNotFoundException(notebookId));
    }
}
