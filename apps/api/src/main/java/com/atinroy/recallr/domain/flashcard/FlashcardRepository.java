package com.atinroy.recallr.domain.flashcard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    Optional<Flashcard> findByIdAndDeckId(UUID id, UUID deckId);
    List<Flashcard> findByDeckId(UUID deckId);
}
