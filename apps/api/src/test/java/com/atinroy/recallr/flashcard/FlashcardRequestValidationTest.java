package com.atinroy.recallr.flashcard;

import com.atinroy.recallr.domain.flashcard.dto.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlashcardRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static MCQFlashcardRequest mcq(List<String> options, int index) {
        var r = new MCQFlashcardRequest();
        r.setQuestion("q");
        r.setOptions(options);
        r.setCorrectOptionIndex(index);
        return r;
    }

    private static MatchingPairDto pair(String prompt, String matchValue) {
        var p = new MatchingPairDto();
        p.setPrompt(prompt);
        p.setMatchValue(matchValue);
        return p;
    }

    @Test
    void blankQuestion_isRejected() {
        var r = new ShortAnswerFlashcardRequest();
        r.setQuestion("  ");
        r.setModelAnswer("a");
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void basic_blankAnswerIsRejected() {
        var r = new BasicFlashcardRequest();
        r.setQuestion("q");
        r.setAnswer(" ");
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void mcq_validCard_passes() {
        assertThat(validator.validate(mcq(List.of("a", "b"), 1))).isEmpty();
    }

    @Test
    void mcq_singleOptionIsRejected() {
        assertThat(validator.validate(mcq(List.of("a"), 0))).isNotEmpty();
    }

    @Test
    void mcq_blankOptionIsRejected() {
        assertThat(validator.validate(mcq(List.of("a", " "), 0))).hasSize(1);
    }

    @Test
    void mcq_negativeIndexIsRejected() {
        assertThat(validator.validate(mcq(List.of("a", "b"), -1))).hasSize(1);
    }

    @Test
    void mcq_indexPastLastOptionIsRejected() {
        var violations = validator.validate(mcq(List.of("a", "b"), 2));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("correctOptionIndex");
    }

    @Test
    void mcq_nullOptions_reportsOnlyTheSizeRule() {
        assertThat(validator.validate(mcq(null, 0))).hasSize(1);
    }

    @Test
    void mcqUpdate_indexPastLastOptionIsRejected() {
        var r = new MCQFlashcardUpdateRequest();
        r.setQuestion("q");
        r.setOptions(List.of("a", "b"));
        r.setCorrectOptionIndex(5);
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void cloze_emptyAnswersIsRejected() {
        var r = new ClozeFlashcardRequest();
        r.setQuestion("q");
        r.setAnswers(List.of());
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void matching_singlePairIsRejected() {
        var r = new MatchingFlashcardRequest();
        r.setQuestion("q");
        r.setPairs(List.of(pair("a", "b")));
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void matching_blankPairValueIsRejected() {
        var r = new MatchingFlashcardRequest();
        r.setQuestion("q");
        r.setPairs(List.of(pair("a", "b"), pair("c", " ")));
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void ordering_singleItemIsRejected() {
        var r = new OrderingFlashcardRequest();
        r.setQuestion("q");
        r.setItems(List.of("a"));
        assertThat(validator.validate(r)).hasSize(1);
    }

    @Test
    void trueFalse_needsOnlyAQuestion() {
        var r = new TrueFalseFlashcardRequest();
        r.setQuestion("q");
        assertThat(validator.validate(r)).isEmpty();
    }
}
