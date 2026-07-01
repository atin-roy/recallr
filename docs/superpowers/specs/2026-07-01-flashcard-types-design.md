# Flashcard Types Design

**Date:** 2026-07-01
**Status:** Approved

## Overview

Add five new flashcard types to the backend — Cloze, True/False, Matching, Ordering, and Short Answer — using the existing single-table inheritance pattern established by `BasicFlashcard` and `MCQFlashcard`. No new endpoints; the existing `POST /decks/{deckId}/flashcards` and related routes handle all types via the polymorphic `type` discriminator.

## Data Model

All types extend `Flashcard` and share the `flashcards` table. The inherited `question` field holds the prompt for every type.

### ClozeFlashcard (`CLOZE`)
- `question` — text with `{{blank}}` markers, e.g. `"The mitochondria is the {{powerhouse}} of the {{cell}}"`
- `answers: List<String>` — correct answers in positional order, one per `{{blank}}`
- Stored in `flashcard_cloze_answers(flashcard_id, answer, answer_order)` with `@OrderColumn`

### TrueFalseFlashcard (`TRUE_FALSE`)
- `question` — the statement the student judges
- `correctAnswer: boolean`
- `explanation: String` (nullable) — reuses the existing `explanation` column from MCQ (STI advantage)

### MatchingFlashcard (`MATCHING`)
- `question` — optional context, e.g. `"Match each country to its capital"`
- `pairs: List<MatchingPair>` — each pair is `@Embeddable` with `prompt` and `matchValue`
- Stored in `flashcard_matching_pairs(flashcard_id, prompt, match_value, pair_order)`

### OrderingFlashcard (`ORDERING`)
- `question` — e.g. `"Arrange these events in chronological order"`
- `items: List<String>` — items in correct order; client shuffles for display
- Stored in `flashcard_ordering_items(flashcard_id, item, item_order)` with `@OrderColumn`

### ShortAnswerFlashcard (`SHORT_ANSWER`)
- `question` — the open-ended prompt
- `modelAnswer: String` (TEXT) — reference answer shown after student responds; grading is self-assessed

### New columns on `flashcards` table (nullable)
- `correct_answer BOOLEAN` — TrueFalseFlashcard
- `model_answer TEXT` — ShortAnswerFlashcard
- (`explanation` already exists from MCQ; reused by TrueFalse)

## DTOs & API Contract

Each type gets three DTOs. The three base classes gain five new `@JsonSubTypes` entries each.

**Discriminator names:** `CLOZE`, `TRUE_FALSE`, `MATCHING`, `ORDERING`, `SHORT_ANSWER`

| Type | Request/UpdateRequest fields | Response fields |
|---|---|---|
| Cloze | `question`, `answers: List<String>` | same |
| TrueFalse | `question`, `correctAnswer: boolean`, `explanation?` | same |
| Matching | `question`, `pairs: List<MatchingPairDto>` | same |
| Ordering | `question`, `items: List<String>` | same |
| ShortAnswer | `question`, `modelAnswer: String` | same |

`MatchingPairDto` has `prompt: String` and `matchValue: String`, shared across request and response.

**FlashcardMapper** gains one `instanceof` branch per type in each of `toEntity`, `toResponse`, and `applyUpdate`.

## DB Migration

`V2__flashcard_new_types.sql` (only V1 exists currently):

```sql
ALTER TABLE flashcards ADD COLUMN correct_answer BOOLEAN;
ALTER TABLE flashcards ADD COLUMN model_answer TEXT;

CREATE TABLE flashcard_cloze_answers (
    flashcard_id UUID NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    answer       VARCHAR(500) NOT NULL,
    answer_order INT NOT NULL
);

CREATE TABLE flashcard_matching_pairs (
    flashcard_id UUID NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    prompt       VARCHAR(500) NOT NULL,
    match_value  VARCHAR(500) NOT NULL,
    pair_order   INT NOT NULL
);

CREATE TABLE flashcard_ordering_items (
    flashcard_id UUID NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    item         TEXT NOT NULL,
    item_order   INT NOT NULL
);
```

## Files Added / Modified

### New entity classes (6)
- `flashcard/ClozeFlashcard.java`
- `flashcard/TrueFalseFlashcard.java`
- `flashcard/MatchingFlashcard.java`
- `flashcard/MatchingPair.java` (`@Embeddable`)
- `flashcard/OrderingFlashcard.java`
- `flashcard/ShortAnswerFlashcard.java`

### New DTOs (16)
- `dto/ClozeFlashcardRequest.java`, `ClozeFlashcardResponse.java`, `ClozeFlashcardUpdateRequest.java`
- `dto/TrueFalseFlashcardRequest.java`, `TrueFalseFlashcardResponse.java`, `TrueFalseFlashcardUpdateRequest.java`
- `dto/MatchingFlashcardRequest.java`, `MatchingFlashcardResponse.java`, `MatchingFlashcardUpdateRequest.java`
- `dto/MatchingPairDto.java` (shared across request/response)
- `dto/OrderingFlashcardRequest.java`, `OrderingFlashcardResponse.java`, `OrderingFlashcardUpdateRequest.java`
- `dto/ShortAnswerFlashcardRequest.java`, `ShortAnswerFlashcardResponse.java`, `ShortAnswerFlashcardUpdateRequest.java`

### Modified (4)
- `dto/FlashcardRequest.java` — add 5 `@JsonSubTypes` entries
- `dto/FlashcardResponse.java` — add 5 `@JsonSubTypes` entries
- `dto/FlashcardUpdateRequest.java` — add 5 `@JsonSubTypes` entries
- `FlashcardMapper.java` — add 5 branches in `toEntity`, `toResponse`, `applyUpdate`

### New migration (1)
- `db/migration/V2__flashcard_new_types.sql`
