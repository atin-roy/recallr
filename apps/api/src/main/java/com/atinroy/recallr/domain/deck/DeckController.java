package com.atinroy.recallr.domain.deck;

import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.deck.dto.DeckUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/notebooks/{notebookId}/decks")
    public DeckResponse createInNotebook(@PathVariable UUID notebookId,
                                         @RequestBody @Valid DeckRequest request) {
        return deckService.createInNotebook(notebookId, request);
    }

    @GetMapping("/notebooks/{notebookId}/decks")
    public List<DeckResponse> listByNotebook(@PathVariable UUID notebookId) {
        return deckService.listByNotebook(notebookId);
    }

    @GetMapping("/decks")
    public List<DeckResponse> listDecks() {
        return deckService.listByUser();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/decks")
    public DeckResponse createStandalone(@RequestBody @Valid DeckRequest request) {
        return deckService.createStandalone(request);
    }

    @GetMapping("/decks/{deckId}")
    public DeckResponse getDeckById(@PathVariable UUID deckId) {
        return deckService.getDeckById(deckId);
    }

    @PutMapping("/decks/{deckId}")
    public DeckResponse updateDeck(@PathVariable UUID deckId,
                                   @RequestBody @Valid DeckUpdateRequest request) {
        return deckService.updateDeck(deckId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/decks/{deckId}")
    public void deleteDeck(@PathVariable UUID deckId) {
        deckService.deleteDeck(deckId);
    }
}
