# Notebook detail page (decks) — design

Date: 2026-07-20
Scope:
- `apps/web` — new `app/(app)/notebooks/[notebookId]/*`, new `components/decks/*`,
  `lib/api.ts`, `app/(app)/dashboard/page.tsx` (cards become links)
- `apps/api` — `domain/deck/*` (`DeckResponse`, `DeckMapper`, `DeckService`),
  `domain/flashcard/FlashcardRepository`

## Problem

The backend has complete CRUD for notebooks, notes, decks, and seven flashcard
types. The frontend has auth pages and a notebooks dashboard, and nothing else.
A notebook on the dashboard is a dead end: there is no way to see or manage what
is inside it.

This spec covers the next slice — opening a notebook and working with the decks
inside it.

## Decisions (confirmed with user)

- **Decks only.** Notes are out of scope. `NoteController` has no list endpoint
  and `NoteRepository`/`NoteService` have no list method, so showing notes would
  require new backend surface. Deferred to its own slice.
- **Full deck CRUD** on day one: list, create, edit (name + description), delete.
  All four endpoints already exist on `DeckController`.
- **Deck cards are not navigable.** There is no deck detail page yet. Edit and
  delete live on the card itself. No stub routes, nothing to remove later.
- **Visual direction: physical stacks** (option B of three mockups reviewed in
  the visual companion; mockups persist in `.superpowers/brainstorm/`). Each deck
  renders as a real stack — tilted, colored layers peeking out behind the top
  card. Per the project design language, the stacked-card silhouette is a
  signature to be used only where the metaphor is earned, and a deck *is* a
  stack of cards.
- **Add `cardCount` to `DeckResponse`**, rather than skipping the count or
  fetching flashcards per deck client-side.
- **Extract components; keep the page thin.** The existing dashboard
  `page.tsx` is 232 lines doing fetch, form, list, and card rendering inline;
  this page does strictly more. The dashboard itself is left alone — refactoring
  it to share a form component was considered and rejected as scope creep.

## Design

### Route and navigation

New route: `apps/web/app/(app)/notebooks/[notebookId]/page.tsx`, inside the
existing `(app)` layout so it inherits the sidebar and the auth guard.

**Next.js 16 note.** `params` is a Promise in this version. In a client component
it is read with React's `use()` hook:

```tsx
export default function NotebookPage({
  params,
}: {
  params: Promise<{ notebookId: string }>;
}) {
  const { notebookId } = use(params);
```

Verified against `apps/web/node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/dynamic-routes.md`
(Next 16.2.9). Do not destructure `params` synchronously.

The page is a **client component**, matching the dashboard. This is forced by the
auth model: the access token is held in memory in `lib/accessToken.ts`, so a
server component cannot read it.

Dashboard notebook cards become links to `/notebooks/{id}`. `PaperCard` renders a
plain `<div>`; wrap it in a Next `<Link>` rather than changing the component's
API. The sidebar's "Decks" row stays marked "soon" — this page is reached
through a notebook, not from the rail.

Header shows a breadcrumb (`Catalog / Notebooks / {name}`, with Notebooks linking
back to `/dashboard`), the notebook name, and a deck count line, mirroring the
dashboard header's structure.

### Backend: card count

Four additive changes.

**1. `FlashcardRepository`** — one grouped count query, so a list of N decks
costs one query rather than N:

```java
@Query("select f.deck.id as deckId, count(f) as count " +
       "from Flashcard f where f.deck.id in :deckIds group by f.deck.id")
List<DeckCardCount> countByDeckIds(@Param("deckIds") Collection<UUID> deckIds);

interface DeckCardCount {
    UUID getDeckId();
    long getCount();
}
```

Decks with no flashcards do not appear in the result — the service must default
them to `0` rather than assuming every deck id is present.

**2. `DeckResponse`** gains `long cardCount` as the final field.

**3. `DeckMapper.toResponse`** takes the count as an explicit parameter. No
overload that defaults to zero: every call site should have to state the count,
so a missed one is a compile error rather than a silent wrong number.

**4. `DeckService`** supplies it:

| Method | Count source |
| --- | --- |
| `listByNotebook` | grouped query over the returned deck ids |
| `listByUser` | grouped query over the returned deck ids |
| `getDeckById` | single-deck count |
| `updateDeck` | single-deck count |
| `createInNotebook` | `0` — a new deck has no cards |
| `createStandalone` | `0` — a new deck has no cards |

This intentionally touches `listByUser` and the standalone-deck paths, which the
notebook page does not use. Accepted deliberately: `cardCount` present on some
`DeckResponse` payloads and absent from others would be worse than a slightly
wider change.

Guard against an empty deck list before calling the grouped query — `in ()` with
an empty collection is invalid in some JPA providers.

### The deck stack (`components/decks/DeckStack.tsx` + CSS Module)

Top card carries: updated date, deck name, description, and a `cardCount` pill.
Behind it sit up to two rotated, colored layers.

**Stack depth is honest** — the silhouette reports real state rather than being
decoration:

| Card count | Rendering |
| --- | --- |
| 0 | single flat card, no backing layers |
| 1–9 | one backing layer |
| 10+ | two backing layers |

**Layer colors are derived, not stored.** `Deck` has no color column and this
spec does not add one. Derive an index from the deck's UUID — sum the string's
char codes, modulo the palette length — into the ordered palette
(`lavender, teal, mint, sky, rose, peach, yellow, coral`), and use that entry
plus the next one (wrapping) for the two layers. Deterministic, so colors are
stable across reloads and differ between decks, with no migration and no extra
field in the create form.

Styling is CSS Modules + tokens only — no inline style objects, no utility
classes in JSX, consistent with the rest of `apps/web`. Reuse existing tokens
(`--border-card`, `--shadow-hard`, `--radius-card`, palette vars); do not
hardcode hex values.

### Components and data flow

The page owns all state and handlers. Presentation is extracted:

- **`components/decks/DeckStack.tsx`** — presentational card, plus the
  edit/delete action row. Props: the deck, and `onEdit`/`onDelete` callbacks.
- **`components/decks/DeckForm.tsx`** — one form for both create and edit,
  switched by a `mode` prop. Fields: name (required, max 200) and description
  (optional). `DeckRequest` and `DeckUpdateRequest` are identical in shape, so a
  single form covers both without branching on fields.
- **`page.tsx`** — fetches, holds `decks` / `status` / form state, passes
  handlers down. Target ~120 lines.

On mount, both `getNotebook(id)` and `getDecksByNotebook(id)` fire. The header
needs the notebook's name and color and there is no combined endpoint. Both are
tracked by a single `status` so the page has one loading state.

`lib/api.ts` gains a `Deck` type and five functions:

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

getNotebook(id)                      // GET  /notebooks/{id}
getDecksByNotebook(notebookId)       // GET  /notebooks/{notebookId}/decks
createDeck(notebookId, input)        // POST /notebooks/{notebookId}/decks
updateDeck(deckId, input)            // PUT  /decks/{deckId}
deleteDeck(deckId)                   // DELETE /decks/{deckId}
```

Mutations update local state directly rather than refetching, following
`handleCreate` in the dashboard.

**Delete uses a two-step inline confirm** on the card: "Delete" flips the card's
action row to "Really delete? / Cancel". No modal — `apps/web` has no modal
primitive and this does not justify introducing one.

### Error handling

Follows the dashboard's established patterns:

| Case | Treatment |
| --- | --- |
| Deck list fails to load | `PaperCard role="alert"` with a Retry button |
| Notebook 404 (bad/stale URL) | "That notebook isn't in the catalog" card, with a link back to `/dashboard` |
| Create or edit fails | inline message inside the form, form stays open with input preserved |
| Delete fails | inline message on the card, card stays in place |
| No decks yet | lone tilted card + "No decks yet" + a create button |

The 404 case is the one genuinely new path — a stale or hand-typed URL must not
crash the page.

Loading state reuses the dashboard's voice ("Pulling the drawer…") with
`role="status"`.

### Testing

**Backend** — extend `apps/api/src/test/java/com/atinroy/recallr/deck/DeckServiceTest.java`:

- `listByNotebook` maps counts onto the right decks
- a deck with zero flashcards reports `cardCount == 0` (it is absent from the
  grouped query result)
- a freshly created deck reports `cardCount == 0`
- an empty deck list does not blow up the grouped query

**Frontend** — `apps/web` has **no test infrastructure**: no Jest, no Vitest, no
Playwright. Its only scripts are `dev`, `build`, `start`, and `lint`.
Verification is therefore `next build` + `eslint` + manual check in the running
app. Standing up a frontend test framework is a real decision that deserves its
own discussion; it is deliberately not smuggled into this slice.

## Out of scope

- Notes inside a notebook (needs a new list endpoint)
- Deck detail / flashcard management (7 subtypes — its own spec)
- Editing or deleting the notebook itself from this page
- Deck colors as a stored field
- Refactoring the dashboard to share a form component
- Frontend test infrastructure
