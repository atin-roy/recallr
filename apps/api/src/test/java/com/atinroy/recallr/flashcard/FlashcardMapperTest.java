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
}
