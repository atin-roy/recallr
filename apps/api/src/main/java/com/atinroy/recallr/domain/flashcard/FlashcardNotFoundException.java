package com.atinroy.recallr.domain.flashcard;

import java.util.UUID;

public class FlashcardNotFoundException extends RuntimeException {
    public FlashcardNotFoundException(UUID id) {
        super("Flashcard not found: " + id);
    }
}
