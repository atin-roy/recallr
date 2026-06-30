package com.atinroy.recallr.domain.flashcard;

import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.deck.DeckNotFoundException;
import com.atinroy.recallr.domain.deck.DeckRepository;
import com.atinroy.recallr.domain.flashcard.dto.FlashcardRequest;
import com.atinroy.recallr.domain.flashcard.dto.FlashcardResponse;
import com.atinroy.recallr.domain.flashcard.dto.FlashcardUpdateRequest;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final DeckRepository deckRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final FlashcardMapper flashcardMapper;

    @Transactional
    public FlashcardResponse createFlashcard(UUID deckId, FlashcardRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        Flashcard card = flashcardMapper.toEntity(request, deck.getUser(), deck);
        return flashcardMapper.toResponse(flashcardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public FlashcardResponse getFlashcardById(UUID deckId, UUID flashcardId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        Flashcard card = flashcardRepository.findByIdAndDeckId(flashcardId, deckId)
                .orElseThrow(() -> new FlashcardNotFoundException(flashcardId));
        return flashcardMapper.toResponse(card);
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> listFlashcardsByDeck(UUID deckId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        return flashcardRepository.findByDeckId(deckId).stream()
                .map(flashcardMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FlashcardResponse updateFlashcard(UUID deckId, UUID flashcardId, FlashcardUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        Flashcard card = flashcardRepository.findByIdAndDeckId(flashcardId, deckId)
                .orElseThrow(() -> new FlashcardNotFoundException(flashcardId));
        flashcardMapper.applyUpdate(request, card);
        return flashcardMapper.toResponse(flashcardRepository.save(card));
    }

    @Transactional
    public void deleteFlashcard(UUID deckId, UUID flashcardId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        Flashcard card = flashcardRepository.findByIdAndDeckId(flashcardId, deckId)
                .orElseThrow(() -> new FlashcardNotFoundException(flashcardId));
        flashcardRepository.delete(card);
    }
}
