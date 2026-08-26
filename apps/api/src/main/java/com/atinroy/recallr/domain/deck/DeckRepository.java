package com.atinroy.recallr.domain.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeckRepository extends JpaRepository<Deck, UUID> {
    Optional<Deck> findByIdAndUserId(UUID id, UUID userId);
    List<Deck> findByNotebookId(UUID notebookId);
    List<Deck> findByUserId(UUID userId);
}
