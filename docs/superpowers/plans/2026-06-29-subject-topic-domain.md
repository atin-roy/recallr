# Subject & Topic Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce Subject and Topic domains, nest Note/MCQ routes under subjects, and wire optional topic assignment on notes and MCQs.

**Architecture:** Subject is user-scoped at the top level; Topic belongs to Subject and is scoped through it (no direct user_id on topic table). Notes and MCQs gain a required `subject_id` FK and an optional `topic_id` FK; user_id stays on Note to preserve NoteLink queries. Services validate the subject belongs to the authenticated user before resolving child resources.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Flyway, PostgreSQL, JUnit 5 + Mockito + AssertJ

## Global Constraints

- All commands run from `apps/api/` directory (where `./mvnw` lives)
- Tests use `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` + AssertJ — no Spring context in unit tests
- Entities extend `BaseEntity` (UUID id generated in constructor, `createdAt`, `updatedAt`, `Persistable<UUID>` optimization)
- All services scope resources by `authenticatedUserProvider.getCurrentUser()`
- `SubjectNotFoundException` and `TopicNotFoundException` must be registered in `GlobalControllerAdvice`
- Flyway migrations are numbered sequentially; next is V3, then V4
- Test file run command: `./mvnw test -Dtest=<TestClassName> -q`

---

### Task 1: V3 Migration + Subject Scaffolding

Lay down the DB tables and all Subject plumbing except the service (which needs its own test cycle).

**Files:**
- Create: `src/main/resources/db/migration/V3__subjects_and_topics.sql`
- Create: `src/main/java/com/atinroy/recallr/subject/Subject.java`
- Create: `src/main/java/com/atinroy/recallr/subject/SubjectRepository.java`
- Create: `src/main/java/com/atinroy/recallr/subject/SubjectNotFoundException.java`
- Create: `src/main/java/com/atinroy/recallr/subject/dto/SubjectRequest.java`
- Create: `src/main/java/com/atinroy/recallr/subject/dto/SubjectResponse.java`
- Create: `src/main/java/com/atinroy/recallr/subject/dto/SubjectUpdateRequest.java`
- Create: `src/main/java/com/atinroy/recallr/subject/SubjectMapper.java`
- Modify: `src/main/java/com/atinroy/recallr/global/GlobalControllerAdvice.java`

**Interfaces:**
- Produces:
  - `SubjectRepository.findByIdAndUserId(UUID, UUID): Optional<Subject>`
  - `SubjectMapper.toEntity(SubjectRequest, User): Subject`
  - `SubjectMapper.toResponse(Subject): SubjectResponse`
  - `SubjectNotFoundException(String)`
  - Records: `SubjectRequest(String name, String description)`, `SubjectResponse(String id, String name, String description)`, `SubjectUpdateRequest(String name, String description)`

- [ ] **Step 1: Create V3 migration**

`src/main/resources/db/migration/V3__subjects_and_topics.sql`:
```sql
CREATE TABLE subjects (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE topics (
    id          UUID PRIMARY KEY,
    subject_id  UUID         NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
```

- [ ] **Step 2: Create Subject entity**

`src/main/java/com/atinroy/recallr/subject/Subject.java`:
```java
package com.atinroy.recallr.subject;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subjects")
@Getter
@Setter
public class Subject extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 200, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
}
```

- [ ] **Step 3: Create SubjectRepository**

`src/main/java/com/atinroy/recallr/subject/SubjectRepository.java`:
```java
package com.atinroy.recallr.subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByIdAndUserId(UUID id, UUID userId);
}
```

- [ ] **Step 4: Create SubjectNotFoundException**

`src/main/java/com/atinroy/recallr/subject/SubjectNotFoundException.java`:
```java
package com.atinroy.recallr.subject;

public class SubjectNotFoundException extends RuntimeException {
    public SubjectNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 5: Create DTOs**

`src/main/java/com/atinroy/recallr/subject/dto/SubjectRequest.java`:
```java
package com.atinroy.recallr.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
```

`src/main/java/com/atinroy/recallr/subject/dto/SubjectResponse.java`:
```java
package com.atinroy.recallr.subject.dto;

public record SubjectResponse(String id, String name, String description) {}
```

`src/main/java/com/atinroy/recallr/subject/dto/SubjectUpdateRequest.java`:
```java
package com.atinroy.recallr.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
```

- [ ] **Step 6: Create SubjectMapper**

`src/main/java/com/atinroy/recallr/subject/SubjectMapper.java`:
```java
package com.atinroy.recallr.subject;

import com.atinroy.recallr.subject.dto.SubjectRequest;
import com.atinroy.recallr.subject.dto.SubjectResponse;
import com.atinroy.recallr.user.User;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {

    public Subject toEntity(SubjectRequest request, User user) {
        Subject subject = new Subject();
        subject.setUser(user);
        subject.setName(request.name());
        subject.setDescription(request.description());
        return subject;
    }

    public SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId().toString(),
                subject.getName(),
                subject.getDescription()
        );
    }
}
```

- [ ] **Step 7: Register exceptions in GlobalControllerAdvice**

In `GlobalControllerAdvice.java`, update the `handleNotFound` handler to include `SubjectNotFoundException.class` (and `TopicNotFoundException.class` — add it now so Task 4 has nothing to touch here):

```java
// Add imports at top:
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.topic.TopicNotFoundException;

// Replace the existing handleNotFound method:
@ExceptionHandler({NoteNotFoundException.class, NoteLinkNotFoundException.class, MCQNotFoundException.class, SubjectNotFoundException.class, TopicNotFoundException.class})
public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
    return new ResponseEntity<>(errorBody(HttpStatus.NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
}
```

Note: `TopicNotFoundException` doesn't exist yet — the project won't compile until Task 4 creates it. If you need the project to compile after this task, skip the `TopicNotFoundException` import and handler entry for now and add it in Task 4.

- [ ] **Step 8: Verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/db/migration/V3__subjects_and_topics.sql \
        src/main/java/com/atinroy/recallr/subject/ \
        src/main/java/com/atinroy/recallr/global/GlobalControllerAdvice.java
git commit -m "feat(subject): add entity, repository, mapper, DTOs, exception"
```

---

### Task 2: SubjectService + Tests

**Files:**
- Create: `src/main/java/com/atinroy/recallr/subject/SubjectService.java`
- Create: `src/test/java/com/atinroy/recallr/subject/SubjectServiceTest.java`

**Interfaces:**
- Consumes: `SubjectRepository.findByIdAndUserId`, `SubjectMapper.toEntity`, `SubjectMapper.toResponse`, `SubjectNotFoundException`
- Produces:
  - `SubjectService.createSubject(SubjectRequest): SubjectResponse`
  - `SubjectService.getSubjectById(UUID): SubjectResponse`
  - `SubjectService.updateSubject(UUID, SubjectUpdateRequest): SubjectResponse`
  - `SubjectService.deleteSubject(UUID): void`

- [ ] **Step 1: Write failing tests**

`src/test/java/com/atinroy/recallr/subject/SubjectServiceTest.java`:
```java
package com.atinroy.recallr.subject;

import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.dto.SubjectRequest;
import com.atinroy.recallr.subject.dto.SubjectResponse;
import com.atinroy.recallr.subject.dto.SubjectUpdateRequest;
import com.atinroy.recallr.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock SubjectMapper subjectMapper;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks SubjectService subjectService;

    private User user;
    private Subject subject;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
    }

    @Test
    void createSubject_withValidRequest_returnsResponse() {
        SubjectRequest request = new SubjectRequest("Math", "Mathematics");
        SubjectResponse expected = new SubjectResponse(subject.getId().toString(), "Math", "Mathematics");
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectMapper.toEntity(request, user)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(subjectMapper.toResponse(subject)).thenReturn(expected);

        SubjectResponse result = subjectService.createSubject(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getSubjectById_whenFound_returnsResponse() {
        SubjectResponse expected = new SubjectResponse(subject.getId().toString(), "Math", null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(expected);

        assertThat(subjectService.getSubjectById(subject.getId())).isEqualTo(expected);
    }

    @Test
    void getSubjectById_whenNotFound_throwsSubjectNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getSubjectById(id))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void updateSubject_whenFound_updatesFieldsAndReturnsResponse() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(new SubjectResponse(subject.getId().toString(), "New", "Desc"));

        subjectService.updateSubject(subject.getId(), new SubjectUpdateRequest("New", "Desc"));

        assertThat(subject.getName()).isEqualTo("New");
        assertThat(subject.getDescription()).isEqualTo("Desc");
    }

    @Test
    void updateSubject_whenNotFound_throwsSubjectNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.updateSubject(id, new SubjectUpdateRequest("X", null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void deleteSubject_whenFound_deletesSubject() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));

        subjectService.deleteSubject(subject.getId());

        verify(subjectRepository).delete(subject);
    }

    @Test
    void deleteSubject_whenNotFound_throwsSubjectNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.deleteSubject(id))
                .isInstanceOf(SubjectNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests — expect failure (SubjectService missing)**

```bash
./mvnw test -Dtest=SubjectServiceTest -q
```
Expected: FAIL — `SubjectService` not found

- [ ] **Step 3: Implement SubjectService**

`src/main/java/com/atinroy/recallr/subject/SubjectService.java`:
```java
package com.atinroy.recallr.subject;

import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.dto.SubjectRequest;
import com.atinroy.recallr.subject.dto.SubjectResponse;
import com.atinroy.recallr.subject.dto.SubjectUpdateRequest;
import com.atinroy.recallr.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Subject saved = subjectRepository.save(subjectMapper.toEntity(request, user));
        return subjectMapper.toResponse(saved);
    }

    public SubjectResponse getSubjectById(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
        return subjectMapper.toResponse(subject);
    }

    @Transactional
    public SubjectResponse updateSubject(UUID subjectId, SubjectUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
        subject.setName(request.name());
        subject.setDescription(request.description());
        return subjectMapper.toResponse(subject);
    }

    @Transactional
    public void deleteSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
        subjectRepository.delete(subject);
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
./mvnw test -Dtest=SubjectServiceTest -q
```
Expected: BUILD SUCCESS, 7 tests passing

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/atinroy/recallr/subject/SubjectService.java \
        src/test/java/com/atinroy/recallr/subject/SubjectServiceTest.java
git commit -m "feat(subject): implement service with CRUD and tests"
```

---

### Task 3: SubjectController

**Files:**
- Create: `src/main/java/com/atinroy/recallr/subject/SubjectController.java`

**Interfaces:**
- Consumes: `SubjectService.createSubject`, `SubjectService.getSubjectById`, `SubjectService.updateSubject`, `SubjectService.deleteSubject`

- [ ] **Step 1: Create SubjectController**

`src/main/java/com/atinroy/recallr/subject/SubjectController.java`:
```java
package com.atinroy.recallr.subject;

import com.atinroy.recallr.subject.dto.SubjectRequest;
import com.atinroy.recallr.subject.dto.SubjectResponse;
import com.atinroy.recallr.subject.dto.SubjectUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SubjectResponse createSubject(@RequestBody @Valid SubjectRequest request) {
        return subjectService.createSubject(request);
    }

    @GetMapping("/{subjectId}")
    public SubjectResponse getSubjectById(@PathVariable UUID subjectId) {
        return subjectService.getSubjectById(subjectId);
    }

    @PutMapping("/{subjectId}")
    public SubjectResponse updateSubject(@PathVariable UUID subjectId,
                                         @RequestBody @Valid SubjectUpdateRequest request) {
        return subjectService.updateSubject(subjectId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{subjectId}")
    public void deleteSubject(@PathVariable UUID subjectId) {
        subjectService.deleteSubject(subjectId);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/atinroy/recallr/subject/SubjectController.java
git commit -m "feat(subject): add REST controller"
```

---

### Task 4: Topic Scaffolding

Fill in the existing `Topic.java` stub and add all Topic plumbing except the service.

**Files:**
- Modify: `src/main/java/com/atinroy/recallr/topic/Topic.java` (currently an empty stub)
- Create: `src/main/java/com/atinroy/recallr/topic/TopicRepository.java`
- Create: `src/main/java/com/atinroy/recallr/topic/TopicNotFoundException.java`
- Create: `src/main/java/com/atinroy/recallr/topic/dto/TopicRequest.java`
- Create: `src/main/java/com/atinroy/recallr/topic/dto/TopicResponse.java`
- Create: `src/main/java/com/atinroy/recallr/topic/dto/TopicUpdateRequest.java`
- Create: `src/main/java/com/atinroy/recallr/topic/TopicMapper.java`
- Modify: `src/main/java/com/atinroy/recallr/global/GlobalControllerAdvice.java` (add TopicNotFoundException if not done in Task 1)

**Interfaces:**
- Consumes: `Subject` entity (from Task 1)
- Produces:
  - `TopicRepository.findByIdAndSubjectId(UUID, UUID): Optional<Topic>`
  - `TopicMapper.toEntity(TopicRequest, Subject): Topic`
  - `TopicMapper.toResponse(Topic): TopicResponse`
  - `TopicNotFoundException(String)`
  - Records: `TopicRequest(String name, String description)`, `TopicResponse(String id, String name, String description)`, `TopicUpdateRequest(String name, String description)`

- [ ] **Step 1: Fill in Topic entity**

Replace the stub contents of `src/main/java/com/atinroy/recallr/topic/Topic.java`:
```java
package com.atinroy.recallr.topic;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.subject.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topics")
@Getter
@Setter
public class Topic extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(length = 200, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
}
```

- [ ] **Step 2: Create TopicRepository**

`src/main/java/com/atinroy/recallr/topic/TopicRepository.java`:
```java
package com.atinroy.recallr.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findByIdAndSubjectId(UUID id, UUID subjectId);
}
```

- [ ] **Step 3: Create TopicNotFoundException**

`src/main/java/com/atinroy/recallr/topic/TopicNotFoundException.java`:
```java
package com.atinroy.recallr.topic;

public class TopicNotFoundException extends RuntimeException {
    public TopicNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Create Topic DTOs**

`src/main/java/com/atinroy/recallr/topic/dto/TopicRequest.java`:
```java
package com.atinroy.recallr.topic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
```

`src/main/java/com/atinroy/recallr/topic/dto/TopicResponse.java`:
```java
package com.atinroy.recallr.topic.dto;

public record TopicResponse(String id, String name, String description) {}
```

`src/main/java/com/atinroy/recallr/topic/dto/TopicUpdateRequest.java`:
```java
package com.atinroy.recallr.topic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
```

- [ ] **Step 5: Create TopicMapper**

`src/main/java/com/atinroy/recallr/topic/TopicMapper.java`:
```java
package com.atinroy.recallr.topic;

import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.dto.TopicRequest;
import com.atinroy.recallr.topic.dto.TopicResponse;
import org.springframework.stereotype.Component;

@Component
public class TopicMapper {

    public Topic toEntity(TopicRequest request, Subject subject) {
        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName(request.name());
        topic.setDescription(request.description());
        return topic;
    }

    public TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId().toString(),
                topic.getName(),
                topic.getDescription()
        );
    }
}
```

- [ ] **Step 6: Register TopicNotFoundException in GlobalControllerAdvice (if not done in Task 1)**

If `TopicNotFoundException` was not included in Task 1's `handleNotFound`, add it now:
```java
import com.atinroy.recallr.topic.TopicNotFoundException;

// Update annotation to include TopicNotFoundException:
@ExceptionHandler({NoteNotFoundException.class, NoteLinkNotFoundException.class, MCQNotFoundException.class, SubjectNotFoundException.class, TopicNotFoundException.class})
public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
    return new ResponseEntity<>(errorBody(HttpStatus.NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
}
```

- [ ] **Step 7: Verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/atinroy/recallr/topic/ \
        src/main/java/com/atinroy/recallr/global/GlobalControllerAdvice.java
git commit -m "feat(topic): add entity, repository, mapper, DTOs, exception"
```

---

### Task 5: TopicService + Tests

**Files:**
- Create: `src/main/java/com/atinroy/recallr/topic/TopicService.java`
- Create: `src/test/java/com/atinroy/recallr/topic/TopicServiceTest.java`

**Interfaces:**
- Consumes:
  - `SubjectRepository.findByIdAndUserId(UUID, UUID): Optional<Subject>`
  - `TopicRepository.findByIdAndSubjectId(UUID, UUID): Optional<Topic>`
  - `TopicMapper.toEntity(TopicRequest, Subject): Topic`
  - `TopicMapper.toResponse(Topic): TopicResponse`
  - `SubjectNotFoundException(String)`, `TopicNotFoundException(String)`
- Produces:
  - `TopicService.createTopic(UUID subjectId, TopicRequest): TopicResponse`
  - `TopicService.getTopicById(UUID subjectId, UUID topicId): TopicResponse`
  - `TopicService.updateTopic(UUID subjectId, UUID topicId, TopicUpdateRequest): TopicResponse`
  - `TopicService.deleteTopic(UUID subjectId, UUID topicId): void`

- [ ] **Step 1: Write failing tests**

`src/test/java/com/atinroy/recallr/topic/TopicServiceTest.java`:
```java
package com.atinroy.recallr.topic;

import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.dto.TopicRequest;
import com.atinroy.recallr.topic.dto.TopicResponse;
import com.atinroy.recallr.topic.dto.TopicUpdateRequest;
import com.atinroy.recallr.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock TopicRepository topicRepository;
    @Mock TopicMapper topicMapper;
    @Mock SubjectRepository subjectRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks TopicService topicService;

    private User user;
    private Subject subject;
    private Topic topic;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
        subject.setUser(user);
        topic = new Topic();
        topic.setSubject(subject);
    }

    @Test
    void createTopic_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.createTopic(subjectId, new TopicRequest("Algebra", null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void createTopic_withValidSubject_returnsResponse() {
        TopicRequest request = new TopicRequest("Algebra", null);
        TopicResponse expected = new TopicResponse(topic.getId().toString(), "Algebra", null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicMapper.toEntity(request, subject)).thenReturn(topic);
        when(topicRepository.save(topic)).thenReturn(topic);
        when(topicMapper.toResponse(topic)).thenReturn(expected);

        assertThat(topicService.createTopic(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getTopicById_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getTopicById(subjectId, UUID.randomUUID()))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void getTopicById_whenTopicNotFound_throwsTopicNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getTopicById(subject.getId(), topicId))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void getTopicById_whenFound_returnsResponse() {
        TopicResponse expected = new TopicResponse(topic.getId().toString(), "Algebra", null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));
        when(topicMapper.toResponse(topic)).thenReturn(expected);

        assertThat(topicService.getTopicById(subject.getId(), topic.getId())).isEqualTo(expected);
    }

    @Test
    void updateTopic_whenFound_updatesFieldsAndReturnsResponse() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));
        when(topicMapper.toResponse(topic)).thenReturn(new TopicResponse(topic.getId().toString(), "New", "Desc"));

        topicService.updateTopic(subject.getId(), topic.getId(), new TopicUpdateRequest("New", "Desc"));

        assertThat(topic.getName()).isEqualTo("New");
        assertThat(topic.getDescription()).isEqualTo("Desc");
    }

    @Test
    void updateTopic_whenTopicNotFound_throwsTopicNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.updateTopic(subject.getId(), topicId, new TopicUpdateRequest("X", null)))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void deleteTopic_whenFound_deletesTopic() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));

        topicService.deleteTopic(subject.getId(), topic.getId());

        verify(topicRepository).delete(topic);
    }

    @Test
    void deleteTopic_whenTopicNotFound_throwsTopicNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.deleteTopic(subject.getId(), topicId))
                .isInstanceOf(TopicNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests — expect failure (TopicService missing)**

```bash
./mvnw test -Dtest=TopicServiceTest -q
```
Expected: FAIL — `TopicService` not found

- [ ] **Step 3: Implement TopicService**

`src/main/java/com/atinroy/recallr/topic/TopicService.java`:
```java
package com.atinroy.recallr.topic;

import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.dto.TopicRequest;
import com.atinroy.recallr.topic.dto.TopicResponse;
import com.atinroy.recallr.topic.dto.TopicUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;
    private final SubjectRepository subjectRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public TopicResponse createTopic(UUID subjectId, TopicRequest request) {
        Subject subject = resolveSubject(subjectId);
        Topic saved = topicRepository.save(topicMapper.toEntity(request, subject));
        return topicMapper.toResponse(saved);
    }

    public TopicResponse getTopicById(UUID subjectId, UUID topicId) {
        resolveSubject(subjectId);
        Topic topic = topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new TopicNotFoundException("Topic not found"));
        return topicMapper.toResponse(topic);
    }

    @Transactional
    public TopicResponse updateTopic(UUID subjectId, UUID topicId, TopicUpdateRequest request) {
        resolveSubject(subjectId);
        Topic topic = topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new TopicNotFoundException("Topic not found"));
        topic.setName(request.name());
        topic.setDescription(request.description());
        return topicMapper.toResponse(topic);
    }

    @Transactional
    public void deleteTopic(UUID subjectId, UUID topicId) {
        resolveSubject(subjectId);
        Topic topic = topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new TopicNotFoundException("Topic not found"));
        topicRepository.delete(topic);
    }

    private Subject resolveSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
./mvnw test -Dtest=TopicServiceTest -q
```
Expected: BUILD SUCCESS, 8 tests passing

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/atinroy/recallr/topic/TopicService.java \
        src/test/java/com/atinroy/recallr/topic/TopicServiceTest.java
git commit -m "feat(topic): implement service with CRUD and tests"
```

---

### Task 6: TopicController

**Files:**
- Create: `src/main/java/com/atinroy/recallr/topic/TopicController.java`

**Interfaces:**
- Consumes: `TopicService.createTopic`, `TopicService.getTopicById`, `TopicService.updateTopic`, `TopicService.deleteTopic`

- [ ] **Step 1: Create TopicController**

`src/main/java/com/atinroy/recallr/topic/TopicController.java`:
```java
package com.atinroy.recallr.topic;

import com.atinroy.recallr.topic.dto.TopicRequest;
import com.atinroy.recallr.topic.dto.TopicResponse;
import com.atinroy.recallr.topic.dto.TopicUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects/{subjectId}/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public TopicResponse createTopic(@PathVariable UUID subjectId,
                                     @RequestBody @Valid TopicRequest request) {
        return topicService.createTopic(subjectId, request);
    }

    @GetMapping("/{topicId}")
    public TopicResponse getTopicById(@PathVariable UUID subjectId,
                                      @PathVariable UUID topicId) {
        return topicService.getTopicById(subjectId, topicId);
    }

    @PutMapping("/{topicId}")
    public TopicResponse updateTopic(@PathVariable UUID subjectId,
                                     @PathVariable UUID topicId,
                                     @RequestBody @Valid TopicUpdateRequest request) {
        return topicService.updateTopic(subjectId, topicId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{topicId}")
    public void deleteTopic(@PathVariable UUID subjectId,
                            @PathVariable UUID topicId) {
        topicService.deleteTopic(subjectId, topicId);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/atinroy/recallr/topic/TopicController.java
git commit -m "feat(topic): add nested REST controller under /subjects/{subjectId}/topics"
```

---

### Task 7: V4 Migration + Note Domain Update

Add `subject_id` and `topic_id` to notes, move NoteController to nested route, update service scoping.

**Files:**
- Create: `src/main/resources/db/migration/V4__notes_mcqs_subject_topic.sql`
- Modify: `src/main/java/com/atinroy/recallr/note/Note.java`
- Modify: `src/main/java/com/atinroy/recallr/note/NoteRepository.java`
- Modify: `src/main/java/com/atinroy/recallr/note/dto/NoteRequest.java`
- Modify: `src/main/java/com/atinroy/recallr/note/dto/NoteResponse.java`
- Modify: `src/main/java/com/atinroy/recallr/note/dto/NoteUpdateRequest.java`
- Modify: `src/main/java/com/atinroy/recallr/note/dto/NoteUpdateResponse.java`
- Modify: `src/main/java/com/atinroy/recallr/note/NoteMapper.java`
- Modify: `src/main/java/com/atinroy/recallr/note/NoteService.java`
- Modify: `src/main/java/com/atinroy/recallr/note/NoteController.java`
- Create: `src/test/java/com/atinroy/recallr/note/NoteServiceTest.java`

**Interfaces:**
- Consumes:
  - `SubjectRepository.findByIdAndUserId(UUID, UUID): Optional<Subject>`
  - `TopicRepository.findByIdAndSubjectId(UUID, UUID): Optional<Topic>`
- Produces:
  - `NoteRepository.findByIdAndSubjectId(UUID noteId, UUID subjectId): Optional<Note>`
  - `NoteService.createNote(UUID subjectId, NoteRequest): NoteResponse`
  - `NoteService.getNoteById(UUID subjectId, UUID noteId): NoteResponse`
  - `NoteService.updateNote(UUID subjectId, UUID noteId, NoteUpdateRequest): NoteUpdateResponse`
  - `NoteService.deleteNote(UUID subjectId, UUID noteId): void`
  - `NoteMapper.toEntity(NoteRequest, Subject, Topic): Note` (Topic may be null)

- [ ] **Step 1: Create V4 migration**

`src/main/resources/db/migration/V4__notes_mcqs_subject_topic.sql`:
```sql
ALTER TABLE notes ADD COLUMN subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE;
ALTER TABLE notes ADD COLUMN topic_id   UUID REFERENCES topics(id) ON DELETE SET NULL;

ALTER TABLE mcqs ADD COLUMN subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE;
ALTER TABLE mcqs ADD COLUMN topic_id   UUID REFERENCES topics(id) ON DELETE SET NULL;
```

Note: This migration requires the `notes` and `mcqs` tables to be empty (dev environment only). If there is existing data, truncate both tables before running the app.

- [ ] **Step 2: Update Note entity**

Replace `src/main/java/com/atinroy/recallr/note/Note.java`:
```java
package com.atinroy.recallr.note;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(length = 100)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NoteLink> outgoingLinks = new HashSet<>();

    @OneToMany(mappedBy = "target")
    private Set<NoteLink> incomingLinks = new HashSet<>();
}
```

- [ ] **Step 3: Update NoteRepository**

Replace `src/main/java/com/atinroy/recallr/note/NoteRepository.java`:
```java
package com.atinroy.recallr.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    Optional<Note> findByIdAndUserId(UUID noteId, UUID userId);
    Optional<Note> findByIdAndSubjectId(UUID noteId, UUID subjectId);
}
```

- [ ] **Step 4: Update Note DTOs**

Replace `src/main/java/com/atinroy/recallr/note/dto/NoteRequest.java`:
```java
package com.atinroy.recallr.note.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record NoteRequest(
        @NotBlank String title,
        String content,
        UUID topicId
) {}
```

Replace `src/main/java/com/atinroy/recallr/note/dto/NoteResponse.java`:
```java
package com.atinroy.recallr.note.dto;

import java.util.UUID;

public record NoteResponse(
        String id,
        String title,
        String content,
        String subjectId,
        String topicId
) {}
```

Replace `src/main/java/com/atinroy/recallr/note/dto/NoteUpdateRequest.java`:
```java
package com.atinroy.recallr.note.dto;

import java.util.UUID;

public record NoteUpdateRequest(
        String title,
        String content,
        UUID topicId
) {}
```

Replace `src/main/java/com/atinroy/recallr/note/dto/NoteUpdateResponse.java`:
```java
package com.atinroy.recallr.note.dto;

import java.util.UUID;

public record NoteUpdateResponse(
        String id,
        String title,
        String content,
        String subjectId,
        String topicId
) {}
```

- [ ] **Step 5: Update NoteMapper**

Replace `src/main/java/com/atinroy/recallr/note/NoteMapper.java`:
```java
package com.atinroy.recallr.note;

import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.Topic;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public Note toEntity(NoteRequest request, Subject subject, Topic topic) {
        Note note = new Note();
        note.setUser(subject.getUser());
        note.setSubject(subject);
        note.setTopic(topic);
        note.setTitle(request.title());
        note.setContent(request.content());
        return note;
    }

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent(),
                note.getSubject().getId().toString(),
                note.getTopic() != null ? note.getTopic().getId().toString() : null
        );
    }

    public NoteUpdateResponse toUpdateResponse(Note note) {
        return new NoteUpdateResponse(
                note.getId().toString(),
                note.getTitle(),
                note.getContent(),
                note.getSubject().getId().toString(),
                note.getTopic() != null ? note.getTopic().getId().toString() : null
        );
    }
}
```

- [ ] **Step 6: Write failing NoteService tests**

`src/test/java/com/atinroy/recallr/note/NoteServiceTest.java`:
```java
package com.atinroy.recallr.note;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.topic.TopicRepository;
import com.atinroy.recallr.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock NoteRepository noteRepository;
    @Mock NoteMapper noteMapper;
    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks NoteService noteService;

    private User user;
    private Subject subject;
    private Topic topic;
    private Note note;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
        subject.setUser(user);
        topic = new Topic();
        topic.setSubject(subject);
        note = new Note();
        note.setSubject(subject);
    }

    @Test
    void createNote_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(subjectId, new NoteRequest("T", null, null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void createNote_withTopicNotInSubject_throwsBadRequestException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(subject.getId(), new NoteRequest("T", null, topicId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createNote_withoutTopic_returnsResponse() {
        NoteRequest request = new NoteRequest("Title", "Content", null);
        NoteResponse expected = new NoteResponse(note.getId().toString(), "Title", "Content", subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteMapper.toEntity(request, subject, null)).thenReturn(note);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.createNote(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void createNote_withValidTopic_assignsTopic() {
        NoteRequest request = new NoteRequest("Title", "Content", topic.getId());
        NoteResponse expected = new NoteResponse(note.getId().toString(), "Title", "Content", subject.getId().toString(), topic.getId().toString());
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topic.getId(), subject.getId())).thenReturn(Optional.of(topic));
        when(noteMapper.toEntity(request, subject, topic)).thenReturn(note);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.createNote(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getNoteById_whenNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(noteId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(subject.getId(), noteId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void getNoteById_whenFound_returnsResponse() {
        NoteResponse expected = new NoteResponse(note.getId().toString(), "T", "C", subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(note.getId(), subject.getId())).thenReturn(Optional.of(note));
        when(noteMapper.toResponse(note)).thenReturn(expected);

        assertThat(noteService.getNoteById(subject.getId(), note.getId())).isEqualTo(expected);
    }

    @Test
    void deleteNote_whenNotFound_throwsNoteNotFoundException() {
        UUID noteId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(noteId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote(subject.getId(), noteId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void deleteNote_whenFound_deletesNote() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(noteRepository.findByIdAndSubjectId(note.getId(), subject.getId())).thenReturn(Optional.of(note));

        noteService.deleteNote(subject.getId(), note.getId());

        verify(noteRepository).delete(note);
    }
}
```

- [ ] **Step 7: Run tests — expect failure (NoteService signature mismatch)**

```bash
./mvnw test -Dtest=NoteServiceTest -q
```
Expected: FAIL — compilation errors (old NoteService signatures don't match)

- [ ] **Step 8: Rewrite NoteService**

Replace `src/main/java/com/atinroy/recallr/note/NoteService.java`:
```java
package com.atinroy.recallr.note;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.topic.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public NoteResponse createNote(UUID subjectId, NoteRequest request) {
        Subject subject = resolveSubject(subjectId);
        Topic topic = resolveTopic(request.topicId(), subjectId);
        Note saved = noteRepository.save(noteMapper.toEntity(request, subject, topic));
        return noteMapper.toResponse(saved);
    }

    public NoteResponse getNoteById(UUID subjectId, UUID noteId) {
        resolveSubject(subjectId);
        Note note = noteRepository.findByIdAndSubjectId(noteId, subjectId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        return noteMapper.toResponse(note);
    }

    @Transactional
    public NoteUpdateResponse updateNote(UUID subjectId, UUID noteId, NoteUpdateRequest request) {
        resolveSubject(subjectId);
        Note note = noteRepository.findByIdAndSubjectId(noteId, subjectId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setTopic(resolveTopic(request.topicId(), subjectId));
        return noteMapper.toUpdateResponse(note);
    }

    @Transactional
    public void deleteNote(UUID subjectId, UUID noteId) {
        resolveSubject(subjectId);
        Note note = noteRepository.findByIdAndSubjectId(noteId, subjectId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found"));
        noteRepository.delete(note);
    }

    private Subject resolveSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
    }

    private Topic resolveTopic(UUID topicId, UUID subjectId) {
        if (topicId == null) return null;
        return topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new BadRequestException("Topic not found in subject"));
    }
}
```

- [ ] **Step 9: Run tests — expect pass**

```bash
./mvnw test -Dtest=NoteServiceTest -q
```
Expected: BUILD SUCCESS, 8 tests passing

- [ ] **Step 10: Update NoteController**

Replace `src/main/java/com/atinroy/recallr/note/NoteController.java`:
```java
package com.atinroy.recallr.note;

import com.atinroy.recallr.note.dto.NoteRequest;
import com.atinroy.recallr.note.dto.NoteResponse;
import com.atinroy.recallr.note.dto.NoteUpdateRequest;
import com.atinroy.recallr.note.dto.NoteUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects/{subjectId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public NoteResponse createNote(@PathVariable UUID subjectId,
                                   @RequestBody @Valid NoteRequest noteRequest) {
        return noteService.createNote(subjectId, noteRequest);
    }

    @GetMapping("/{noteId}")
    public NoteResponse getNoteById(@PathVariable UUID subjectId,
                                    @PathVariable UUID noteId) {
        return noteService.getNoteById(subjectId, noteId);
    }

    @PutMapping("/{noteId}")
    public NoteUpdateResponse updateNote(@PathVariable UUID subjectId,
                                         @PathVariable UUID noteId,
                                         @RequestBody @Valid NoteUpdateRequest noteUpdateRequest) {
        return noteService.updateNote(subjectId, noteId, noteUpdateRequest);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{noteId}")
    public void deleteNote(@PathVariable UUID subjectId,
                           @PathVariable UUID noteId) {
        noteService.deleteNote(subjectId, noteId);
    }
}
```

- [ ] **Step 11: Run all tests**

```bash
./mvnw test -q
```
Expected: BUILD SUCCESS, all tests passing (including existing NoteLinkServiceTest)

- [ ] **Step 12: Commit**

```bash
git add src/main/resources/db/migration/V4__notes_mcqs_subject_topic.sql \
        src/main/java/com/atinroy/recallr/note/ \
        src/test/java/com/atinroy/recallr/note/NoteServiceTest.java
git commit -m "feat(note): add subject/topic ownership, nest routes under /subjects/{subjectId}/notes"
```

---

### Task 8: MCQ Domain Update

Move MCQ routes under subjects and add subject/topic scoping.

**Files:**
- Modify: `src/main/java/com/atinroy/recallr/mcq/MCQ.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/MCQRepository.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/MCQCreateRequest.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/dto/MCQUpdateRequest.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/dto/MCQResponse.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/MCQMapper.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/MCQService.java`
- Modify: `src/main/java/com/atinroy/recallr/mcq/MCQController.java`
- Create: `src/test/java/com/atinroy/recallr/mcq/MCQServiceTest.java`

**Interfaces:**
- Consumes:
  - `SubjectRepository.findByIdAndUserId(UUID, UUID): Optional<Subject>`
  - `TopicRepository.findByIdAndSubjectId(UUID, UUID): Optional<Topic>`
- Produces:
  - `MCQRepository.findByIdAndSubjectId(UUID mcqId, UUID subjectId): Optional<MCQ>`
  - `MCQService.createMCQ(UUID subjectId, MCQCreateRequest): MCQResponse`
  - `MCQService.getMCQById(UUID subjectId, UUID mcqId): MCQResponse`
  - `MCQService.updateMCQ(UUID subjectId, UUID mcqId, MCQUpdateRequest): MCQResponse`
  - `MCQService.deleteMCQ(UUID subjectId, UUID mcqId): void`
  - `MCQMapper.toEntity(MCQCreateRequest, Subject, Topic): MCQ` (Topic may be null)

- [ ] **Step 1: Update MCQ entity**

Replace `src/main/java/com/atinroy/recallr/mcq/MCQ.java`:
```java
package com.atinroy.recallr.mcq;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mcqs")
@Getter
@Setter
public class MCQ extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column
    private String question;

    @ElementCollection
    private List<String> options = new ArrayList<>();

    @Column
    private int correctOptionIndex;

    @Column(length = 1000)
    private String explanation;
}
```

- [ ] **Step 2: Update MCQRepository**

Replace `src/main/java/com/atinroy/recallr/mcq/MCQRepository.java`:
```java
package com.atinroy.recallr.mcq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MCQRepository extends JpaRepository<MCQ, UUID> {
    Optional<MCQ> findByIdAndUserId(UUID id, UUID userId);
    Optional<MCQ> findByIdAndSubjectId(UUID id, UUID subjectId);
}
```

- [ ] **Step 3: Update MCQ DTOs**

Replace `src/main/java/com/atinroy/recallr/mcq/MCQCreateRequest.java`:
```java
package com.atinroy.recallr.mcq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MCQCreateRequest(
        @NotNull @NotBlank String question,
        @NotEmpty List<String> options,
        @PositiveOrZero int correctOptionIndex,
        String explanation,
        UUID topicId
) implements Serializable {}
```

Replace `src/main/java/com/atinroy/recallr/mcq/dto/MCQUpdateRequest.java`:
```java
package com.atinroy.recallr.mcq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record MCQUpdateRequest(
        @NotBlank String question,
        @NotEmpty List<String> options,
        @PositiveOrZero int correctOptionIndex,
        String explanation,
        UUID topicId
) {}
```

Replace `src/main/java/com/atinroy/recallr/mcq/dto/MCQResponse.java`:
```java
package com.atinroy.recallr.mcq.dto;

import java.util.List;

public record MCQResponse(
        String id,
        String question,
        List<String> options,
        int correctOptionIndex,
        String explanation,
        String subjectId,
        String topicId
) {}
```

- [ ] **Step 4: Update MCQMapper**

Replace `src/main/java/com/atinroy/recallr/mcq/MCQMapper.java`:
```java
package com.atinroy.recallr.mcq;

import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.Topic;
import org.springframework.stereotype.Component;

@Component
public class MCQMapper {

    public MCQ toEntity(MCQCreateRequest request, Subject subject, Topic topic) {
        MCQ mcq = new MCQ();
        mcq.setUser(subject.getUser());
        mcq.setSubject(subject);
        mcq.setTopic(topic);
        mcq.setQuestion(request.question());
        mcq.setOptions(request.options());
        mcq.setCorrectOptionIndex(request.correctOptionIndex());
        mcq.setExplanation(request.explanation());
        return mcq;
    }

    public MCQResponse toResponse(MCQ mcq) {
        return new MCQResponse(
                mcq.getId().toString(),
                mcq.getQuestion(),
                mcq.getOptions(),
                mcq.getCorrectOptionIndex(),
                mcq.getExplanation(),
                mcq.getSubject().getId().toString(),
                mcq.getTopic() != null ? mcq.getTopic().getId().toString() : null
        );
    }
}
```

- [ ] **Step 5: Write failing MCQService tests**

`src/test/java/com/atinroy/recallr/mcq/MCQServiceTest.java`:
```java
package com.atinroy.recallr.mcq;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.mcq.dto.MCQUpdateRequest;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.topic.TopicRepository;
import com.atinroy.recallr.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MCQServiceTest {

    @Mock MCQRepository mcqRepository;
    @Mock MCQMapper mcqMapper;
    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks MCQService mcqService;

    private User user;
    private Subject subject;
    private Topic topic;
    private MCQ mcq;

    @BeforeEach
    void setUp() {
        user = new User();
        subject = new Subject();
        subject.setUser(user);
        topic = new Topic();
        topic.setSubject(subject);
        mcq = new MCQ();
        mcq.setSubject(subject);
    }

    @Test
    void createMCQ_whenSubjectNotFound_throwsSubjectNotFoundException() {
        UUID subjectId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subjectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.createMCQ(subjectId, new MCQCreateRequest("Q", List.of("A", "B"), 0, null, null)))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void createMCQ_withTopicNotInSubject_throwsBadRequestException() {
        UUID topicId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(topicRepository.findByIdAndSubjectId(topicId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.createMCQ(subject.getId(), new MCQCreateRequest("Q", List.of("A", "B"), 0, null, topicId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createMCQ_withoutTopic_returnsResponse() {
        MCQCreateRequest request = new MCQCreateRequest("Q?", List.of("A", "B"), 0, "Exp", null);
        MCQResponse expected = new MCQResponse(mcq.getId().toString(), "Q?", List.of("A", "B"), 0, "Exp", subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqMapper.toEntity(request, subject, null)).thenReturn(mcq);
        when(mcqRepository.save(mcq)).thenReturn(mcq);
        when(mcqMapper.toResponse(mcq)).thenReturn(expected);

        assertThat(mcqService.createMCQ(subject.getId(), request)).isEqualTo(expected);
    }

    @Test
    void getMCQById_whenNotFound_throwsMCQNotFoundException() {
        UUID mcqId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcqId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.getMCQById(subject.getId(), mcqId))
                .isInstanceOf(MCQNotFoundException.class);
    }

    @Test
    void getMCQById_whenFound_returnsResponse() {
        MCQResponse expected = new MCQResponse(mcq.getId().toString(), "Q", List.of("A"), 0, null, subject.getId().toString(), null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcq.getId(), subject.getId())).thenReturn(Optional.of(mcq));
        when(mcqMapper.toResponse(mcq)).thenReturn(expected);

        assertThat(mcqService.getMCQById(subject.getId(), mcq.getId())).isEqualTo(expected);
    }

    @Test
    void deleteMCQ_whenFound_deletesMCQ() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcq.getId(), subject.getId())).thenReturn(Optional.of(mcq));

        mcqService.deleteMCQ(subject.getId(), mcq.getId());

        verify(mcqRepository).delete(mcq);
    }

    @Test
    void deleteMCQ_whenNotFound_throwsMCQNotFoundException() {
        UUID mcqId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(subjectRepository.findByIdAndUserId(subject.getId(), user.getId())).thenReturn(Optional.of(subject));
        when(mcqRepository.findByIdAndSubjectId(mcqId, subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcqService.deleteMCQ(subject.getId(), mcqId))
                .isInstanceOf(MCQNotFoundException.class);
    }
}
```

- [ ] **Step 6: Run tests — expect failure (MCQService signature mismatch)**

```bash
./mvnw test -Dtest=MCQServiceTest -q
```
Expected: FAIL — compilation errors

- [ ] **Step 7: Rewrite MCQService**

Replace `src/main/java/com/atinroy/recallr/mcq/MCQService.java`:
```java
package com.atinroy.recallr.mcq;

import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.mcq.dto.MCQUpdateRequest;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.subject.SubjectNotFoundException;
import com.atinroy.recallr.subject.SubjectRepository;
import com.atinroy.recallr.topic.Topic;
import com.atinroy.recallr.topic.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MCQService {

    private final MCQRepository mcqRepository;
    private final MCQMapper mcqMapper;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public MCQResponse createMCQ(UUID subjectId, MCQCreateRequest request) {
        Subject subject = resolveSubject(subjectId);
        Topic topic = resolveTopic(request.topicId(), subjectId);
        MCQ saved = mcqRepository.save(mcqMapper.toEntity(request, subject, topic));
        return mcqMapper.toResponse(saved);
    }

    public MCQResponse getMCQById(UUID subjectId, UUID mcqId) {
        resolveSubject(subjectId);
        MCQ mcq = mcqRepository.findByIdAndSubjectId(mcqId, subjectId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        return mcqMapper.toResponse(mcq);
    }

    @Transactional
    public MCQResponse updateMCQ(UUID subjectId, UUID mcqId, MCQUpdateRequest request) {
        resolveSubject(subjectId);
        MCQ mcq = mcqRepository.findByIdAndSubjectId(mcqId, subjectId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        mcq.setQuestion(request.question());
        mcq.setOptions(request.options());
        mcq.setCorrectOptionIndex(request.correctOptionIndex());
        mcq.setExplanation(request.explanation());
        mcq.setTopic(resolveTopic(request.topicId(), subjectId));
        return mcqMapper.toResponse(mcq);
    }

    @Transactional
    public void deleteMCQ(UUID subjectId, UUID mcqId) {
        resolveSubject(subjectId);
        MCQ mcq = mcqRepository.findByIdAndSubjectId(mcqId, subjectId)
                .orElseThrow(() -> new MCQNotFoundException("MCQ not found"));
        mcqRepository.delete(mcq);
    }

    private Subject resolveSubject(UUID subjectId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
    }

    private Topic resolveTopic(UUID topicId, UUID subjectId) {
        if (topicId == null) return null;
        return topicRepository.findByIdAndSubjectId(topicId, subjectId)
                .orElseThrow(() -> new BadRequestException("Topic not found in subject"));
    }
}
```

- [ ] **Step 8: Run tests — expect pass**

```bash
./mvnw test -Dtest=MCQServiceTest -q
```
Expected: BUILD SUCCESS, 6 tests passing

- [ ] **Step 9: Update MCQController**

Replace `src/main/java/com/atinroy/recallr/mcq/MCQController.java`:
```java
package com.atinroy.recallr.mcq;

import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.mcq.dto.MCQUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects/{subjectId}/mcqs")
@RequiredArgsConstructor
public class MCQController {

    private final MCQService mcqService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MCQResponse createMCQ(@PathVariable UUID subjectId,
                                  @RequestBody @Valid MCQCreateRequest request) {
        return mcqService.createMCQ(subjectId, request);
    }

    @GetMapping("/{mcqId}")
    public MCQResponse getMCQById(@PathVariable UUID subjectId,
                                   @PathVariable UUID mcqId) {
        return mcqService.getMCQById(subjectId, mcqId);
    }

    @PutMapping("/{mcqId}")
    public MCQResponse updateMCQ(@PathVariable UUID subjectId,
                                  @PathVariable UUID mcqId,
                                  @RequestBody @Valid MCQUpdateRequest request) {
        return mcqService.updateMCQ(subjectId, mcqId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{mcqId}")
    public void deleteMCQ(@PathVariable UUID subjectId,
                          @PathVariable UUID mcqId) {
        mcqService.deleteMCQ(subjectId, mcqId);
    }
}
```

- [ ] **Step 10: Run all tests**

```bash
./mvnw test -q
```
Expected: BUILD SUCCESS, all tests passing

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/atinroy/recallr/mcq/ \
        src/test/java/com/atinroy/recallr/mcq/MCQServiceTest.java
git commit -m "feat(mcq): add subject/topic ownership, nest routes under /subjects/{subjectId}/mcqs"
```
