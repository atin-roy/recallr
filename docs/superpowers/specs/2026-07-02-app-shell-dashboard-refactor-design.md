# App shell + dashboard refactor — design

Date: 2026-07-02
Scope: `apps/web` — `components/shell/Sidebar*`, `app/(app)/dashboard/*`, `lib/api.ts`

## Problem

The authed app shell is a rail with a logo, a single "Notebooks" link, and an
avatar; the dashboard is a bare fetch-and-list. Neither carries the index-card
catalog language of the landing page, and the page offers no actions (no
create, no sign-out, error state says "Try again" but isn't clickable).

## Decisions (confirmed with user)

- Full refactor of both sidebar and dashboard.
- Future sections (Review, Decks, Notes) shown as visibly disabled "soon" rows.
- Real data only — everything rendered derives from API responses
  (`NotebookResponse`: id, name, description, createdAt, updatedAt).
- Inline "New notebook" create flow using the existing `POST /notebooks`
  (defaulted: user was away for this question; easy to strip back).

## Design

### Sidebar → catalog rail (`components/shell/Sidebar.tsx`)

- Brand (unchanged) → dashed hairline divider → mono uppercase "Catalog" label.
- Nav: Notebooks (active, yellow, teal dot) + Review/Decks/Notes as muted
  non-interactive rows, each with a colored dot and a mono `soon` chip;
  `aria-disabled`, rendered as spans not links.
- Footer user block above a hairline: coral initials avatar + "Sign out"
  button → `POST /auth/logout`, clear in-memory token, `router.replace("/login")`.
  Local token is cleared even if the request fails.
- Sidebar becomes a client component (it gains interactivity).

### Dashboard → desk file (`app/(app)/dashboard/page.tsx`)

- Persistent header (shown in all fetch states): mono eyebrow
  `Catalog / Notebooks`, h1 "Your notebooks", mono count line derived from data
  ("N notebooks on file" / "Nothing filed yet"), and a "New notebook" Button.
- Inline create: an index-card `PaperCard` form (TextField name — required,
  matches API's @NotBlank/@Size(200) — plus optional description) that POSTs
  via `createNotebook()` and prepends the result to the grid. Inline error
  line on failure; Cancel closes and resets.
- Notebook cards: existing color rotation + a mono meta line with the real
  `updatedAt` date.
- Error state: designed card with the message and a working Retry button that
  re-runs the fetch.
- Empty state: LogoMark + copy + "Create your first notebook" button opening
  the same form.

### API helpers (`lib/api.ts`)

- `apiFetch` parses via `res.text()` so empty bodies (logout returns void)
  don't crash `res.json()`.
- Add `Notebook` type (with createdAt/updatedAt), `getNotebooks`,
  `createNotebook`, `logout`.

## Constraints

- CSS Modules + tokens.css only; no Tailwind in JSX.
- Mono labels use `--font-mono` (Geist Mono), uppercase, tracked — same
  catalog voice as the landing page.
- Keyboard focus visible, reduced-motion respected, responsive collapse of the
  rail at 720px preserved.

## Out of scope

- Fixing the 401 seen in the user's screenshot (session expiry; re-login).
- Real routes for Review/Decks/Notes.
- Notebook edit/delete UI.
