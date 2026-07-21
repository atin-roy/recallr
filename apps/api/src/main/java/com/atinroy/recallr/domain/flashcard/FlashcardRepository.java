package com.atinroy.recallr.domain.flashcard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    Optional<Flashcard> findByIdAndDeckId(UUID id, UUID deckId);
    List<Flashcard> findByDeckId(UUID deckId);

    long countByDeckId(UUID deckId);

    @Query("select f.deck.id as deckId, count(f) as count " +
           "from Flashcard f where f.deck.id in :deckIds group by f.deck.id")
    List<DeckCardCount> countByDeckIds(@Param("deckIds") Collection<UUID> deckIds);

    interface DeckCardCount {
        UUID getDeckId();
        long getCount();
    }
}
