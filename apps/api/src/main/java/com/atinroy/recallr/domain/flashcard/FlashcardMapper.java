package com.atinroy.recallr.domain.flashcard;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.flashcard.dto.*;
import com.atinroy.recallr.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

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
        } else if (request instanceof ClozeFlashcardRequest r) {
            ClozeFlashcard card = new ClozeFlashcard();
            card.setUser(user);
            card.setDeck(deck);
            card.setQuestion(r.getQuestion());
            card.setAnswers(r.getAnswers());
            return card;
        } else if (request instanceof TrueFalseFlashcardRequest r) {
            TrueFalseFlashcard card = new TrueFalseFlashcard();
            card.setUser(user);
            card.setDeck(deck);
            card.setQuestion(r.getQuestion());
            card.setCorrectAnswer(r.isCorrectAnswer());
            card.setExplanation(r.getExplanation());
            return card;
        } else if (request instanceof MatchingFlashcardRequest r) {
            MatchingFlashcard card = new MatchingFlashcard();
            card.setUser(user);
            card.setDeck(deck);
            card.setQuestion(r.getQuestion());
            card.setPairs(r.getPairs().stream()
                    .map(dto -> {
                        MatchingPair p = new MatchingPair();
                        p.setPrompt(dto.getPrompt());
                        p.setMatchValue(dto.getMatchValue());
                        return p;
                    })
                    .collect(Collectors.toList()));
            return card;
        } else if (request instanceof OrderingFlashcardRequest r) {
            OrderingFlashcard card = new OrderingFlashcard();
            card.setUser(user);
            card.setDeck(deck);
            card.setQuestion(r.getQuestion());
            card.setItems(r.getItems());
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
        } else if (card instanceof ClozeFlashcard c) {
            ClozeFlashcardResponse r = new ClozeFlashcardResponse();
            mapBase(card, r);
            r.setAnswers(c.getAnswers());
            return r;
        } else if (card instanceof TrueFalseFlashcard c) {
            TrueFalseFlashcardResponse r = new TrueFalseFlashcardResponse();
            mapBase(card, r);
            r.setCorrectAnswer(c.isCorrectAnswer());
            r.setExplanation(c.getExplanation());
            return r;
        } else if (card instanceof MatchingFlashcard c) {
            MatchingFlashcardResponse r = new MatchingFlashcardResponse();
            mapBase(card, r);
            r.setPairs(c.getPairs().stream()
                    .map(p -> {
                        MatchingPairDto dto = new MatchingPairDto();
                        dto.setPrompt(p.getPrompt());
                        dto.setMatchValue(p.getMatchValue());
                        return dto;
                    })
                    .collect(Collectors.toList()));
            return r;
        } else if (card instanceof OrderingFlashcard c) {
            OrderingFlashcardResponse r = new OrderingFlashcardResponse();
            mapBase(card, r);
            r.setItems(c.getItems());
            return r;
        }
        throw new IllegalArgumentException("Unknown flashcard type");
    }

    public void applyUpdate(FlashcardUpdateRequest request, Flashcard card) {
        if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
            (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard)) ||
            (request instanceof ClozeFlashcardUpdateRequest && !(card instanceof ClozeFlashcard)) ||
            (request instanceof TrueFalseFlashcardUpdateRequest && !(card instanceof TrueFalseFlashcard)) ||
            (request instanceof MatchingFlashcardUpdateRequest && !(card instanceof MatchingFlashcard)) ||
            (request instanceof OrderingFlashcardUpdateRequest && !(card instanceof OrderingFlashcard))) {
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
        } else if (request instanceof ClozeFlashcardUpdateRequest r && card instanceof ClozeFlashcard c) {
            c.setAnswers(r.getAnswers());
        } else if (request instanceof TrueFalseFlashcardUpdateRequest r && card instanceof TrueFalseFlashcard c) {
            c.setCorrectAnswer(r.isCorrectAnswer());
            c.setExplanation(r.getExplanation());
        } else if (request instanceof MatchingFlashcardUpdateRequest r && card instanceof MatchingFlashcard c) {
            c.setPairs(r.getPairs().stream()
                    .map(dto -> {
                        MatchingPair p = new MatchingPair();
                        p.setPrompt(dto.getPrompt());
                        p.setMatchValue(dto.getMatchValue());
                        return p;
                    })
                    .collect(Collectors.toList()));
        } else if (request instanceof OrderingFlashcardUpdateRequest r && card instanceof OrderingFlashcard c) {
            c.setItems(r.getItems());
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
