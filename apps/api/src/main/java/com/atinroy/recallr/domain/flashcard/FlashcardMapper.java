package com.atinroy.recallr.domain.flashcard;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.flashcard.dto.*;
import com.atinroy.recallr.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class FlashcardMapper {

    public Flashcard toEntity(FlashcardRequest request, User user, Deck deck) {
        if (request instanceof BasicFlashcardRequest r) {
            BasicFlashcard card = new BasicFlashcard();
            card.setUser(user);
            card.setDeck(deck);
            card.setQuestion(r.getQuestion());
            card.setAnswer(r.getAnswer());
            card.setReverse(r.isReverse());
            return card;
        } else if (request instanceof MCQFlashcardRequest r) {
            MCQFlashcard card = new MCQFlashcard();
            card.setUser(user);
            card.setDeck(deck);
            card.setQuestion(r.getQuestion());
            card.setOptions(r.getOptions());
            card.setCorrectOptionIndex(r.getCorrectOptionIndex());
            card.setExplanation(r.getExplanation());
            return card;
        }
        throw new IllegalArgumentException("Unknown flashcard type");
    }

    public FlashcardResponse toResponse(Flashcard card) {
        if (card instanceof BasicFlashcard c) {
            BasicFlashcardResponse r = new BasicFlashcardResponse();
            mapBase(card, r);
            r.setAnswer(c.getAnswer());
            r.setReverse(c.isReverse());
            return r;
        } else if (card instanceof MCQFlashcard c) {
            MCQFlashcardResponse r = new MCQFlashcardResponse();
            mapBase(card, r);
            r.setOptions(c.getOptions());
            r.setCorrectOptionIndex(c.getCorrectOptionIndex());
            r.setExplanation(c.getExplanation());
            return r;
        }
        throw new IllegalArgumentException("Unknown flashcard type");
    }

    public void applyUpdate(FlashcardUpdateRequest request, Flashcard card) {
        if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
            (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard))) {
            throw new BadRequestException("Update request type does not match flashcard type");
        }
        card.setQuestion(request.getQuestion());
        if (request instanceof BasicFlashcardUpdateRequest r && card instanceof BasicFlashcard c) {
            c.setAnswer(r.getAnswer());
            c.setReverse(r.isReverse());
        } else if (request instanceof MCQFlashcardUpdateRequest r && card instanceof MCQFlashcard c) {
            c.setOptions(r.getOptions());
            c.setCorrectOptionIndex(r.getCorrectOptionIndex());
            c.setExplanation(r.getExplanation());
        }
    }

    private void mapBase(Flashcard card, FlashcardResponse r) {
        r.setId(card.getId());
        r.setDeckId(card.getDeck().getId());
        r.setQuestion(card.getQuestion());
        r.setCreatedAt(card.getCreatedAt());
        r.setUpdatedAt(card.getUpdatedAt());
    }
}
