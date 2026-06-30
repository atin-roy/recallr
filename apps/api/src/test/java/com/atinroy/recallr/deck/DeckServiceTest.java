package com.atinroy.recallr.deck;

import com.atinroy.recallr.domain.deck.*;
import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.deck.dto.DeckUpdateRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    DeckRepository deckRepository;
    @Mock
    DeckMapper deckMapper;
    @Mock
    NotebookRepository notebookRepository;
    @Mock
    AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    DeckService deckService;

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
    void createInNotebook_whenNotebookFound_returnsDeckResponse() {
        DeckRequest request = new DeckRequest("My Deck", "A description");
        DeckResponse expected = new DeckResponse(deck.getId(), notebook.getId(), "My Deck", "A description", null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(deckMapper.toEntity(request, user, notebook)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck)).thenReturn(expected);

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
    void createStandalone_returnsDeckResponse() {
        DeckRequest request = new DeckRequest("Standalone Deck", null);
        DeckResponse expected = new DeckResponse(deck.getId(), null, "Standalone Deck", null, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckMapper.toEntity(request, user, null)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck)).thenReturn(expected);

        DeckResponse result = deckService.createStandalone(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getDeckById_whenFound_returnsDeckResponse() {
        DeckResponse expected = new DeckResponse(deck.getId(), null, "My Deck", null, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(deckMapper.toResponse(deck)).thenReturn(expected);

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
    void updateDeck_whenFound_updatesFieldsAndReturnsResponse() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(deckMapper.toResponse(deck)).thenReturn(new DeckResponse(deck.getId(), null, "New Name", "New Desc", null, null));

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
    void listByUser_returnsAllUserDecks() {
        User localUser = new User();
        UUID userId = localUser.getId();
        Deck deck1 = new Deck(); deck1.setUser(localUser);
        Deck deck2 = new Deck(); deck2.setUser(localUser);
        DeckResponse resp1 = new DeckResponse(null, null, "Deck 1", null, null, null);
        DeckResponse resp2 = new DeckResponse(null, null, "Deck 2", null, null, null);

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(localUser);
        when(deckRepository.findByUserId(userId)).thenReturn(List.of(deck1, deck2));
        when(deckMapper.toResponse(deck1)).thenReturn(resp1);
        when(deckMapper.toResponse(deck2)).thenReturn(resp2);

        List<DeckResponse> result = deckService.listByUser();

        assertThat(result).containsExactly(resp1, resp2);
    }
}
