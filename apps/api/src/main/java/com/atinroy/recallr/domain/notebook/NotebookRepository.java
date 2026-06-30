package com.atinroy.recallr.domain.notebook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
    Optional<Notebook> findByIdAndUserId(UUID id, UUID userId);
}
