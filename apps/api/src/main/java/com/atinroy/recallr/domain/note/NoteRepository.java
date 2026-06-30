package com.atinroy.recallr.domain.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    Optional<Note> findByIdAndUserId(UUID noteId, UUID userId);
    Optional<Note> findByIdAndNotebookId(UUID noteId, UUID notebookId);
}
