package com.atinroy.recallr.domain.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteLinkRepository extends JpaRepository<NoteLink, UUID> {
    Optional<NoteLink> findByIdAndSourceUserId(UUID id, UUID userId);
}
