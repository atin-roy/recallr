package com.atinroy.recallr.flashcard;

import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.deck.DeckNotFoundException;
import com.atinroy.recallr.domain.deck.DeckRepository;
import com.atinroy.recallr.domain.flashcard.*;
import com.atinroy.recallr.domain.flashcard.dto.*;
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
class FlashcardServiceTest {

    @Mock
    FlashcardRepository flashcardRepository;
    @Mock
    DeckRepository deckRepository;
    @Mock
    AuthenticatedUserProvider authenticatedUserProvider;
    @Mock
    FlashcardMapper flashcardMapper;

    @InjectMocks
    FlashcardService flashcardService;

    private User user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        user = new User();
        deck = new Deck();
        deck.setUser(user);
    }

    @Test
    void createFlashcard_basic_returnsBasicFlashcardResponse() {
        BasicFlashcardRequest request = new BasicFlashcardRequest();
        request.setQuestion("What is Java?");
        request.setAnswer("A programming language");
        request.setReverse(false);

        BasicFlashcard card = new BasicFlashcard();
        BasicFlashcardResponse expected = new BasicFlashcardResponse();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardMapper.toEntity(request, user, deck)).thenReturn(card);
        when(flashcardRepository.save(card)).thenReturn(card);
        when(flashcardMapper.toResponse(card)).thenReturn(expected);

        FlashcardResponse result = flashcardService.createFlashcard(deck.getId(), request);

        assertThat(result).isEqualTo(expected);
        assertThat(result).isInstanceOf(BasicFlashcardResponse.class);
    }

    @Test
    void createFlashcard_mcq_returnsMCQFlashcardResponse() {
        MCQFlashcardRequest request = new MCQFlashcardRequest();
        request.setQuestion("What is OOP?");
        request.setOptions(List.of("A", "B", "C", "D"));
        request.setCorrectOptionIndex(0);
        request.setExplanation("Object-Oriented Programming");

        MCQFlashcard card = new MCQFlashcard();
        MCQFlashcardResponse expected = new MCQFlashcardResponse();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardMapper.toEntity(request, user, deck)).thenReturn(card);
        when(flashcardRepository.save(card)).thenReturn(card);
        when(flashcardMapper.toResponse(card)).thenReturn(expected);

        FlashcardResponse result = flashcardService.createFlashcard(deck.getId(), request);

        assertThat(result).isEqualTo(expected);
        assertThat(result).isInstanceOf(MCQFlashcardResponse.class);
    }

    @Test
    void createFlashcard_deckNotFound_throwsDeckNotFoundException() {
        UUID deckId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deckId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashcardService.createFlashcard(deckId, new BasicFlashcardRequest()))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void getFlashcardById_found_returnsResponse() {
        BasicFlashcard card = new BasicFlashcard();
        BasicFlashcardResponse expected = new BasicFlashcardResponse();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByIdAndDeckId(card.getId(), deck.getId())).thenReturn(Optional.of(card));
        when(flashcardMapper.toResponse(card)).thenReturn(expected);

        FlashcardResponse result = flashcardService.getFlashcardById(deck.getId(), card.getId());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getFlashcardById_notFound_throwsFlashcardNotFoundException() {
        UUID flashcardId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByIdAndDeckId(flashcardId, deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashcardService.getFlashcardById(deck.getId(), flashcardId))
                .isInstanceOf(FlashcardNotFoundException.class);
    }

    @Test
    void listFlashcardsByDeck_returnsMixedList() {
        BasicFlashcard basicCard = new BasicFlashcard();
        MCQFlashcard mcqCard = new MCQFlashcard();
        BasicFlashcardResponse basicResponse = new BasicFlashcardResponse();
        MCQFlashcardResponse mcqResponse = new MCQFlashcardResponse();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByDeckId(deck.getId())).thenReturn(List.of(basicCard, mcqCard));
        when(flashcardMapper.toResponse(basicCard)).thenReturn(basicResponse);
        when(flashcardMapper.toResponse(mcqCard)).thenReturn(mcqResponse);

        List<FlashcardResponse> result = flashcardService.listFlashcardsByDeck(deck.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isInstanceOf(BasicFlashcardResponse.class);
        assertThat(result.get(1)).isInstanceOf(MCQFlashcardResponse.class);
    }

    @Test
    void updateFlashcard_basic_appliesUpdate() {
        BasicFlashcard card = new BasicFlashcard();
        BasicFlashcardUpdateRequest request = new BasicFlashcardUpdateRequest();
        request.setQuestion("Updated question");
        request.setAnswer("Updated answer");
        request.setReverse(true);
        BasicFlashcardResponse expected = new BasicFlashcardResponse();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByIdAndDeckId(card.getId(), deck.getId())).thenReturn(Optional.of(card));
        when(flashcardRepository.save(card)).thenReturn(card);
        when(flashcardMapper.toResponse(card)).thenReturn(expected);

        FlashcardResponse result = flashcardService.updateFlashcard(deck.getId(), card.getId(), request);

        verify(flashcardMapper).applyUpdate(request, card);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void updateFlashcard_mcq_appliesUpdate() {
        MCQFlashcard card = new MCQFlashcard();
        MCQFlashcardUpdateRequest request = new MCQFlashcardUpdateRequest();
        request.setQuestion("Updated MCQ question");
        request.setOptions(List.of("X", "Y", "Z"));
        request.setCorrectOptionIndex(1);
        request.setExplanation("Updated explanation");
        MCQFlashcardResponse expected = new MCQFlashcardResponse();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByIdAndDeckId(card.getId(), deck.getId())).thenReturn(Optional.of(card));
        when(flashcardRepository.save(card)).thenReturn(card);
        when(flashcardMapper.toResponse(card)).thenReturn(expected);

        FlashcardResponse result = flashcardService.updateFlashcard(deck.getId(), card.getId(), request);

        verify(flashcardMapper).applyUpdate(request, card);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void deleteFlashcard_verifiesDeleteCalled() {
        BasicFlashcard card = new BasicFlashcard();

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByIdAndDeckId(card.getId(), deck.getId())).thenReturn(Optional.of(card));

        flashcardService.deleteFlashcard(deck.getId(), card.getId());

        verify(flashcardRepository).delete(card);
    }
}
