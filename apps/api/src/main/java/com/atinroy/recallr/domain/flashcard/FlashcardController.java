package com.atinroy.recallr.domain.flashcard;

import com.atinroy.recallr.domain.flashcard.dto.FlashcardRequest;
import com.atinroy.recallr.domain.flashcard.dto.FlashcardResponse;
import com.atinroy.recallr.domain.flashcard.dto.FlashcardUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/decks/{deckId}/flashcards")
public class FlashcardController {

    private final FlashcardService flashcardService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FlashcardResponse createFlashcard(@PathVariable UUID deckId,
                                              @RequestBody @Valid FlashcardRequest request) {
        return flashcardService.createFlashcard(deckId, request);
    }

    @GetMapping
    public List<FlashcardResponse> listFlashcardsByDeck(@PathVariable UUID deckId) {
        return flashcardService.listFlashcardsByDeck(deckId);
    }

    @GetMapping("/{flashcardId}")
    public FlashcardResponse getFlashcardById(@PathVariable UUID deckId,
                                               @PathVariable UUID flashcardId) {
        return flashcardService.getFlashcardById(deckId, flashcardId);
    }

    @PutMapping("/{flashcardId}")
    public FlashcardResponse updateFlashcard(@PathVariable UUID deckId,
                                              @PathVariable UUID flashcardId,
                                              @RequestBody @Valid FlashcardUpdateRequest request) {
        return flashcardService.updateFlashcard(deckId, flashcardId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{flashcardId}")
    public void deleteFlashcard(@PathVariable UUID deckId,
                                 @PathVariable UUID flashcardId) {
        flashcardService.deleteFlashcard(deckId, flashcardId);
    }
}
