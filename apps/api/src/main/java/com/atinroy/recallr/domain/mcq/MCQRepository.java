package com.atinroy.recallr.domain.mcq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MCQRepository extends JpaRepository<MCQ, UUID> {
    Optional<MCQ> findByIdAndSubjectId(UUID id, UUID subjectId);
}
