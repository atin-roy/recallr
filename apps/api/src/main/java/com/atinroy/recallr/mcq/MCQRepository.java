package com.atinroy.recallr.mcq;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MCQRepository extends JpaRepository<MCQ, UUID> {
    Optional<MCQ> findByIdAndUserId(UUID id, UUID userId);
}