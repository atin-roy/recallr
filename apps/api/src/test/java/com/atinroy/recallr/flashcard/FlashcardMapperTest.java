package com.atinroy.recallr.flashcard;

import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.flashcard.*;
import com.atinroy.recallr.domain.flashcard.dto.*;
import com.atinroy.recallr.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FlashcardMapperTest {

    FlashcardMapper mapper = new FlashcardMapper();
    User user;
    Deck deck;

    @BeforeEach
    void setUp() {
        user = new User();
        deck = new Deck();
    }

    // ── Cloze ────────────────────────────────────────────────────────────────

    @Test
    void toEntity_cloze_mapsAllFields() {
        ClozeFlashcardRequest req = new ClozeFlashcardRequest();
        req.setQuestion("The {{sun}} is a {{star}}");
        req.setAnswers(List.of("sun", "star"));

        Flashcard result = mapper.toEntity(req, user, deck);

        assertThat(result).isInstanceOf(ClozeFlashcard.class);
        ClozeFlashcard card = (ClozeFlashcard) result;
        assertThat(card.getQuestion()).isEqualTo("The {{sun}} is a {{star}}");
        assertThat(card.getAnswers()).containsExactly("sun", "star");
        assertThat(card.getUser()).isEqualTo(user);
        assertThat(card.getDeck()).isEqualTo(deck);
    }

    @Test
    void toResponse_cloze_mapsAllFields() {
        ClozeFlashcard card = new ClozeFlashcard();
        card.setQuestion("The {{sun}} is a {{star}}");
        card.setAnswers(List.of("sun", "star"));
        card.setDeck(deck);

        FlashcardResponse result = mapper.toResponse(card);

        assertThat(result).isInstanceOf(ClozeFlashcardResponse.class);
        ClozeFlashcardResponse response = (ClozeFlashcardResponse) result;
        assertThat(response.getQuestion()).isEqualTo("The {{sun}} is a {{star}}");
        assertThat(response.getAnswers()).containsExactly("sun", "star");
    }

    @Test
    void applyUpdate_cloze_updatesFields() {
        ClozeFlashcard card = new ClozeFlashcard();
        card.setQuestion("Old question");
        card.setAnswers(List.of("old"));

        ClozeFlashcardUpdateRequest req = new ClozeFlashcardUpdateRequest();
        req.setQuestion("New {{question}}");
        req.setAnswers(List.of("question"));

        mapper.applyUpdate(req, card);

        assertThat(card.getQuestion()).isEqualTo("New {{question}}");
        assertThat(card.getAnswers()).containsExactly("question");
    }

    @Test
    void applyUpdate_cloze_withMCQCard_throwsBadRequestException() {
        MCQFlashcard card = new MCQFlashcard();
        ClozeFlashcardUpdateRequest req = new ClozeFlashcardUpdateRequest();
        req.setQuestion("q");
        req.setAnswers(List.of("a"));

        assertThatThrownBy(() -> mapper.applyUpdate(req, card))
                .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
    }

    // ── True/False ────────────────────────────────────────────────────────────

    @Test
    void toEntity_trueFalse_mapsAllFields() {
        TrueFalseFlashcardRequest req = new TrueFalseFlashcardRequest();
        req.setQuestion("The Earth is flat.");
        req.setCorrectAnswer(false);
        req.setExplanation("The Earth is an oblate spheroid.");

        Flashcard result = mapper.toEntity(req, user, deck);

        assertThat(result).isInstanceOf(TrueFalseFlashcard.class);
        TrueFalseFlashcard card = (TrueFalseFlashcard) result;
        assertThat(card.getQuestion()).isEqualTo("The Earth is flat.");
        assertThat(card.isCorrectAnswer()).isFalse();
        assertThat(card.getExplanation()).isEqualTo("The Earth is an oblate spheroid.");
    }

    @Test
    void toResponse_trueFalse_mapsAllFields() {
        TrueFalseFlashcard card = new TrueFalseFlashcard();
        card.setQuestion("The Earth is flat.");
        card.setCorrectAnswer(false);
        card.setExplanation("The Earth is an oblate spheroid.");
        card.setDeck(deck);

        FlashcardResponse result = mapper.toResponse(card);

        assertThat(result).isInstanceOf(TrueFalseFlashcardResponse.class);
        TrueFalseFlashcardResponse response = (TrueFalseFlashcardResponse) result;
        assertThat(response.isCorrectAnswer()).isFalse();
        assertThat(response.getExplanation()).isEqualTo("The Earth is an oblate spheroid.");
    }

    @Test
    void applyUpdate_trueFalse_updatesFields() {
        TrueFalseFlashcard card = new TrueFalseFlashcard();
        card.setCorrectAnswer(false);
        card.setExplanation("Old");

        TrueFalseFlashcardUpdateRequest req = new TrueFalseFlashcardUpdateRequest();
        req.setQuestion("Updated statement.");
        req.setCorrectAnswer(true);
        req.setExplanation("Updated explanation.");

        mapper.applyUpdate(req, card);

        assertThat(card.isCorrectAnswer()).isTrue();
        assertThat(card.getExplanation()).isEqualTo("Updated explanation.");
    }

    @Test
    void applyUpdate_trueFalse_withClozeCard_throwsBadRequestException() {
        ClozeFlashcard card = new ClozeFlashcard();
        TrueFalseFlashcardUpdateRequest req = new TrueFalseFlashcardUpdateRequest();
        req.setQuestion("q");

        assertThatThrownBy(() -> mapper.applyUpdate(req, card))
                .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    @Test
    void toEntity_matching_mapsAllFields() {
        MatchingPairDto pair1 = new MatchingPairDto();
        pair1.setPrompt("France");
        pair1.setMatchValue("Paris");
        MatchingPairDto pair2 = new MatchingPairDto();
        pair2.setPrompt("Japan");
        pair2.setMatchValue("Tokyo");

        MatchingFlashcardRequest req = new MatchingFlashcardRequest();
        req.setQuestion("Match each country to its capital.");
        req.setPairs(List.of(pair1, pair2));

        Flashcard result = mapper.toEntity(req, user, deck);

        assertThat(result).isInstanceOf(MatchingFlashcard.class);
        MatchingFlashcard card = (MatchingFlashcard) result;
        assertThat(card.getQuestion()).isEqualTo("Match each country to its capital.");
        assertThat(card.getPairs()).hasSize(2);
        assertThat(card.getPairs().get(0).getPrompt()).isEqualTo("France");
        assertThat(card.getPairs().get(0).getMatchValue()).isEqualTo("Paris");
        assertThat(card.getPairs().get(1).getPrompt()).isEqualTo("Japan");
        assertThat(card.getPairs().get(1).getMatchValue()).isEqualTo("Tokyo");
    }

    @Test
    void toResponse_matching_mapsAllFields() {
        MatchingPair pair = new MatchingPair();
        pair.setPrompt("France");
        pair.setMatchValue("Paris");

        MatchingFlashcard card = new MatchingFlashcard();
        card.setQuestion("Match capitals.");
        card.setPairs(List.of(pair));
        card.setDeck(deck);

        FlashcardResponse result = mapper.toResponse(card);

        assertThat(result).isInstanceOf(MatchingFlashcardResponse.class);
        MatchingFlashcardResponse response = (MatchingFlashcardResponse) result;
        assertThat(response.getPairs()).hasSize(1);
        assertThat(response.getPairs().get(0).getPrompt()).isEqualTo("France");
        assertThat(response.getPairs().get(0).getMatchValue()).isEqualTo("Paris");
    }

    @Test
    void applyUpdate_matching_updatesFields() {
        MatchingFlashcard card = new MatchingFlashcard();
        card.setQuestion("Old question");

        MatchingPairDto newPair = new MatchingPairDto();
        newPair.setPrompt("Germany");
        newPair.setMatchValue("Berlin");

        MatchingFlashcardUpdateRequest req = new MatchingFlashcardUpdateRequest();
        req.setQuestion("New question");
        req.setPairs(List.of(newPair));

        mapper.applyUpdate(req, card);

        assertThat(card.getQuestion()).isEqualTo("New question");
        assertThat(card.getPairs()).hasSize(1);
        assertThat(card.getPairs().get(0).getPrompt()).isEqualTo("Germany");
    }

    @Test
    void applyUpdate_matching_withTrueFalseCard_throwsBadRequestException() {
        TrueFalseFlashcard card = new TrueFalseFlashcard();
        MatchingFlashcardUpdateRequest req = new MatchingFlashcardUpdateRequest();
        req.setQuestion("q");
        req.setPairs(List.of());

        assertThatThrownBy(() -> mapper.applyUpdate(req, card))
                .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
    }
}
