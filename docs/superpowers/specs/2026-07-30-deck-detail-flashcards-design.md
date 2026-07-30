# Deck detail page (flashcards) — design

Date: 2026-07-30
Scope:
- `apps/web` — new `app/(app)/decks/[deckId]/*`, new `components/flashcards/*`,
  `lib/api.ts`, `components/decks/DeckStack.*` (stacks become links)
- `apps/api` — none; every needed endpoint exists

## Problem

Decks are now full CRUD from the notebook detail page, but a deck is a dead
end: there is no way to see, create, or edit the flashcards inside it. The
backend has complete CRUD for seven flashcard types
(`/decks/{deckId}/flashcards`), none of it reachable from the UI.

This spec covers the next slice — opening a deck and managing its cards. The
spaced-repetition/study engine is explicitly deferred; this page is for
browsing and authoring only.

## Decisions (confirmed with user)

- **Flashcards only.** Notes remain out of scope (still no backend list
  endpoint; note links have no controller at all).
- **All seven types get authoring UI now**: BASIC, MCQ, CLOZE, TRUE_FALSE,
  MATCHING, ORDERING, SHORT_ANSWER. The deck page is genuinely finished
  rather than a subset.
- **Index-card grid** for browsing: each flashcard renders as a physical index
  card; clicking flips it to reveal the answer. Chosen over an expandable list
  and a table — a flashcard *is* an index card, so the physical metaphor is
  earned (same rule that justified deck stacks).
- **Inline authoring panel** at the top of the page, like the notebook and
  deck forms — no new route, no modal.

## Routing and navigation

- New page: `app/(app)/decks/[deckId]/page.tsx` (client component, same
  pattern as the notebook detail page).
- `DeckStack` gets a stretched link to `/decks/{deckId}` using the
  `::after { inset: 0 }` overlay pattern from the dashboard cards; the
  edit/delete actions stay above it with their own hit area.
- Page header: breadcrumb `Dashboard / {notebook} / {deck}` — the deck
  response carries `notebookId`, and the notebook name comes from
  `getNotebook(notebookId)` (fetched in parallel; if it fails, the crumb
  degrades to "Notebook" without blocking the page). Below the crumb: deck
  name, optional description, and a card-count line driven by the loaded
  flashcards (not the possibly stale `cardCount`).
- A deck with `notebookId: null` (backend allows it) breadcrumbs straight to
  the dashboard.

## API client (`lib/api.ts`)

A discriminated union mirroring the backend's `@JsonTypeInfo(property = "type")`
polymorphism:

```ts
type FlashcardBase = { id, deckId, question, createdAt, updatedAt };
export type Flashcard = FlashcardBase & (
  | { type: "BASIC"; answer: string; reverse: boolean }
  | { type: "MCQ"; options: string[]; correctOptionIndex: number; explanation?: string | null }
  | { type: "CLOZE"; answers: string[] }
  | { type: "TRUE_FALSE"; correctAnswer: boolean; explanation?: string | null }
  | { type: "MATCHING"; pairs: { prompt: string; matchValue: string }[] }
  | { type: "ORDERING"; items: string[] }
  | { type: "SHORT_ANSWER"; modelAnswer: string }
);
```

Functions: `getFlashcards(deckId)`, `createFlashcard(deckId, input)`,
`updateFlashcard(deckId, flashcardId, input)`, `deleteFlashcard(deckId, flashcardId)`.
Create/update inputs are the same union minus the base metadata fields
(`type` + `question` + per-type payload). Update requests keep the `type`
discriminator (the backend update DTOs are polymorphic the same way); the
type itself is immutable — the backend cannot convert a card's type.

## Components

Keep the page thin; per-type logic lives in `components/flashcards/`:

- **`FlashcardCard.tsx`** — one index card. Front: type tag (mono eyebrow) +
  question. Back: the answer rendered per type — BASIC answer text (plus a
  "reversible" marker), MCQ options with the correct one marked, cloze
  question with blanks filled, TRUE/FALSE verdict + explanation, matching
  pairs as prompt → match rows, ordering as a numbered list, short answer as
  model-answer text. Click (or Enter/Space — the card is a button) flips it.
  Flip is a CSS 3D transform (`transform: rotateY`), swapped for an instant
  toggle under `prefers-reduced-motion`. Edit/Delete actions on the card use
  the confirm-in-place pattern ("Really delete?" / Cancel), stopping
  propagation so they don't flip the card.
- **`FlashcardForm.tsx`** — the authoring panel. Type picker as radio chips
  (visible only when creating; when editing, the type shows as a static tag).
  Question field, then per-type fields:
  - BASIC: answer + "also study in reverse" checkbox
  - MCQ: option rows (min 2, add/remove), radio to mark the correct one,
    optional explanation
  - CLOZE: answer rows (min 1), one per `{{blank}}` marker in the question,
    in marker order — the convention set by the 2026-07-01 flashcard-types
    spec. The form shows a syntax hint and warns when the marker count and
    answer-row count disagree (warns, not blocks — the backend doesn't
    enforce it)
  - TRUE_FALSE: true/false toggle + optional explanation
  - MATCHING: prompt/match pair rows (min 2, add/remove)
  - ORDERING: item rows in correct order (min 2, add/remove)
  - SHORT_ANSWER: model answer textarea
  Submit disabled until the type's required fields are non-blank. Switching
  type while creating resets the per-type fields but keeps the question.
- **`FlashcardGrid.tsx`** — maps flashcards to `FlashcardCard`s in a
  responsive grid (`repeat(auto-fill, minmax(...))`).

## Data flow, states, errors

Same vocabulary as the notebook page: local `useState` list updated
optimistically-after-response (create prepends, update replaces by id, delete
filters); loading line ("Shuffling the deck…" register), error `PaperCard`
with retry, empty state ("No cards yet" + CTA opening the form). Form errors
render in the panel via the `Error.message` thrown by `apiFetch`; delete
errors render on the affected card.

## Out of scope

- Study/review mode and the spaced-repetition engine.
- Notes UI and note-link backend surface.
- Moving a card between decks; converting a card's type.
- Search/filter/sort within a deck.

## Verification

`tsc --noEmit`, `eslint`, `next build` (no frontend test runner by design),
plus a manual pass against the running backend: create one card of each of
the seven types, flip each, edit one list-based type (MCQ) and one scalar
type (BASIC), delete one, and confirm the 404/error states.
