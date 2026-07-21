package com.atinroy.recallr.deck;

import com.atinroy.recallr.domain.deck.*;
import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.deck.dto.DeckUpdateRequest;
import com.atinroy.recallr.domain.flashcard.FlashcardRepository;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookRepository;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock DeckRepository deckRepository;
    @Mock DeckMapper deckMapper;
    @Mock NotebookRepository notebookRepository;
    @Mock FlashcardRepository flashcardRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks DeckService deckService;

    private User user;
    private Notebook notebook;
    private Deck deck;

    @BeforeEach
    void setUp() {
        user = new User();
        notebook = new Notebook();
        notebook.setUser(user);
        deck = new Deck();
    }

    @Test
    void createInNotebook_whenNotebookFound_returnsDeckResponseWithZeroCount() {
        DeckRequest request = new DeckRequest("My Deck", "A description");
        DeckResponse expected = new DeckResponse(deck.getId(), notebook.getId(), "My Deck", "A description", 0L, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(deckMapper.toEntity(request, user, notebook)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck, 0L)).thenReturn(expected);

        DeckResponse result = deckService.createInNotebook(notebook.getId(), request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void createInNotebook_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.createInNotebook(notebookId, new DeckRequest("Deck", null)))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void createStandalone_returnsDeckResponseWithZeroCount() {
        DeckRequest request = new DeckRequest("Standalone Deck", null);
        DeckResponse expected = new DeckResponse(deck.getId(), null, "Standalone Deck", null, 0L, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckMapper.toEntity(request, user, null)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck, 0L)).thenReturn(expected);

        DeckResponse result = deckService.createStandalone(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getDeckById_whenFound_returnsResponseWithCount() {
        DeckResponse expected = new DeckResponse(deck.getId(), null, "My Deck", null, 5L, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.countByDeckId(deck.getId())).thenReturn(5L);
        when(deckMapper.toResponse(deck, 5L)).thenReturn(expected);

        assertThat(deckService.getDeckById(deck.getId())).isEqualTo(expected);
    }

    @Test
    void getDeckById_whenNotFound_throwsDeckNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeckById(id))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void updateDeck_whenFound_updatesFieldsAndReturnsResponseWithCount() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.countByDeckId(deck.getId())).thenReturn(3L);
        when(deckMapper.toResponse(deck, 3L))
                .thenReturn(new DeckResponse(deck.getId(), null, "New Name", "New Desc", 3L, null, null));

        deckService.updateDeck(deck.getId(), new DeckUpdateRequest("New Name", "New Desc"));

        assertThat(deck.getName()).isEqualTo("New Name");
        assertThat(deck.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void updateDeck_whenNotFound_throwsDeckNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.updateDeck(id, new DeckUpdateRequest("X", null)))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void deleteDeck_whenFound_deletesDeck() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));

        deckService.deleteDeck(deck.getId());

        verify(deckRepository).delete(deck);
    }

    @Test
    void deleteDeck_whenNotFound_throwsDeckNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.deleteDeck(id))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void listByNotebook_mapsCountsOntoMatchingDecks() {
        UUID userId = user.getId();
        Deck deckA = new Deck();
        Deck deckB = new Deck();
        DeckCardCountStub countA = new DeckCardCountStub(deckA.getId(), 7L);
        // deckB deliberately absent from the grouped result -> should default to 0
        DeckResponse respA = new DeckResponse(deckA.getId(), null, "A", null, 7L, null, null);
        DeckResponse respB = new DeckResponse(deckB.getId(), null, "B", null, 0L, null, null);

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), userId)).thenReturn(Optional.of(notebook));
        when(deckRepository.findByNotebookId(notebook.getId())).thenReturn(List.of(deckA, deckB));
        when(flashcardRepository.countByDeckIds(anyCollection())).thenReturn(List.of(countA));
        when(deckMapper.toResponse(deckA, 7L)).thenReturn(respA);
        when(deckMapper.toResponse(deckB, 0L)).thenReturn(respB);

        List<DeckResponse> result = deckService.listByNotebook(notebook.getId());

        assertThat(result).containsExactly(respA, respB);
    }

    @Test
    void listByNotebook_whenNoDecks_returnsEmptyWithoutQueryingCounts() {
        UUID userId = user.getId();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), userId)).thenReturn(Optional.of(notebook));
        when(deckRepository.findByNotebookId(notebook.getId())).thenReturn(List.of());

        List<DeckResponse> result = deckService.listByNotebook(notebook.getId());

        assertThat(result).isEmpty();
        verify(flashcardRepository, never()).countByDeckIds(anyCollection());
    }

    @Test
    void listByNotebook_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.listByNotebook(notebookId))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void listByUser_returnsAllUserDecksWithCounts() {
        UUID userId = user.getId();
        Deck deck1 = new Deck();
        Deck deck2 = new Deck();
        DeckResponse resp1 = new DeckResponse(deck1.getId(), null, "Deck 1", null, 0L, null, null);
        DeckResponse resp2 = new DeckResponse(deck2.getId(), null, "Deck 2", null, 0L, null, null);

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByUserId(userId)).thenReturn(List.of(deck1, deck2));
        when(flashcardRepository.countByDeckIds(anyCollection())).thenReturn(List.of());
        when(deckMapper.toResponse(deck1, 0L)).thenReturn(resp1);
        when(deckMapper.toResponse(deck2, 0L)).thenReturn(resp2);

        List<DeckResponse> result = deckService.listByUser();

        assertThat(result).containsExactly(resp1, resp2);
    }

    /** Minimal stand-in for the interface-based projection FlashcardRepository.DeckCardCount. */
    private record DeckCardCountStub(UUID deckId, long count) implements FlashcardRepository.DeckCardCount {
        @Override public UUID getDeckId() { return deckId; }
        @Override public long getCount() { return count; }
    }
}
