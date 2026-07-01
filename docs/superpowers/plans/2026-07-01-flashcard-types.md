# Flashcard Types Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add five new flashcard types — Cloze, True/False, Matching, Ordering, Short Answer — to the backend using the existing single-table inheritance pattern.

**Architecture:** Each type is a new `@Entity` subclass of `Flashcard` with a unique `@DiscriminatorValue`. Collection fields use `@ElementCollection` stored in separate tables. `FlashcardMapper` routes by `instanceof`. No new endpoints or service changes needed — the existing polymorphic CRUD handles all types.

**Tech Stack:** Java 21, Spring Boot 4.1, JPA/Hibernate, Flyway, Lombok, JUnit 5, Mockito, AssertJ

## Global Constraints

- All entity classes in `com.atinroy.recallr.domain.flashcard`
- All DTOs in `com.atinroy.recallr.domain.flashcard.dto`
- All tests in `com.atinroy.recallr.flashcard` (existing test package)
- Run tests from: `apps/api/` using `./mvnw test -Dtest=<TestClass> -q`
- Run all tests: `./mvnw test -q` from `apps/api/`
- Lombok: use `@Getter @Setter` (no constructors — JPA needs no-arg constructor)
- Follow existing pattern: `@Lob @Column(columnDefinition = "TEXT")` for long text fields

---

### Task 1: DB Migration

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V2__flashcard_new_types.sql`

**Interfaces:**
- Produces: Schema columns and tables consumed by all entity classes in Tasks 2–6

- [ ] **Step 1: Create the migration file**

```sql
ALTER TABLE flashcards ADD COLUMN correct_answer BOOLEAN;
ALTER TABLE flashcards ADD COLUMN model_answer TEXT;

CREATE TABLE flashcard_cloze_answers (
    flashcard_id UUID    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    answer       VARCHAR(500) NOT NULL,
    answer_order INT     NOT NULL
);

CREATE TABLE flashcard_matching_pairs (
    flashcard_id UUID    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    prompt       VARCHAR(500) NOT NULL,
    match_value  VARCHAR(500) NOT NULL,
    pair_order   INT     NOT NULL
);

CREATE TABLE flashcard_ordering_items (
    flashcard_id UUID    NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    item         TEXT    NOT NULL,
    item_order   INT     NOT NULL
);
```

- [ ] **Step 2: Start the app to verify Flyway applies migration cleanly**

```bash
cd apps/api && ./mvnw spring-boot:run -q 2>&1 | grep -E "(Flyway|ERROR|WARN)" | head -20
```

Expected: Flyway logs `Successfully applied 1 migration to schema "public"` (V2). No errors.

Stop the app with Ctrl+C after confirming.

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/resources/db/migration/V2__flashcard_new_types.sql
git commit -m "feat(db): add migration for new flashcard type columns and tables"
```

---

### Task 2: Cloze Flashcard

**Files:**
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/ClozeFlashcard.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ClozeFlashcardRequest.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ClozeFlashcardResponse.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ClozeFlashcardUpdateRequest.java`
- Create: `apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java`

**Interfaces:**
- Consumes: `Flashcard` base entity, `FlashcardRequest/Response/UpdateRequest` base DTOs, `FlashcardMapper`
- Produces: `ClozeFlashcard` entity, `ClozeFlashcard*` DTOs, mapper branches for CLOZE type

- [ ] **Step 1: Write failing mapper tests**

Create `apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java`:

```java
package com.atinroy.recallr.flashcard;

import com.atinroy.recallr.domain.deck.Deck;
import com.atinroy.recallr.domain.flashcard.*;
import com.atinroy.recallr.domain.flashcard.dto.*;
import com.atinroy.recallr.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FlashcardMapperTest {

    FlashcardMapper mapper = new FlashcardMapper();
    User user;
    Deck deck;

    @BeforeEach
    void setUp() {
        user = new User();
        deck = new Deck();
    }

    // ── Cloze ────────────────────────────────────────────────────────────────

    @Test
    void toEntity_cloze_mapsAllFields() {
        ClozeFlashcardRequest req = new ClozeFlashcardRequest();
        req.setQuestion("The {{sun}} is a {{star}}");
        req.setAnswers(List.of("sun", "star"));

        Flashcard result = mapper.toEntity(req, user, deck);

        assertThat(result).isInstanceOf(ClozeFlashcard.class);
        ClozeFlashcard card = (ClozeFlashcard) result;
        assertThat(card.getQuestion()).isEqualTo("The {{sun}} is a {{star}}");
        assertThat(card.getAnswers()).containsExactly("sun", "star");
        assertThat(card.getUser()).isEqualTo(user);
        assertThat(card.getDeck()).isEqualTo(deck);
    }

    @Test
    void toResponse_cloze_mapsAllFields() {
        ClozeFlashcard card = new ClozeFlashcard();
        card.setQuestion("The {{sun}} is a {{star}}");
        card.setAnswers(List.of("sun", "star"));
        card.setDeck(deck);

        FlashcardResponse result = mapper.toResponse(card);

        assertThat(result).isInstanceOf(ClozeFlashcardResponse.class);
        ClozeFlashcardResponse response = (ClozeFlashcardResponse) result;
        assertThat(response.getQuestion()).isEqualTo("The {{sun}} is a {{star}}");
        assertThat(response.getAnswers()).containsExactly("sun", "star");
    }

    @Test
    void applyUpdate_cloze_updatesFields() {
        ClozeFlashcard card = new ClozeFlashcard();
        card.setQuestion("Old question");
        card.setAnswers(List.of("old"));

        ClozeFlashcardUpdateRequest req = new ClozeFlashcardUpdateRequest();
        req.setQuestion("New {{question}}");
        req.setAnswers(List.of("question"));

        mapper.applyUpdate(req, card);

        assertThat(card.getQuestion()).isEqualTo("New {{question}}");
        assertThat(card.getAnswers()).containsExactly("question");
    }

    @Test
    void applyUpdate_cloze_withMCQCard_throwsBadRequestException() {
        MCQFlashcard card = new MCQFlashcard();
        ClozeFlashcardUpdateRequest req = new ClozeFlashcardUpdateRequest();
        req.setQuestion("q");
        req.setAnswers(List.of("a"));

        assertThatThrownBy(() -> mapper.applyUpdate(req, card))
                .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -20
```

Expected: Compilation error — `ClozeFlashcard`, `ClozeFlashcardRequest`, etc. do not exist yet.

- [ ] **Step 3: Create ClozeFlashcard entity**

```java
package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CLOZE")
@Getter
@Setter
public class ClozeFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_cloze_answers", joinColumns = @JoinColumn(name = "flashcard_id"))
    @Column(name = "answer")
    @OrderColumn(name = "answer_order")
    private List<String> answers = new ArrayList<>();
}
```

- [ ] **Step 4: Create DTOs**

`ClozeFlashcardRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClozeFlashcardRequest extends FlashcardRequest {
    private List<String> answers;
}
```

`ClozeFlashcardResponse.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClozeFlashcardResponse extends FlashcardResponse {
    private List<String> answers;
}
```

`ClozeFlashcardUpdateRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClozeFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private List<String> answers;
}
```

- [ ] **Step 5: Register CLOZE in @JsonSubTypes on the three base DTOs**

In `FlashcardRequest.java`, add to the `@JsonSubTypes` list:
```java
@JsonSubTypes.Type(value = ClozeFlashcardRequest.class, name = "CLOZE"),
```

In `FlashcardResponse.java`, add:
```java
@JsonSubTypes.Type(value = ClozeFlashcardResponse.class, name = "CLOZE"),
```

In `FlashcardUpdateRequest.java`, add:
```java
@JsonSubTypes.Type(value = ClozeFlashcardUpdateRequest.class, name = "CLOZE"),
```

- [ ] **Step 6: Add CLOZE branches to FlashcardMapper**

In `toEntity`, after the MCQ branch (before the final `throw`):
```java
} else if (request instanceof ClozeFlashcardRequest r) {
    ClozeFlashcard card = new ClozeFlashcard();
    card.setUser(user);
    card.setDeck(deck);
    card.setQuestion(r.getQuestion());
    card.setAnswers(r.getAnswers());
    return card;
}
```

In `toResponse`, after the MCQ branch (before the final `throw`):
```java
} else if (card instanceof ClozeFlashcard c) {
    ClozeFlashcardResponse r = new ClozeFlashcardResponse();
    mapBase(card, r);
    r.setAnswers(c.getAnswers());
    return r;
}
```

In `applyUpdate`, extend the type-mismatch guard to include Cloze:
```java
if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
    (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard)) ||
    (request instanceof ClozeFlashcardUpdateRequest && !(card instanceof ClozeFlashcard))) {
    throw new BadRequestException("Update request type does not match flashcard type");
}
```

Then add the Cloze branch after the existing MCQ branch:
```java
} else if (request instanceof ClozeFlashcardUpdateRequest r && card instanceof ClozeFlashcard c) {
    c.setAnswers(r.getAnswers());
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 4 tests pass.

- [ ] **Step 8: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/ClozeFlashcard.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ClozeFlashcard*.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java \
        apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java
git commit -m "feat(flashcard): add CLOZE flashcard type"
```

---

### Task 3: True/False Flashcard

**Files:**
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/TrueFalseFlashcard.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/TrueFalseFlashcardRequest.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/TrueFalseFlashcardResponse.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/TrueFalseFlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java`
- Modify: `apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java`

**Interfaces:**
- Consumes: `Flashcard` base entity, base DTOs, `FlashcardMapper`
- Produces: `TrueFalseFlashcard` entity with `correctAnswer` and `explanation` fields; mapper branches for TRUE_FALSE

- [ ] **Step 1: Add failing True/False tests to FlashcardMapperTest**

Append these test methods inside `FlashcardMapperTest`:

```java
// ── True/False ────────────────────────────────────────────────────────────

@Test
void toEntity_trueFalse_mapsAllFields() {
    TrueFalseFlashcardRequest req = new TrueFalseFlashcardRequest();
    req.setQuestion("The Earth is flat.");
    req.setCorrectAnswer(false);
    req.setExplanation("The Earth is an oblate spheroid.");

    Flashcard result = mapper.toEntity(req, user, deck);

    assertThat(result).isInstanceOf(TrueFalseFlashcard.class);
    TrueFalseFlashcard card = (TrueFalseFlashcard) result;
    assertThat(card.getQuestion()).isEqualTo("The Earth is flat.");
    assertThat(card.isCorrectAnswer()).isFalse();
    assertThat(card.getExplanation()).isEqualTo("The Earth is an oblate spheroid.");
}

@Test
void toResponse_trueFalse_mapsAllFields() {
    TrueFalseFlashcard card = new TrueFalseFlashcard();
    card.setQuestion("The Earth is flat.");
    card.setCorrectAnswer(false);
    card.setExplanation("The Earth is an oblate spheroid.");
    card.setDeck(deck);

    FlashcardResponse result = mapper.toResponse(card);

    assertThat(result).isInstanceOf(TrueFalseFlashcardResponse.class);
    TrueFalseFlashcardResponse response = (TrueFalseFlashcardResponse) result;
    assertThat(response.isCorrectAnswer()).isFalse();
    assertThat(response.getExplanation()).isEqualTo("The Earth is an oblate spheroid.");
}

@Test
void applyUpdate_trueFalse_updatesFields() {
    TrueFalseFlashcard card = new TrueFalseFlashcard();
    card.setCorrectAnswer(false);
    card.setExplanation("Old");

    TrueFalseFlashcardUpdateRequest req = new TrueFalseFlashcardUpdateRequest();
    req.setQuestion("Updated statement.");
    req.setCorrectAnswer(true);
    req.setExplanation("Updated explanation.");

    mapper.applyUpdate(req, card);

    assertThat(card.isCorrectAnswer()).isTrue();
    assertThat(card.getExplanation()).isEqualTo("Updated explanation.");
}

@Test
void applyUpdate_trueFalse_withClozeCard_throwsBadRequestException() {
    ClozeFlashcard card = new ClozeFlashcard();
    TrueFalseFlashcardUpdateRequest req = new TrueFalseFlashcardUpdateRequest();
    req.setQuestion("q");

    assertThatThrownBy(() -> mapper.applyUpdate(req, card))
            .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: Compilation error — `TrueFalseFlashcard`, `TrueFalseFlashcardRequest`, etc. do not exist yet.

- [ ] **Step 3: Create TrueFalseFlashcard entity**

```java
package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("TRUE_FALSE")
@Getter
@Setter
public class TrueFalseFlashcard extends Flashcard {

    @Column(name = "correct_answer")
    private boolean correctAnswer;

    @Column(length = 1000)
    private String explanation;
}
```

- [ ] **Step 4: Create DTOs**

`TrueFalseFlashcardRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrueFalseFlashcardRequest extends FlashcardRequest {
    private boolean correctAnswer;
    private String explanation;
}
```

`TrueFalseFlashcardResponse.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrueFalseFlashcardResponse extends FlashcardResponse {
    private boolean correctAnswer;
    private String explanation;
}
```

`TrueFalseFlashcardUpdateRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrueFalseFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private boolean correctAnswer;
    private String explanation;
}
```

- [ ] **Step 5: Register TRUE_FALSE in @JsonSubTypes on the three base DTOs**

In `FlashcardRequest.java`, add:
```java
@JsonSubTypes.Type(value = TrueFalseFlashcardRequest.class, name = "TRUE_FALSE"),
```

In `FlashcardResponse.java`, add:
```java
@JsonSubTypes.Type(value = TrueFalseFlashcardResponse.class, name = "TRUE_FALSE"),
```

In `FlashcardUpdateRequest.java`, add:
```java
@JsonSubTypes.Type(value = TrueFalseFlashcardUpdateRequest.class, name = "TRUE_FALSE"),
```

- [ ] **Step 6: Add TRUE_FALSE branches to FlashcardMapper**

In `toEntity`, after the CLOZE branch:
```java
} else if (request instanceof TrueFalseFlashcardRequest r) {
    TrueFalseFlashcard card = new TrueFalseFlashcard();
    card.setUser(user);
    card.setDeck(deck);
    card.setQuestion(r.getQuestion());
    card.setCorrectAnswer(r.isCorrectAnswer());
    card.setExplanation(r.getExplanation());
    return card;
}
```

In `toResponse`, after the CLOZE branch:
```java
} else if (card instanceof TrueFalseFlashcard c) {
    TrueFalseFlashcardResponse r = new TrueFalseFlashcardResponse();
    mapBase(card, r);
    r.setCorrectAnswer(c.isCorrectAnswer());
    r.setExplanation(c.getExplanation());
    return r;
}
```

In `applyUpdate`, extend the type-mismatch guard to include TrueFalse:
```java
if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
    (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard)) ||
    (request instanceof ClozeFlashcardUpdateRequest && !(card instanceof ClozeFlashcard)) ||
    (request instanceof TrueFalseFlashcardUpdateRequest && !(card instanceof TrueFalseFlashcard))) {
    throw new BadRequestException("Update request type does not match flashcard type");
}
```

Add the TrueFalse branch after the CLOZE branch:
```java
} else if (request instanceof TrueFalseFlashcardUpdateRequest r && card instanceof TrueFalseFlashcard c) {
    c.setCorrectAnswer(r.isCorrectAnswer());
    c.setExplanation(r.getExplanation());
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 8 tests pass.

- [ ] **Step 8: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/TrueFalseFlashcard.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/TrueFalseFlashcard*.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java \
        apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java
git commit -m "feat(flashcard): add TRUE_FALSE flashcard type"
```

---

### Task 4: Matching Flashcard

**Files:**
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/MatchingPair.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/MatchingFlashcard.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/MatchingPairDto.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/MatchingFlashcardRequest.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/MatchingFlashcardResponse.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/MatchingFlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java`
- Modify: `apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java`

**Interfaces:**
- Consumes: `Flashcard` base entity, base DTOs, `FlashcardMapper`
- Produces: `MatchingFlashcard` entity, `MatchingPair` embeddable, `MatchingPairDto` shared DTO, mapper branches for MATCHING

- [ ] **Step 1: Add failing Matching tests to FlashcardMapperTest**

Append these test methods:

```java
// ── Matching ──────────────────────────────────────────────────────────────

@Test
void toEntity_matching_mapsAllFields() {
    MatchingPairDto pair1 = new MatchingPairDto();
    pair1.setPrompt("France");
    pair1.setMatchValue("Paris");
    MatchingPairDto pair2 = new MatchingPairDto();
    pair2.setPrompt("Japan");
    pair2.setMatchValue("Tokyo");

    MatchingFlashcardRequest req = new MatchingFlashcardRequest();
    req.setQuestion("Match each country to its capital.");
    req.setPairs(List.of(pair1, pair2));

    Flashcard result = mapper.toEntity(req, user, deck);

    assertThat(result).isInstanceOf(MatchingFlashcard.class);
    MatchingFlashcard card = (MatchingFlashcard) result;
    assertThat(card.getQuestion()).isEqualTo("Match each country to its capital.");
    assertThat(card.getPairs()).hasSize(2);
    assertThat(card.getPairs().get(0).getPrompt()).isEqualTo("France");
    assertThat(card.getPairs().get(0).getMatchValue()).isEqualTo("Paris");
    assertThat(card.getPairs().get(1).getPrompt()).isEqualTo("Japan");
    assertThat(card.getPairs().get(1).getMatchValue()).isEqualTo("Tokyo");
}

@Test
void toResponse_matching_mapsAllFields() {
    MatchingPair pair = new MatchingPair();
    pair.setPrompt("France");
    pair.setMatchValue("Paris");

    MatchingFlashcard card = new MatchingFlashcard();
    card.setQuestion("Match capitals.");
    card.setPairs(List.of(pair));
    card.setDeck(deck);

    FlashcardResponse result = mapper.toResponse(card);

    assertThat(result).isInstanceOf(MatchingFlashcardResponse.class);
    MatchingFlashcardResponse response = (MatchingFlashcardResponse) result;
    assertThat(response.getPairs()).hasSize(1);
    assertThat(response.getPairs().get(0).getPrompt()).isEqualTo("France");
    assertThat(response.getPairs().get(0).getMatchValue()).isEqualTo("Paris");
}

@Test
void applyUpdate_matching_updatesFields() {
    MatchingFlashcard card = new MatchingFlashcard();
    card.setQuestion("Old question");

    MatchingPairDto newPair = new MatchingPairDto();
    newPair.setPrompt("Germany");
    newPair.setMatchValue("Berlin");

    MatchingFlashcardUpdateRequest req = new MatchingFlashcardUpdateRequest();
    req.setQuestion("New question");
    req.setPairs(List.of(newPair));

    mapper.applyUpdate(req, card);

    assertThat(card.getQuestion()).isEqualTo("New question");
    assertThat(card.getPairs()).hasSize(1);
    assertThat(card.getPairs().get(0).getPrompt()).isEqualTo("Germany");
}

@Test
void applyUpdate_matching_withTrueFalseCard_throwsBadRequestException() {
    TrueFalseFlashcard card = new TrueFalseFlashcard();
    MatchingFlashcardUpdateRequest req = new MatchingFlashcardUpdateRequest();
    req.setQuestion("q");
    req.setPairs(List.of());

    assertThatThrownBy(() -> mapper.applyUpdate(req, card))
            .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: Compilation error — `MatchingFlashcard`, `MatchingPair`, `MatchingPairDto`, etc. do not exist yet.

- [ ] **Step 3: Create MatchingPair embeddable**

```java
package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class MatchingPair {

    @Column(name = "prompt", length = 500)
    private String prompt;

    @Column(name = "match_value", length = 500)
    private String matchValue;
}
```

- [ ] **Step 4: Create MatchingFlashcard entity**

```java
package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("MATCHING")
@Getter
@Setter
public class MatchingFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_matching_pairs", joinColumns = @JoinColumn(name = "flashcard_id"))
    @OrderColumn(name = "pair_order")
    private List<MatchingPair> pairs = new ArrayList<>();
}
```

- [ ] **Step 5: Create DTOs**

`MatchingPairDto.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingPairDto {
    private String prompt;
    private String matchValue;
}
```

`MatchingFlashcardRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingFlashcardRequest extends FlashcardRequest {
    private List<MatchingPairDto> pairs;
}
```

`MatchingFlashcardResponse.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingFlashcardResponse extends FlashcardResponse {
    private List<MatchingPairDto> pairs;
}
```

`MatchingFlashcardUpdateRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private List<MatchingPairDto> pairs;
}
```

- [ ] **Step 6: Register MATCHING in @JsonSubTypes on the three base DTOs**

In `FlashcardRequest.java`, add:
```java
@JsonSubTypes.Type(value = MatchingFlashcardRequest.class, name = "MATCHING"),
```

In `FlashcardResponse.java`, add:
```java
@JsonSubTypes.Type(value = MatchingFlashcardResponse.class, name = "MATCHING"),
```

In `FlashcardUpdateRequest.java`, add:
```java
@JsonSubTypes.Type(value = MatchingFlashcardUpdateRequest.class, name = "MATCHING"),
```

- [ ] **Step 7: Add MATCHING branches to FlashcardMapper**

Add this import to `FlashcardMapper.java`:
```java
import java.util.stream.Collectors;
```

In `toEntity`, after the TRUE_FALSE branch:
```java
} else if (request instanceof MatchingFlashcardRequest r) {
    MatchingFlashcard card = new MatchingFlashcard();
    card.setUser(user);
    card.setDeck(deck);
    card.setQuestion(r.getQuestion());
    card.setPairs(r.getPairs().stream()
            .map(dto -> {
                MatchingPair p = new MatchingPair();
                p.setPrompt(dto.getPrompt());
                p.setMatchValue(dto.getMatchValue());
                return p;
            })
            .collect(Collectors.toList()));
    return card;
}
```

In `toResponse`, after the TRUE_FALSE branch:
```java
} else if (card instanceof MatchingFlashcard c) {
    MatchingFlashcardResponse r = new MatchingFlashcardResponse();
    mapBase(card, r);
    r.setPairs(c.getPairs().stream()
            .map(p -> {
                MatchingPairDto dto = new MatchingPairDto();
                dto.setPrompt(p.getPrompt());
                dto.setMatchValue(p.getMatchValue());
                return dto;
            })
            .collect(Collectors.toList()));
    return r;
}
```

In `applyUpdate`, extend the type-mismatch guard:
```java
if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
    (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard)) ||
    (request instanceof ClozeFlashcardUpdateRequest && !(card instanceof ClozeFlashcard)) ||
    (request instanceof TrueFalseFlashcardUpdateRequest && !(card instanceof TrueFalseFlashcard)) ||
    (request instanceof MatchingFlashcardUpdateRequest && !(card instanceof MatchingFlashcard))) {
    throw new BadRequestException("Update request type does not match flashcard type");
}
```

Add the MATCHING branch after the TRUE_FALSE branch:
```java
} else if (request instanceof MatchingFlashcardUpdateRequest r && card instanceof MatchingFlashcard c) {
    c.setPairs(r.getPairs().stream()
            .map(dto -> {
                MatchingPair p = new MatchingPair();
                p.setPrompt(dto.getPrompt());
                p.setMatchValue(dto.getMatchValue());
                return p;
            })
            .collect(Collectors.toList()));
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 12 tests pass.

- [ ] **Step 9: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/MatchingPair.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/MatchingFlashcard.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/MatchingPairDto.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/MatchingFlashcard*.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java \
        apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java
git commit -m "feat(flashcard): add MATCHING flashcard type"
```

---

### Task 5: Ordering Flashcard

**Files:**
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/OrderingFlashcard.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/OrderingFlashcardRequest.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/OrderingFlashcardResponse.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/OrderingFlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java`
- Modify: `apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java`

**Interfaces:**
- Consumes: `Flashcard` base entity, base DTOs, `FlashcardMapper`
- Produces: `OrderingFlashcard` entity with `items` list (correct order stored, client shuffles for display), mapper branches for ORDERING

- [ ] **Step 1: Add failing Ordering tests to FlashcardMapperTest**

Append these test methods:

```java
// ── Ordering ──────────────────────────────────────────────────────────────

@Test
void toEntity_ordering_mapsAllFields() {
    OrderingFlashcardRequest req = new OrderingFlashcardRequest();
    req.setQuestion("Arrange these events in chronological order.");
    req.setItems(List.of("World War I", "World War II", "Cold War"));

    Flashcard result = mapper.toEntity(req, user, deck);

    assertThat(result).isInstanceOf(OrderingFlashcard.class);
    OrderingFlashcard card = (OrderingFlashcard) result;
    assertThat(card.getQuestion()).isEqualTo("Arrange these events in chronological order.");
    assertThat(card.getItems()).containsExactly("World War I", "World War II", "Cold War");
}

@Test
void toResponse_ordering_mapsAllFields() {
    OrderingFlashcard card = new OrderingFlashcard();
    card.setQuestion("Arrange in order.");
    card.setItems(List.of("A", "B", "C"));
    card.setDeck(deck);

    FlashcardResponse result = mapper.toResponse(card);

    assertThat(result).isInstanceOf(OrderingFlashcardResponse.class);
    OrderingFlashcardResponse response = (OrderingFlashcardResponse) result;
    assertThat(response.getItems()).containsExactly("A", "B", "C");
}

@Test
void applyUpdate_ordering_updatesFields() {
    OrderingFlashcard card = new OrderingFlashcard();
    card.setItems(List.of("X", "Y"));

    OrderingFlashcardUpdateRequest req = new OrderingFlashcardUpdateRequest();
    req.setQuestion("New ordering question.");
    req.setItems(List.of("A", "B", "C"));

    mapper.applyUpdate(req, card);

    assertThat(card.getItems()).containsExactly("A", "B", "C");
}

@Test
void applyUpdate_ordering_withMatchingCard_throwsBadRequestException() {
    MatchingFlashcard card = new MatchingFlashcard();
    OrderingFlashcardUpdateRequest req = new OrderingFlashcardUpdateRequest();
    req.setQuestion("q");
    req.setItems(List.of());

    assertThatThrownBy(() -> mapper.applyUpdate(req, card))
            .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: Compilation error — `OrderingFlashcard`, `OrderingFlashcardRequest`, etc. do not exist yet.

- [ ] **Step 3: Create OrderingFlashcard entity**

```java
package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("ORDERING")
@Getter
@Setter
public class OrderingFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_ordering_items", joinColumns = @JoinColumn(name = "flashcard_id"))
    @Column(name = "item")
    @OrderColumn(name = "item_order")
    private List<String> items = new ArrayList<>();
}
```

- [ ] **Step 4: Create DTOs**

`OrderingFlashcardRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderingFlashcardRequest extends FlashcardRequest {
    private List<String> items;
}
```

`OrderingFlashcardResponse.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderingFlashcardResponse extends FlashcardResponse {
    private List<String> items;
}
```

`OrderingFlashcardUpdateRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderingFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private List<String> items;
}
```

- [ ] **Step 5: Register ORDERING in @JsonSubTypes on the three base DTOs**

In `FlashcardRequest.java`, add:
```java
@JsonSubTypes.Type(value = OrderingFlashcardRequest.class, name = "ORDERING"),
```

In `FlashcardResponse.java`, add:
```java
@JsonSubTypes.Type(value = OrderingFlashcardResponse.class, name = "ORDERING"),
```

In `FlashcardUpdateRequest.java`, add:
```java
@JsonSubTypes.Type(value = OrderingFlashcardUpdateRequest.class, name = "ORDERING"),
```

- [ ] **Step 6: Add ORDERING branches to FlashcardMapper**

In `toEntity`, after the MATCHING branch:
```java
} else if (request instanceof OrderingFlashcardRequest r) {
    OrderingFlashcard card = new OrderingFlashcard();
    card.setUser(user);
    card.setDeck(deck);
    card.setQuestion(r.getQuestion());
    card.setItems(r.getItems());
    return card;
}
```

In `toResponse`, after the MATCHING branch:
```java
} else if (card instanceof OrderingFlashcard c) {
    OrderingFlashcardResponse r = new OrderingFlashcardResponse();
    mapBase(card, r);
    r.setItems(c.getItems());
    return r;
}
```

In `applyUpdate`, extend the type-mismatch guard:
```java
if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
    (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard)) ||
    (request instanceof ClozeFlashcardUpdateRequest && !(card instanceof ClozeFlashcard)) ||
    (request instanceof TrueFalseFlashcardUpdateRequest && !(card instanceof TrueFalseFlashcard)) ||
    (request instanceof MatchingFlashcardUpdateRequest && !(card instanceof MatchingFlashcard)) ||
    (request instanceof OrderingFlashcardUpdateRequest && !(card instanceof OrderingFlashcard))) {
    throw new BadRequestException("Update request type does not match flashcard type");
}
```

Add the ORDERING branch after the MATCHING branch:
```java
} else if (request instanceof OrderingFlashcardUpdateRequest r && card instanceof OrderingFlashcard c) {
    c.setItems(r.getItems());
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 16 tests pass.

- [ ] **Step 8: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/OrderingFlashcard.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/OrderingFlashcard*.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java \
        apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java
git commit -m "feat(flashcard): add ORDERING flashcard type"
```

---

### Task 6: Short Answer Flashcard

**Files:**
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/ShortAnswerFlashcard.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ShortAnswerFlashcardRequest.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ShortAnswerFlashcardResponse.java`
- Create: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ShortAnswerFlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java`
- Modify: `apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java`

**Interfaces:**
- Consumes: `Flashcard` base entity, base DTOs, `FlashcardMapper`
- Produces: `ShortAnswerFlashcard` entity with `modelAnswer` field; mapper branches for SHORT_ANSWER

- [ ] **Step 1: Add failing Short Answer tests to FlashcardMapperTest**

Append these test methods:

```java
// ── Short Answer ──────────────────────────────────────────────────────────

@Test
void toEntity_shortAnswer_mapsAllFields() {
    ShortAnswerFlashcardRequest req = new ShortAnswerFlashcardRequest();
    req.setQuestion("What is photosynthesis?");
    req.setModelAnswer("The process by which plants convert sunlight into glucose.");

    Flashcard result = mapper.toEntity(req, user, deck);

    assertThat(result).isInstanceOf(ShortAnswerFlashcard.class);
    ShortAnswerFlashcard card = (ShortAnswerFlashcard) result;
    assertThat(card.getQuestion()).isEqualTo("What is photosynthesis?");
    assertThat(card.getModelAnswer()).isEqualTo("The process by which plants convert sunlight into glucose.");
}

@Test
void toResponse_shortAnswer_mapsAllFields() {
    ShortAnswerFlashcard card = new ShortAnswerFlashcard();
    card.setQuestion("What is photosynthesis?");
    card.setModelAnswer("The process by which plants convert sunlight into glucose.");
    card.setDeck(deck);

    FlashcardResponse result = mapper.toResponse(card);

    assertThat(result).isInstanceOf(ShortAnswerFlashcardResponse.class);
    ShortAnswerFlashcardResponse response = (ShortAnswerFlashcardResponse) result;
    assertThat(response.getModelAnswer()).isEqualTo("The process by which plants convert sunlight into glucose.");
}

@Test
void applyUpdate_shortAnswer_updatesFields() {
    ShortAnswerFlashcard card = new ShortAnswerFlashcard();
    card.setModelAnswer("Old answer.");

    ShortAnswerFlashcardUpdateRequest req = new ShortAnswerFlashcardUpdateRequest();
    req.setQuestion("Updated question.");
    req.setModelAnswer("Updated model answer.");

    mapper.applyUpdate(req, card);

    assertThat(card.getQuestion()).isEqualTo("Updated question.");
    assertThat(card.getModelAnswer()).isEqualTo("Updated model answer.");
}

@Test
void applyUpdate_shortAnswer_withOrderingCard_throwsBadRequestException() {
    OrderingFlashcard card = new OrderingFlashcard();
    ShortAnswerFlashcardUpdateRequest req = new ShortAnswerFlashcardUpdateRequest();
    req.setQuestion("q");
    req.setModelAnswer("a");

    assertThatThrownBy(() -> mapper.applyUpdate(req, card))
            .isInstanceOf(com.atinroy.recallr.common.BadRequestException.class);
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./mvnw test -Dtest=FlashcardMapperTest -q 2>&1 | tail -10
```

Expected: Compilation error — `ShortAnswerFlashcard`, `ShortAnswerFlashcardRequest`, etc. do not exist yet.

- [ ] **Step 3: Create ShortAnswerFlashcard entity**

```java
package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("SHORT_ANSWER")
@Getter
@Setter
public class ShortAnswerFlashcard extends Flashcard {

    @Lob
    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;
}
```

- [ ] **Step 4: Create DTOs**

`ShortAnswerFlashcardRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortAnswerFlashcardRequest extends FlashcardRequest {
    private String modelAnswer;
}
```

`ShortAnswerFlashcardResponse.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortAnswerFlashcardResponse extends FlashcardResponse {
    private String modelAnswer;
}
```

`ShortAnswerFlashcardUpdateRequest.java`:
```java
package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortAnswerFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private String modelAnswer;
}
```

- [ ] **Step 5: Register SHORT_ANSWER in @JsonSubTypes on the three base DTOs**

In `FlashcardRequest.java`, add:
```java
@JsonSubTypes.Type(value = ShortAnswerFlashcardRequest.class, name = "SHORT_ANSWER"),
```

In `FlashcardResponse.java`, add:
```java
@JsonSubTypes.Type(value = ShortAnswerFlashcardResponse.class, name = "SHORT_ANSWER"),
```

In `FlashcardUpdateRequest.java`, add:
```java
@JsonSubTypes.Type(value = ShortAnswerFlashcardUpdateRequest.class, name = "SHORT_ANSWER"),
```

- [ ] **Step 6: Add SHORT_ANSWER branches to FlashcardMapper**

In `toEntity`, after the ORDERING branch:
```java
} else if (request instanceof ShortAnswerFlashcardRequest r) {
    ShortAnswerFlashcard card = new ShortAnswerFlashcard();
    card.setUser(user);
    card.setDeck(deck);
    card.setQuestion(r.getQuestion());
    card.setModelAnswer(r.getModelAnswer());
    return card;
}
```

In `toResponse`, after the ORDERING branch:
```java
} else if (card instanceof ShortAnswerFlashcard c) {
    ShortAnswerFlashcardResponse r = new ShortAnswerFlashcardResponse();
    mapBase(card, r);
    r.setModelAnswer(c.getModelAnswer());
    return r;
}
```

In `applyUpdate`, extend the type-mismatch guard to its final form:
```java
if ((request instanceof BasicFlashcardUpdateRequest && !(card instanceof BasicFlashcard)) ||
    (request instanceof MCQFlashcardUpdateRequest && !(card instanceof MCQFlashcard)) ||
    (request instanceof ClozeFlashcardUpdateRequest && !(card instanceof ClozeFlashcard)) ||
    (request instanceof TrueFalseFlashcardUpdateRequest && !(card instanceof TrueFalseFlashcard)) ||
    (request instanceof MatchingFlashcardUpdateRequest && !(card instanceof MatchingFlashcard)) ||
    (request instanceof OrderingFlashcardUpdateRequest && !(card instanceof OrderingFlashcard)) ||
    (request instanceof ShortAnswerFlashcardUpdateRequest && !(card instanceof ShortAnswerFlashcard))) {
    throw new BadRequestException("Update request type does not match flashcard type");
}
```

Add the SHORT_ANSWER branch after the ORDERING branch:
```java
} else if (request instanceof ShortAnswerFlashcardUpdateRequest r && card instanceof ShortAnswerFlashcard c) {
    c.setModelAnswer(r.getModelAnswer());
}
```

- [ ] **Step 7: Run all tests to verify everything passes**

```bash
cd apps/api && ./mvnw test -q 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`. All tests pass, including `FlashcardServiceTest` (existing) and `FlashcardMapperTest` (20 tests).

- [ ] **Step 8: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/ShortAnswerFlashcard.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/ShortAnswerFlashcard*.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardResponse.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/dto/FlashcardUpdateRequest.java \
        apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardMapper.java \
        apps/api/src/test/java/com/atinroy/recallr/flashcard/FlashcardMapperTest.java
git commit -m "feat(flashcard): add SHORT_ANSWER flashcard type"
```
