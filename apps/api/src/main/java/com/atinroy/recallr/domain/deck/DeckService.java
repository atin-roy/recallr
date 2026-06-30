package com.atinroy.recallr.domain.deck;

import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.deck.dto.DeckUpdateRequest;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookRepository;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckMapper deckMapper;
    private final NotebookRepository notebookRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public DeckResponse createInNotebook(UUID notebookId, DeckRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Notebook notebook = notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotebookNotFoundException(notebookId));
        User user = notebook.getUser();
        Deck saved = deckRepository.save(deckMapper.toEntity(request, user, notebook));
        return deckMapper.toResponse(saved);
    }

    @Transactional
    public DeckResponse createStandalone(DeckRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Deck saved = deckRepository.save(deckMapper.toEntity(request, user, null));
        return deckMapper.toResponse(saved);
    }

    public DeckResponse getDeckById(UUID deckId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = resolveDeck(deckId, userId);
        return deckMapper.toResponse(deck);
    }

    @Transactional
    public DeckResponse updateDeck(UUID deckId, DeckUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = resolveDeck(deckId, userId);
        deck.setName(request.name());
        deck.setDescription(request.description());
        return deckMapper.toResponse(deck);
    }

    @Transactional
    public void deleteDeck(UUID deckId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = resolveDeck(deckId, userId);
        deckRepository.delete(deck);
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> listByUser() {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return deckRepository.findByUserId(userId).stream()
                .map(deckMapper::toResponse)
                .toList();
    }

    public List<DeckResponse> listByNotebook(UUID notebookId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotebookNotFoundException(notebookId));
        return deckRepository.findByNotebookId(notebookId).stream()
                .map(deckMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Deck resolveDeck(UUID deckId, UUID userId) {
        return deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
    }
}
