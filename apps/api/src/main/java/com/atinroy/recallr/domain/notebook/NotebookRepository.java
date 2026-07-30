package com.atinroy.recallr.domain.notebook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
    Optional<Notebook> findByIdAndUserId(UUID id, UUID userId);

    List<Notebook> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    // lower() rather than Spring Data's IgnoreCase (which generates upper()) so these
    // match the uk_notebooks_user_name expression index.
    @Query("select count(n) > 0 from Notebook n where n.user.id = :userId and lower(n.name) = lower(:name)")
    boolean existsByUserIdAndNameIgnoreCase(@Param("userId") UUID userId, @Param("name") String name);

    @Query("select count(n) > 0 from Notebook n where n.user.id = :userId and lower(n.name) = lower(:name) and n.id <> :excludedId")
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(@Param("userId") UUID userId, @Param("name") String name, @Param("excludedId") UUID excludedId);
}
