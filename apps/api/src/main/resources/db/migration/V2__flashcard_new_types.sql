ALTER TABLE flashcards ADD COLUMN correct_answer BOOLEAN;
ALTER TABLE flashcards ADD COLUMN model_answer TEXT;

CREATE TABLE flashcard_cloze_answers (
    flashcard_id UUID    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    answer       VARCHAR(500) NOT NULL,
    answer_order INT     NOT NULL
);

CREATE TABLE flashcard_matching_pairs (
    flashcard_id UUID    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    prompt       VARCHAR(500) NOT NULL,
    match_value  VARCHAR(500) NOT NULL,
    pair_order   INT     NOT NULL
);

CREATE TABLE flashcard_ordering_items (
    flashcard_id UUID    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    item         TEXT    NOT NULL,
    item_order   INT     NOT NULL
);
