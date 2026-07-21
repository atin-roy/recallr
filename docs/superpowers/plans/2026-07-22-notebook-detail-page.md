# Notebook Detail Page (Decks) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user open a notebook and manage the decks inside it — list, create, edit, delete — with decks rendered as physical index-card stacks.

**Architecture:** Backend adds a flashcard count to `DeckResponse` (one grouped query for lists, a derived count for single decks). Frontend adds a dynamic route `notebooks/[notebookId]`, a presentational `DeckStack`, a shared create/edit `DeckForm`, and five API client functions. The page owns all data and handlers; components are presentational.

**Tech Stack:** Spring Boot 4 (JPA, JUnit 5 + Mockito + AssertJ), Next.js 16 (App Router, client components), React 19, TypeScript, CSS Modules.

## Global Constraints

- **Styling:** CSS Modules + design tokens only. No Tailwind, no inline `style` objects, no utility classes in JSX. Reuse tokens from `apps/web/app/tokens.css` (`--border-card`, `--shadow-hard`, `--radius-card`, palette vars); never hardcode hex.
- **Aesthetic:** Paper Stack — warm paper, 3px ink borders, hard offset shadows, physical tilted stacks. Must not read as generic SaaS. Lean into the stacked-card silhouette on decks (a deck IS a stack).
- **Next.js 16:** `params` is a `Promise`. In client components read it with React's `use()` — never destructure synchronously. Confirmed in `apps/web/node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/dynamic-routes.md`.
- **Auth model:** access token lives in memory (`lib/accessToken.ts`); every data-fetching page is a client component. `apiFetch` already handles 401 refresh + retry.
- **Deck validation:** name required, max 200 chars; description optional. Matches `DeckRequest`/`DeckUpdateRequest`.
- **Frontend has no test framework** (only `dev`/`build`/`start`/`lint`). Frontend verification = `npm run build` + `npm run lint` from `apps/web`. Do not add a test runner.
- **Commit trailer:** do NOT add a `Co-Authored-By: Claude` trailer.
- **API base:** all frontend calls go through `apiFetch` in `lib/api.ts`; paths are relative to `NEXT_PUBLIC_API_URL`.

---

## Task 1: Flashcard count queries on `FlashcardRepository`

**Files:**
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardRepository.java`
- Test: covered indirectly via Task 2's `DeckServiceTest` (repository is a Spring Data interface with no logic of its own; the grouped-query behavior is asserted through the service). No standalone test here.

**Interfaces:**
- Produces:
  - `long countByDeckId(UUID deckId)` — derived count for a single deck.
  - `List<FlashcardRepository.DeckCardCount> countByDeckIds(Collection<UUID> deckIds)` — grouped count; decks with zero cards are absent from the result.
  - nested projection `interface DeckCardCount { UUID getDeckId(); long getCount(); }`

- [ ] **Step 1: Add the count methods and projection**

Replace the file body with:

```java
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
```

- [ ] **Step 2: Verify it compiles**

Run: `cd apps/api && ./mvnw -q compile`
Expected: BUILD SUCCESS (no test run yet; behavior is exercised in Task 2).

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/flashcard/FlashcardRepository.java
git commit -m "feat(api): flashcard count queries (single + grouped)"
```

---

## Task 2: Add `cardCount` to `DeckResponse` and wire it through the service

This is one atomic task: adding the field breaks every `new DeckResponse(...)` and `deckMapper.toResponse(...)` call site, so the DTO, mapper, service, and existing tests must change together to compile.

**Files:**
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/deck/dto/DeckResponse.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/deck/DeckMapper.java`
- Modify: `apps/api/src/main/java/com/atinroy/recallr/domain/deck/DeckService.java`
- Test: `apps/api/src/test/java/com/atinroy/recallr/deck/DeckServiceTest.java`

**Interfaces:**
- Consumes: `FlashcardRepository.countByDeckId`, `FlashcardRepository.countByDeckIds`, `FlashcardRepository.DeckCardCount` (Task 1).
- Produces:
  - `DeckResponse(UUID id, UUID notebookId, String name, String description, long cardCount, Instant createdAt, Instant updatedAt)` — `cardCount` inserted before the timestamps.
  - `DeckMapper.toResponse(Deck deck, long cardCount)` — count is now a required parameter.

- [ ] **Step 1: Confirm the mapper's only caller is the service**

Run: `grep -rn "toResponse\|new DeckResponse" apps/api/src/main apps/api/src/test`
Expected: matches only in `DeckMapper.java`, `DeckService.java`, and `DeckServiceTest.java`. If any other file appears, it must be updated the same way in this task.

- [ ] **Step 2: Add the field to `DeckResponse`**

```java
package com.atinroy.recallr.domain.deck.dto;

import java.time.Instant;
import java.util.UUID;

public record DeckResponse(
        UUID id,
        UUID notebookId,
        String name,
        String description,
        long cardCount,
        Instant createdAt,
        Instant updatedAt
) {}
```

- [ ] **Step 3: Update `DeckMapper.toResponse` to take the count**

Replace the `toResponse` method (leave `toEntity` unchanged):

```java
    public DeckResponse toResponse(Deck deck, long cardCount) {
        return new DeckResponse(
                deck.getId(),
                deck.getNotebook() != null ? deck.getNotebook().getId() : null,
                deck.getName(),
                deck.getDescription(),
                cardCount,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
```

- [ ] **Step 4: Wire counts through `DeckService`**

Add the flashcard repository dependency and supply counts at every call site. The full updated file:

```java
package com.atinroy.recallr.domain.deck;

import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.deck.dto.DeckUpdateRequest;
import com.atinroy.recallr.domain.flashcard.FlashcardRepository;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookRepository;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckMapper deckMapper;
    private final NotebookRepository notebookRepository;
    private final FlashcardRepository flashcardRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public DeckResponse createInNotebook(UUID notebookId, DeckRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Notebook notebook = notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotebookNotFoundException(notebookId));
        User user = notebook.getUser();
        Deck saved = deckRepository.save(deckMapper.toEntity(request, user, notebook));
        return deckMapper.toResponse(saved, 0L);
    }

    @Transactional
    public DeckResponse createStandalone(DeckRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Deck saved = deckRepository.save(deckMapper.toEntity(request, user, null));
        return deckMapper.toResponse(saved, 0L);
    }

    public DeckResponse getDeckById(UUID deckId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = resolveDeck(deckId, userId);
        return deckMapper.toResponse(deck, flashcardRepository.countByDeckId(deckId));
    }

    @Transactional
    public DeckResponse updateDeck(UUID deckId, DeckUpdateRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = resolveDeck(deckId, userId);
        deck.setName(request.name());
        deck.setDescription(request.description());
        return deckMapper.toResponse(deck, flashcardRepository.countByDeckId(deckId));
    }

    @Transactional
    public void deleteDeck(UUID deckId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        Deck deck = resolveDeck(deckId, userId);
        deckRepository.delete(deck);
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> listByUser() {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        return withCounts(deckRepository.findByUserId(userId));
    }

    public List<DeckResponse> listByNotebook(UUID notebookId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().getId();
        notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotebookNotFoundException(notebookId));
        return withCounts(deckRepository.findByNotebookId(notebookId));
    }

    private List<DeckResponse> withCounts(List<Deck> decks) {
        if (decks.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = decks.stream().map(Deck::getId).toList();
        Map<UUID, Long> counts = flashcardRepository.countByDeckIds(ids).stream()
                .collect(Collectors.toMap(
                        FlashcardRepository.DeckCardCount::getDeckId,
                        FlashcardRepository.DeckCardCount::getCount));
        return decks.stream()
                .map(deck -> deckMapper.toResponse(deck, counts.getOrDefault(deck.getId(), 0L)))
                .toList();
    }

    private Deck resolveDeck(UUID deckId, UUID userId) {
        return deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
    }
}
```

- [ ] **Step 5: Update `DeckServiceTest` — fix existing tests and add count tests**

Replace the whole file. Every `new DeckResponse(...)` gains a `cardCount` arg (7 fields), every `toResponse` stub takes the count, `flashcardRepository` is mocked, and four new count-focused tests are added. Note `deck.getId()` is null for a `new Deck()` (id is assigned on persist), so single-deck count stubs use `anyOrNull()`-safe matching by stubbing `countByDeckId(deck.getId())` directly with that null id — Mockito matches null equality fine.

```java
package com.atinroy.recallr.deck;

import com.atinroy.recallr.domain.deck.*;
import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.deck.dto.DeckUpdateRequest;
import com.atinroy.recallr.domain.flashcard.FlashcardRepository;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookRepository;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock DeckRepository deckRepository;
    @Mock DeckMapper deckMapper;
    @Mock NotebookRepository notebookRepository;
    @Mock FlashcardRepository flashcardRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks DeckService deckService;

    private User user;
    private Notebook notebook;
    private Deck deck;

    @BeforeEach
    void setUp() {
        user = new User();
        notebook = new Notebook();
        notebook.setUser(user);
        deck = new Deck();
    }

    @Test
    void createInNotebook_whenNotebookFound_returnsDeckResponseWithZeroCount() {
        DeckRequest request = new DeckRequest("My Deck", "A description");
        DeckResponse expected = new DeckResponse(deck.getId(), notebook.getId(), "My Deck", "A description", 0L, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), user.getId())).thenReturn(Optional.of(notebook));
        when(deckMapper.toEntity(request, user, notebook)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck, 0L)).thenReturn(expected);

        DeckResponse result = deckService.createInNotebook(notebook.getId(), request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void createInNotebook_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.createInNotebook(notebookId, new DeckRequest("Deck", null)))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void createStandalone_returnsDeckResponseWithZeroCount() {
        DeckRequest request = new DeckRequest("Standalone Deck", null);
        DeckResponse expected = new DeckResponse(deck.getId(), null, "Standalone Deck", null, 0L, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckMapper.toEntity(request, user, null)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck, 0L)).thenReturn(expected);

        DeckResponse result = deckService.createStandalone(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getDeckById_whenFound_returnsResponseWithCount() {
        DeckResponse expected = new DeckResponse(deck.getId(), null, "My Deck", null, 5L, null, null);
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.countByDeckId(deck.getId())).thenReturn(5L);
        when(deckMapper.toResponse(deck, 5L)).thenReturn(expected);

        assertThat(deckService.getDeckById(deck.getId())).isEqualTo(expected);
    }

    @Test
    void getDeckById_whenNotFound_throwsDeckNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeckById(id))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void updateDeck_whenFound_updatesFieldsAndReturnsResponseWithCount() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.countByDeckId(deck.getId())).thenReturn(3L);
        when(deckMapper.toResponse(deck, 3L))
                .thenReturn(new DeckResponse(deck.getId(), null, "New Name", "New Desc", 3L, null, null));

        deckService.updateDeck(deck.getId(), new DeckUpdateRequest("New Name", "New Desc"));

        assertThat(deck.getName()).isEqualTo("New Name");
        assertThat(deck.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void updateDeck_whenNotFound_throwsDeckNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.updateDeck(id, new DeckUpdateRequest("X", null)))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void deleteDeck_whenFound_deletesDeck() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(deck.getId(), user.getId())).thenReturn(Optional.of(deck));

        deckService.deleteDeck(deck.getId());

        verify(deckRepository).delete(deck);
    }

    @Test
    void deleteDeck_whenNotFound_throwsDeckNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.deleteDeck(id))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void listByNotebook_mapsCountsOntoMatchingDecks() {
        UUID userId = user.getId();
        Deck deckA = new Deck();
        Deck deckB = new Deck();
        DeckCardCountStub countA = new DeckCardCountStub(deckA.getId(), 7L);
        // deckB deliberately absent from the grouped result -> should default to 0
        DeckResponse respA = new DeckResponse(deckA.getId(), null, "A", null, 7L, null, null);
        DeckResponse respB = new DeckResponse(deckB.getId(), null, "B", null, 0L, null, null);

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), userId)).thenReturn(Optional.of(notebook));
        when(deckRepository.findByNotebookId(notebook.getId())).thenReturn(List.of(deckA, deckB));
        when(flashcardRepository.countByDeckIds(anyCollection())).thenReturn(List.of(countA));
        when(deckMapper.toResponse(deckA, 7L)).thenReturn(respA);
        when(deckMapper.toResponse(deckB, 0L)).thenReturn(respB);

        List<DeckResponse> result = deckService.listByNotebook(notebook.getId());

        assertThat(result).containsExactly(respA, respB);
    }

    @Test
    void listByNotebook_whenNoDecks_returnsEmptyWithoutQueryingCounts() {
        UUID userId = user.getId();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebook.getId(), userId)).thenReturn(Optional.of(notebook));
        when(deckRepository.findByNotebookId(notebook.getId())).thenReturn(List.of());

        List<DeckResponse> result = deckService.listByNotebook(notebook.getId());

        assertThat(result).isEmpty();
        verify(flashcardRepository, never()).countByDeckIds(anyCollection());
    }

    @Test
    void listByNotebook_whenNotebookNotFound_throwsNotebookNotFoundException() {
        UUID notebookId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(notebookRepository.findByIdAndUserId(notebookId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.listByNotebook(notebookId))
                .isInstanceOf(NotebookNotFoundException.class);
    }

    @Test
    void listByUser_returnsAllUserDecksWithCounts() {
        UUID userId = user.getId();
        Deck deck1 = new Deck();
        Deck deck2 = new Deck();
        DeckResponse resp1 = new DeckResponse(deck1.getId(), null, "Deck 1", null, 0L, null, null);
        DeckResponse resp2 = new DeckResponse(deck2.getId(), null, "Deck 2", null, 0L, null, null);

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(deckRepository.findByUserId(userId)).thenReturn(List.of(deck1, deck2));
        when(flashcardRepository.countByDeckIds(anyCollection())).thenReturn(List.of());
        when(deckMapper.toResponse(deck1, 0L)).thenReturn(resp1);
        when(deckMapper.toResponse(deck2, 0L)).thenReturn(resp2);

        List<DeckResponse> result = deckService.listByUser();

        assertThat(result).containsExactly(resp1, resp2);
    }

    /** Minimal stand-in for the interface-based projection FlashcardRepository.DeckCardCount. */
    private record DeckCardCountStub(UUID deckId, long count) implements FlashcardRepository.DeckCardCount {
        @Override public UUID getDeckId() { return deckId; }
        @Override public long getCount() { return count; }
    }
}
```

- [ ] **Step 6: Run the service tests — verify they pass**

Run: `cd apps/api && ./mvnw -q -Dtest=DeckServiceTest test`
Expected: PASS, all tests green.

- [ ] **Step 7: Full backend build to catch any other broken call sites**

Run: `cd apps/api && ./mvnw -q test`
Expected: BUILD SUCCESS. If a controller/integration test constructs `DeckResponse` directly, update it to the 7-field form (the `grep` in Step 1 should have surfaced it).

- [ ] **Step 8: Commit**

```bash
git add apps/api/src/main/java/com/atinroy/recallr/domain/deck apps/api/src/test/java/com/atinroy/recallr/deck/DeckServiceTest.java
git commit -m "feat(api): add cardCount to DeckResponse"
```

---

## Task 3: Deck API client (`lib/api.ts`)

**Files:**
- Modify: `apps/web/lib/api.ts`

**Interfaces:**
- Consumes: existing `apiFetch<T>`, existing `Notebook` type.
- Produces:
  - `type Deck = { id: string; notebookId: string | null; name: string; description?: string | null; cardCount: number; createdAt: string; updatedAt: string }`
  - `getNotebook(id: string): Promise<Notebook>`
  - `getDecksByNotebook(notebookId: string): Promise<Deck[]>`
  - `createDeck(notebookId: string, input: { name: string; description?: string }): Promise<Deck>`
  - `updateDeck(deckId: string, input: { name: string; description?: string }): Promise<Deck>`
  - `deleteDeck(deckId: string): Promise<void>`

- [ ] **Step 1: Add the `Deck` type and functions**

Append to `apps/web/lib/api.ts` (after the existing `createNotebook` function, before end of file):

```ts
export type Deck = {
  id: string;
  notebookId: string | null;
  name: string;
  description?: string | null;
  cardCount: number;
  createdAt: string;
  updatedAt: string;
};

export function getNotebook(id: string) {
  return apiFetch<Notebook>(`/notebooks/${id}`);
}

export function getDecksByNotebook(notebookId: string) {
  return apiFetch<Deck[]>(`/notebooks/${notebookId}/decks`);
}

export function createDeck(
  notebookId: string,
  input: { name: string; description?: string },
) {
  return apiFetch<Deck>(`/notebooks/${notebookId}/decks`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateDeck(
  deckId: string,
  input: { name: string; description?: string },
) {
  return apiFetch<Deck>(`/decks/${deckId}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteDeck(deckId: string) {
  return apiFetch<void>(`/decks/${deckId}`, { method: "DELETE" });
}
```

- [ ] **Step 2: Typecheck**

Run: `cd apps/web && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add apps/web/lib/api.ts
git commit -m "feat(web): deck API client functions + Deck type"
```

---

## Task 4: `DeckStack` component

A presentational stacked-card for one deck, with an inline action row (edit + two-step delete confirm). Stack depth reflects `cardCount`; layer colors derive deterministically from the deck id.

**Files:**
- Create: `apps/web/components/decks/DeckStack.tsx`
- Create: `apps/web/components/decks/DeckStack.module.css`

**Interfaces:**
- Consumes: `Deck` type (Task 3).
- Produces: `DeckStack` component.

```ts
type DeckStackProps = {
  deck: Deck;
  onEdit: (deck: Deck) => void;
  onDelete: (deck: Deck) => void;   // called only after the user confirms
  deleting?: boolean;
  deleteError?: string | null;
};
```

- [ ] **Step 1: Write the component**

Create `apps/web/components/decks/DeckStack.tsx`:

```tsx
"use client";

import { useState } from "react";
import type { Deck } from "@/lib/api";
import styles from "./DeckStack.module.css";

const PALETTE = [
  "lavender",
  "teal",
  "mint",
  "sky",
  "rose",
  "peach",
  "yellow",
  "coral",
] as const;

const dateFormat = new Intl.DateTimeFormat("en-GB", {
  day: "numeric",
  month: "short",
  year: "numeric",
});

// Deterministic index from the deck id so a deck's colors are stable and differ
// between decks, without storing a color on the backend.
function paletteIndex(id: string): number {
  let sum = 0;
  for (let i = 0; i < id.length; i += 1) sum += id.charCodeAt(i);
  return sum % PALETTE.length;
}

type DeckStackProps = {
  deck: Deck;
  onEdit: (deck: Deck) => void;
  onDelete: (deck: Deck) => void;
  deleting?: boolean;
  deleteError?: string | null;
};

export function DeckStack({
  deck,
  onEdit,
  onDelete,
  deleting = false,
  deleteError = null,
}: DeckStackProps) {
  const [confirming, setConfirming] = useState(false);

  const layers = deck.cardCount >= 10 ? 2 : deck.cardCount >= 1 ? 1 : 0;
  const base = paletteIndex(deck.id);
  const layer1Color = PALETTE[base];
  const layer2Color = PALETTE[(base + 1) % PALETTE.length];

  return (
    <div className={styles.stackWrap}>
      <div className={styles.stack}>
        {layers >= 2 && (
          <span
            className={`${styles.layer} ${styles.layer2} ${styles[layer2Color]}`}
            aria-hidden="true"
          />
        )}
        {layers >= 1 && (
          <span
            className={`${styles.layer} ${styles.layer1} ${styles[layer1Color]}`}
            aria-hidden="true"
          />
        )}
        <div className={styles.top}>
          <p className={styles.meta}>
            Updated {dateFormat.format(new Date(deck.updatedAt))}
          </p>
          <div className={styles.name}>{deck.name}</div>
          {deck.description && <p className={styles.desc}>{deck.description}</p>}
          <p className={styles.countLine}>
            <span className={styles.countPill}>
              {deck.cardCount} card{deck.cardCount === 1 ? "" : "s"}
            </span>
          </p>

          {deleteError && (
            <p className={styles.error} role="alert">
              {deleteError}
            </p>
          )}

          <div className={styles.actions}>
            {confirming ? (
              <>
                <button
                  type="button"
                  className={styles.dangerAction}
                  onClick={() => onDelete(deck)}
                  disabled={deleting}
                >
                  {deleting ? "Deleting…" : "Really delete?"}
                </button>
                <button
                  type="button"
                  className={styles.action}
                  onClick={() => setConfirming(false)}
                  disabled={deleting}
                >
                  Cancel
                </button>
              </>
            ) : (
              <>
                <button
                  type="button"
                  className={styles.action}
                  onClick={() => onEdit(deck)}
                >
                  Edit
                </button>
                <button
                  type="button"
                  className={styles.action}
                  onClick={() => setConfirming(true)}
                >
                  Delete
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Write the styles**

Create `apps/web/components/decks/DeckStack.module.css`:

```css
.stackWrap {
  padding: var(--space-3) var(--space-2) var(--space-4);
}
.stack {
  position: relative;
  min-height: 180px;
}
.layer {
  position: absolute;
  inset: 0;
  border: var(--border-card);
  border-radius: var(--radius-card);
}
.layer1 {
  transform: rotate(3deg) translate(5px, 4px);
}
.layer2 {
  transform: rotate(-5deg) translate(-7px, 7px);
}
.top {
  position: relative;
  background: var(--cream);
  border: var(--border-card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-hard);
  padding: var(--space-6);
}
.meta {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--color-text-soft);
  margin-bottom: var(--space-3);
}
.name {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.desc {
  margin-top: var(--space-2);
  color: var(--color-text-muted);
  font-size: 0.95rem;
  overflow: hidden;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.countLine {
  margin-top: var(--space-4);
}
.countPill {
  display: inline-block;
  border: var(--border-thin);
  border-radius: var(--radius-pill);
  padding: 2px var(--space-3);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 700;
  background: var(--paper);
}
.error {
  margin-top: var(--space-3);
  color: var(--coral);
  font-weight: 600;
  font-size: 0.85rem;
}
.actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
.action,
.dangerAction {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  background: none;
  border: 0;
  padding: 0;
  cursor: pointer;
  color: var(--color-text-muted);
}
.action:hover {
  color: var(--ink);
}
.dangerAction {
  color: var(--coral);
}
.dangerAction:disabled,
.action:disabled {
  opacity: 0.6;
  cursor: default;
}

/* palette layer fills */
.lavender { background: var(--lavender); }
.teal { background: var(--teal); }
.mint { background: var(--mint); }
.sky { background: var(--sky); }
.rose { background: var(--rose); }
.peach { background: var(--peach); }
.yellow { background: var(--yellow); }
.coral { background: var(--coral); }
```

- [ ] **Step 3: Typecheck + lint**

Run: `cd apps/web && npx tsc --noEmit && npm run lint`
Expected: no errors (component is not yet imported anywhere — that is fine).

- [ ] **Step 4: Commit**

```bash
git add apps/web/components/decks/DeckStack.tsx apps/web/components/decks/DeckStack.module.css
git commit -m "feat(web): DeckStack component (physical stack + inline actions)"
```

---

## Task 5: `DeckForm` component

One form for create and edit, switched by `mode`. Mirrors the dashboard's create-form markup and voice.

**Files:**
- Create: `apps/web/components/decks/DeckForm.tsx`
- Create: `apps/web/components/decks/DeckForm.module.css`

**Interfaces:**
- Consumes: existing `Button`, `TextField`, `PaperCard` UI components; `Deck` type.
- Produces: `DeckForm` component.

```ts
type DeckFormProps = {
  mode: "create" | "edit";
  initial?: { name: string; description: string };
  saving?: boolean;
  error?: string | null;
  onSubmit: (values: { name: string; description: string }) => void;
  onCancel: () => void;
};
```

- [ ] **Step 1: Write the component**

Create `apps/web/components/decks/DeckForm.tsx`:

```tsx
"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { PaperCard } from "@/components/ui/PaperCard";
import { TextField } from "@/components/ui/TextField";
import styles from "./DeckForm.module.css";

type DeckFormProps = {
  mode: "create" | "edit";
  initial?: { name: string; description: string };
  saving?: boolean;
  error?: string | null;
  onSubmit: (values: { name: string; description: string }) => void;
  onCancel: () => void;
};

export function DeckForm({
  mode,
  initial,
  saving = false,
  error = null,
  onSubmit,
  onCancel,
}: DeckFormProps) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");

  const heading = mode === "create" ? "New deck" : "Edit deck";
  const submitLabel = mode === "create" ? "Create deck" : "Save changes";
  const savingLabel = mode === "create" ? "Filing…" : "Saving…";

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    onSubmit({ name: name.trim(), description: description.trim() });
  }

  return (
    <PaperCard className={styles.formCard}>
      <form onSubmit={handleSubmit}>
        <p className={styles.formLabel}>{heading}</p>
        <div className={styles.formFields}>
          <TextField
            id="deck-name"
            label="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            maxLength={200}
            autoFocus
          />
          <TextField
            id="deck-description"
            label="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
        {error && (
          <p className={styles.formError} role="alert">
            {error}
          </p>
        )}
        <div className={styles.formActions}>
          <Button type="submit" disabled={saving || !name.trim()}>
            {saving ? savingLabel : submitLabel}
          </Button>
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        </div>
      </form>
    </PaperCard>
  );
}
```

- [ ] **Step 2: Write the styles**

Create `apps/web/components/decks/DeckForm.module.css`:

```css
.formCard {
  padding: var(--space-6);
  margin-bottom: var(--space-6);
  max-width: 480px;
}
.formLabel {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: var(--color-text-muted);
  margin-bottom: var(--space-4);
}
.formFields {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.formError {
  margin-top: var(--space-3);
  color: var(--coral);
  font-weight: 600;
  font-size: 0.9rem;
}
.formActions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-6);
}
```

- [ ] **Step 3: Typecheck + lint**

Run: `cd apps/web && npx tsc --noEmit && npm run lint`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add apps/web/components/decks/DeckForm.tsx apps/web/components/decks/DeckForm.module.css
git commit -m "feat(web): DeckForm component (shared create/edit)"
```

---

## Task 6: Notebook detail page + dashboard cards become links

Wires everything: the dynamic route, its page state/handlers, its CSS, and turning dashboard notebook cards into links to it.

**Files:**
- Create: `apps/web/app/(app)/notebooks/[notebookId]/page.tsx`
- Create: `apps/web/app/(app)/notebooks/[notebookId]/page.module.css`
- Modify: `apps/web/app/(app)/dashboard/page.tsx` (wrap notebook cards in `Link`)

**Interfaces:**
- Consumes: `getNotebook`, `getDecksByNotebook`, `createDeck`, `updateDeck`, `deleteDeck`, `Deck`, `Notebook` (Task 3); `DeckStack` (Task 4); `DeckForm` (Task 5); existing `Button`, `PaperCard`, `LogoMark`.

- [ ] **Step 1: Write the page**

Create `apps/web/app/(app)/notebooks/[notebookId]/page.tsx`:

```tsx
"use client";

import { use, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { PaperCard } from "@/components/ui/PaperCard";
import { LogoMark } from "@/components/LogoMark";
import { DeckStack } from "@/components/decks/DeckStack";
import { DeckForm } from "@/components/decks/DeckForm";
import {
  getNotebook,
  getDecksByNotebook,
  createDeck,
  updateDeck,
  deleteDeck,
  type Deck,
  type Notebook,
} from "@/lib/api";
import styles from "./page.module.css";

type Status = "loading" | "ready" | "error" | "notFound";
type FormState =
  | { kind: "closed" }
  | { kind: "create" }
  | { kind: "edit"; deck: Deck };

export default function NotebookPage({
  params,
}: {
  params: Promise<{ notebookId: string }>;
}) {
  const { notebookId } = use(params);

  const [notebook, setNotebook] = useState<Notebook | null>(null);
  const [decks, setDecks] = useState<Deck[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [form, setForm] = useState<FormState>({ kind: "closed" });
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [deleteErrorId, setDeleteErrorId] = useState<{
    id: string;
    message: string;
  } | null>(null);

  const load = useCallback(() => {
    let cancelled = false;
    setStatus("loading");
    Promise.all([getNotebook(notebookId), getDecksByNotebook(notebookId)])
      .then(([nb, ds]) => {
        if (cancelled) return;
        setNotebook(nb);
        setDecks(ds);
        setStatus("ready");
      })
      .catch((error) => {
        if (cancelled) return;
        const message = error instanceof Error ? error.message : "";
        setStatus(/not.*found|404/i.test(message) ? "notFound" : "error");
      });
    return () => {
      cancelled = true;
    };
  }, [notebookId]);

  useEffect(() => load(), [load]);

  function openCreate() {
    setFormError(null);
    setForm({ kind: "create" });
  }

  function openEdit(deck: Deck) {
    setFormError(null);
    setForm({ kind: "edit", deck });
  }

  function closeForm() {
    setForm({ kind: "closed" });
    setFormError(null);
  }

  async function handleSubmit(values: { name: string; description: string }) {
    setSaving(true);
    setFormError(null);
    const input = {
      name: values.name,
      description: values.description || undefined,
    };
    try {
      if (form.kind === "create") {
        const created = await createDeck(notebookId, input);
        setDecks((current) => [created, ...current]);
      } else if (form.kind === "edit") {
        const updated = await updateDeck(form.deck.id, input);
        setDecks((current) =>
          current.map((d) => (d.id === updated.id ? updated : d)),
        );
      }
      closeForm();
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Couldn't save the deck.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(deck: Deck) {
    setDeletingId(deck.id);
    setDeleteErrorId(null);
    try {
      await deleteDeck(deck.id);
      setDecks((current) => current.filter((d) => d.id !== deck.id));
    } catch (error) {
      setDeleteErrorId({
        id: deck.id,
        message:
          error instanceof Error ? error.message : "Couldn't delete the deck.",
      });
    } finally {
      setDeletingId(null);
    }
  }

  if (status === "notFound") {
    return (
      <PaperCard className={styles.errorCard} role="alert">
        <p className={styles.errorTitle}>That notebook isn&apos;t in the catalog.</p>
        <p className={styles.errorText}>
          The link may be out of date. Head back to your notebooks.
        </p>
        <Link href="/dashboard" className={styles.backLink}>
          Back to notebooks
        </Link>
      </PaperCard>
    );
  }

  const deckCountLine =
    status !== "ready"
      ? "Catalog / Decks"
      : decks.length === 0
        ? "No decks yet"
        : `${decks.length} deck${decks.length === 1 ? "" : "s"} in this notebook`;

  return (
    <div>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>
            <Link href="/dashboard" className={styles.crumbLink}>
              Notebooks
            </Link>
            {notebook ? ` / ${notebook.name}` : ""}
          </p>
          <h1 className={styles.heading}>{notebook?.name ?? "Notebook"}</h1>
          <p className={styles.countLine}>{deckCountLine}</p>
        </div>
        {form.kind === "closed" && status === "ready" && (
          <Button onClick={openCreate}>New deck</Button>
        )}
      </header>

      {form.kind === "create" && (
        <DeckForm
          mode="create"
          saving={saving}
          error={formError}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      )}
      {form.kind === "edit" && (
        <DeckForm
          mode="edit"
          initial={{
            name: form.deck.name,
            description: form.deck.description ?? "",
          }}
          saving={saving}
          error={formError}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      )}

      {status === "loading" && (
        <p className={styles.state} role="status">
          Pulling the drawer…
        </p>
      )}

      {status === "error" && (
        <PaperCard className={styles.errorCard} role="alert">
          <p className={styles.errorTitle}>Couldn&apos;t open this notebook.</p>
          <p className={styles.errorText}>
            The decks didn&apos;t load. Check that you&apos;re signed in, then
            retry.
          </p>
          <Button variant="secondary" onClick={load}>
            Retry
          </Button>
        </PaperCard>
      )}

      {status === "ready" &&
        (decks.length === 0 ? (
          form.kind === "closed" && (
            <div className={styles.empty}>
              <LogoMark size={96} />
              <p className={styles.emptyTitle}>No decks yet</p>
              <p>Decks hold the flashcards you&apos;ll study.</p>
              <Button onClick={openCreate}>Create your first deck</Button>
            </div>
          )
        ) : (
          <div className={styles.grid}>
            {decks.map((deck) => (
              <DeckStack
                key={deck.id}
                deck={deck}
                onEdit={openEdit}
                onDelete={handleDelete}
                deleting={deletingId === deck.id}
                deleteError={
                  deleteErrorId?.id === deck.id ? deleteErrorId.message : null
                }
              />
            ))}
          </div>
        ))}
    </div>
  );
}
```

- [ ] **Step 2: Write the page styles**

Create `apps/web/app/(app)/notebooks/[notebookId]/page.module.css`:

```css
.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}
.eyebrow {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: var(--color-text-muted);
  margin-bottom: var(--space-2);
}
.crumbLink {
  color: inherit;
  text-decoration: none;
}
.crumbLink:hover {
  color: var(--ink);
}
.heading {
  font-size: 2.1rem;
  font-weight: 800;
}
.countLine {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--color-text-muted);
  margin-top: var(--space-1);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-8);
}
.state {
  font-family: var(--font-mono);
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--color-text-muted);
  padding: var(--space-8) 0;
  text-align: center;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-8) 0;
  text-align: center;
}
.emptyTitle {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
}
.errorCard {
  padding: var(--space-6);
  max-width: 480px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}
.errorTitle {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
}
.errorText {
  color: var(--color-text-muted);
  font-size: 0.95rem;
}
.backLink {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--blue);
}

@media (max-width: 640px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }
}
```

- [ ] **Step 3: Make dashboard notebook cards link to the detail page**

In `apps/web/app/(app)/dashboard/page.tsx`, add the Next `Link` import at the top with the other imports:

```tsx
import Link from "next/link";
```

Then wrap the notebook `PaperCard` in a `Link`. Replace the existing card block (the `notebooks.map(...)` body inside `.grid`) with:

```tsx
            {notebooks.map((nb, i) => (
              <Link
                key={nb.id}
                href={`/notebooks/${nb.id}`}
                className={styles.cardLink}
              >
                <PaperCard
                  interactive
                  color={nb.color ?? COLORS[i % COLORS.length]}
                  className={styles.card}
                >
                  <p className={styles.cardMeta}>
                    Updated {dateFormat.format(new Date(nb.updatedAt))}
                  </p>
                  <div className={styles.name}>{nb.name}</div>
                  {nb.description && (
                    <p className={styles.desc}>{nb.description}</p>
                  )}
                </PaperCard>
              </Link>
            ))}
```

- [ ] **Step 4: Add the `.cardLink` style to the dashboard CSS**

Append to `apps/web/app/(app)/dashboard/page.module.css`:

```css
.cardLink {
  text-decoration: none;
  color: inherit;
  display: block;
}
```

- [ ] **Step 5: Typecheck, lint, and build**

Run: `cd apps/web && npx tsc --noEmit && npm run lint && npm run build`
Expected: type check clean, lint clean, build succeeds and lists the `/notebooks/[notebookId]` route.

- [ ] **Step 6: Manual verification in the running app**

Start the API and web dev server. Then, signed in:
1. On the dashboard, click a notebook card → navigates to `/notebooks/{id}`, header shows the notebook name.
2. "New deck" → form opens, create a deck → it appears as a stack (flat card, "0 cards").
3. Edit the deck → name/description change persists after the form closes.
4. Delete → "Delete" flips to "Really delete? / Cancel"; confirm removes the card.
5. Visit `/notebooks/00000000-0000-0000-0000-000000000000` → "That notebook isn't in the catalog" with a working back link.
6. A notebook with no decks shows the lone-card empty state.

Expected: all six behave as described. (No automated frontend tests exist; this manual pass is the frontend gate.)

- [ ] **Step 7: Commit**

```bash
git add apps/web/app/\(app\)/notebooks apps/web/app/\(app\)/dashboard/page.tsx apps/web/app/\(app\)/dashboard/page.module.css
git commit -m "feat(web): notebook detail page with deck stacks + CRUD"
```

---

## Self-review notes (addressed)

- **Spec coverage:** route + `use(params)` (Task 6); backend card count via grouped query + single count (Tasks 1–2); honest stack depth 0/1–9/10+ and derived colors (Task 4); extracted `DeckStack`/`DeckForm` + thin page (Tasks 4–6); five API functions (Task 3); two-step inline delete confirm (Task 4); 404 / error / empty states (Task 6); backend count tests incl. zero-card, new-deck, empty-list (Task 2); dashboard cards → links (Task 6).
- **Type consistency:** `DeckResponse` 7-field shape and `toResponse(deck, count)` signature are identical across Tasks 1, 2. `Deck` TS type (Task 3) matches the JSON `DeckResponse` produces. `DeckStack`/`DeckForm` prop shapes match their call sites in Task 6.
- **Out of scope (unchanged):** notes, deck detail/flashcards, notebook edit/delete from this page, stored deck color, dashboard form sharing, frontend test infra.
