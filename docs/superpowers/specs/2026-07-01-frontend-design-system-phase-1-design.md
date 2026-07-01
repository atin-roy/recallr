# Recallr Frontend — Phase 1: Design System & App Shell

**Date:** 2026-07-01
**Status:** Design (awaiting review)
**App:** `apps/web` (Next.js 16, React 19)

## Overview

The recallr web app currently has only auth pages, styled in a generic dark-SaaS look (violet→indigo gradient, zinc-950, soft blurred shadows). This phase replaces that with a distinctive, intentional visual language and the foundational architecture every later screen will build on.

This is **Phase 1 of a phased frontend build**. Later phases (their own specs) cover Notebooks/Decks, Notes, and Flashcards CRUD, and the study/review experience.

### Goals

1. Establish a styling architecture the user can actually read and maintain (styling out of JSX).
2. Ship the locked visual language ("Paper Stack") as reusable tokens + primitive components.
3. Restyle the existing auth pages and the landing page.
4. Prove the full stack end-to-end with one authenticated, data-backed screen (a dashboard).

### Non-goals (deferred to later phases)

- Deck / notebook / note / flashcard **creation, editing, deletion** UI.
- The study/review experience (the flashcard-flip hero moment).
- Full deck cards with progress/mastery/due-today (needs backend support that doesn't exist yet — see Backend Notes).
- Dark mode.

## Locked design decisions (from visual brainstorm)

- **Visual direction — "Paper Stack":** warm paper background (`#f4efe3`) with faint dotted grain; colored index cards (full palette, not all cream); thick ~2.5px near-black ink borders (`#1a1a1a`); **hard offset shadows, no blur** (e.g. `6px 6px 0 #1a1a1a`); physically stacked/tilted cards with peeking layers; tactile skeuomorphic props (washi tape, pushpin, gem clip); pressable buttons that depress on `:active`.
- **Fonts:** **Bricolage Grotesque** for headings/titles, **Inter** for body/labels.
- **Palette:** teal `#22c1bf`, lavender `#e7ddfb`, blue `#4f6df5`, warm yellow `#f5c518`, coral `#e0574a`, paper `#f4efe3`, cream `#fffdf7`, ink `#1a1a1a`.
- **Signature move (the stacked-flashcard silhouette):** a *signature, not a texture*. Lean in HARD only where the metaphor is earned — deck cards, the study/review view, and empty/loading states. Keep everything else CALM and flat — notes, forms, settings, and nav get the paper/border/shadow treatment but **no tilting**, single cards not stacks. (Deck cards + study view are later phases; Phase 1 delivers the calm surfaces plus the empty-state stacked-card treatment.)

Reference mockups: `.superpowers/brainstorm/**/paper-stack-v2.html` (look) and `font-pairings.html` (type).

## Styling architecture

**Decision: CSS Modules + a global token file. Remove Tailwind.**

Rationale: the user finds Tailwind-in-JSX hard to read and wants `.tsx` files to be pure structure/logic. Neo-brutalism is built from ~5 repeated primitives (thick border, hard offset shadow, no/small radius, flat bold color, press interaction), which suit a small set of semantic reusable classes far better than per-element utility soup.

### Changes

- **Remove** the `tailwindcss` and `@tailwindcss/postcss` dev dependencies, `postcss.config.mjs`, and the `@import "tailwindcss"` / `@theme` blocks in `globals.css`.
- **Add** `app/tokens.css` — all design tokens as CSS custom properties on `:root` (single source of design truth).
- **Add** per-component `*.module.css` files colocated with each component.
- `app/globals.css` keeps only: a minimal reset, `@import "./tokens.css"`, base `html/body` (paper background + dotted grain, base font/color), and base typography defaults.
- **Zero styling in JSX.** No `className` string utilities; components import their `.module.css` and reference semantic class names (`styles.card`, `styles.button`).

### Token file shape (`app/tokens.css`)

```css
:root {
  /* palette */
  --ink: #1a1a1a;
  --paper: #f4efe3;
  --cream: #fffdf7;
  --teal: #22c1bf;
  --lavender: #e7ddfb;
  --blue: #4f6df5;
  --yellow: #f5c518;
  --coral: #e0574a;

  /* borders & shadows (the brutalist primitives) */
  --border-w: 2.5px;
  --border: var(--border-w) solid var(--ink);
  --shadow-hard: 6px 6px 0 var(--ink);
  --shadow-hard-sm: 4px 4px 0 var(--ink);
  --shadow-hard-lg: 8px 8px 0 var(--ink);

  /* radius, spacing scale, etc. */
  --radius: 10px;
  --space-1: 4px; --space-2: 8px; --space-3: 12px;
  --space-4: 16px; --space-6: 24px; --space-8: 32px;

  /* type */
  --font-display: var(--font-bricolage), system-ui, sans-serif;
  --font-body: var(--font-inter), system-ui, sans-serif;
}
```

### Fonts

Swap the `next/font/google` setup in `app/layout.tsx`: remove Geist/Raleway/Archivo Black, add **Bricolage Grotesque** (`--font-bricolage`) and **Inter** (`--font-inter`). Expose as CSS variables consumed by `--font-display` / `--font-body`. The existing `Logo.tsx` (which references `--font-raleway` / `--font-archivo-black`) is restyled to the new fonts + stacked-card glyph.

## Component inventory (primitives)

Each is a small, focused component with a colocated `.module.css`. These are the reusable brutalist primitives every later phase composes from.

| Component | Purpose | Notes |
|---|---|---|
| `PaperCard` | The base surface: cream fill, ink border, hard offset shadow. | Props for `color` (palette variant) and optional `interactive` (hover-lift). **Flat, no tilt** — stacking is a separate concern. |
| `Button` | Primary/secondary chunky button. | Pressable: `:hover` lift, `:active` translate + shadow shrink. Variants via palette color. |
| `Badge` | Small pill/tag label (e.g. counts). | Inter, uppercase option. |
| `ProgressBar` | Inset physical track + chunky colored fill. | Used later for mastery; included now as a primitive. |
| `TextField` / `PasswordField` | Form inputs in the paper style. | Replace the existing dark-styled versions. |
| `StackedCardMark` | The signature stacked-flashcard silhouette (3 tilted rounded rects, palette colors, ink borders). | Used in the logo, empty states, and loaders. The one place tilt is allowed in Phase 1. |
| `Logo` | Wordmark + `StackedCardMark` glyph. | Restyle of existing `Logo.tsx`. |

The existing `components/auth/AuthBackground.tsx` and `AuthCard.tsx` are replaced/absorbed by the new paper background + `PaperCard`.

## App shell & layout

- **`components/shell/TopNav`** — the authenticated app nav: `Logo` left, nav items ("Notebooks", etc.) center/left, user avatar right. Paper/cream bar, ink border, hard shadow. Calm (no tilt).
- **`components/shell/AppShell`** — wraps authenticated pages: paper background + dotted grain, `TopNav`, a max-width content container.
- The root `app/layout.tsx` sets fonts + global background; the authenticated route group uses `AppShell`.

## Pages in scope

### 1. Landing `/` (unauthenticated)

Replace the Next.js default `app/page.tsx`. A restyled hero in the paper aesthetic: `Logo`, a headline, a one-line value prop, CTA buttons to Register / Login, and a decorative `StackedCardMark`. Establishes the brand at first impression.

### 2. Auth — `/login`, `/register` (restyle)

Keep existing logic/data flow (`apiFetch`, `setAccessToken`, redirect). Re-skin to the paper language using the new primitives (`PaperCard`, `TextField`, `PasswordField`, `Button`). Remove the gradient/dark styling. Register page follows the same pattern (verify it exists and matches login's structure).

### 3. Dashboard `/dashboard` (authenticated, data-backed)

The end-to-end proof. After login, the user lands here. It:
- Renders inside `AppShell`.
- Fetches the user's **notebooks** via `GET /notebooks` (real data — see Backend Notes for why notebooks, not decks) and renders each as a **flat** `PaperCard` (name + description) in a responsive grid.
- Shows a **stacked-card empty state** (`StackedCardMark` + "No notebooks yet" + CTA) when the list is empty — the one signature moment in Phase 1.
- Shows a loading state and an error state.

## Auth guard / session hydration

The access token is in-memory only (`lib/accessToken.ts`), so it's lost on refresh; a refresh-token cookie persists. Phase 1 adds the minimum needed to make an authenticated route work across reloads:

- **`components/auth/AuthProvider`** (client) — on mount, if there's no in-memory access token, attempt `POST /auth/refresh` to hydrate one (and load `GET /auth/me` for the user). Exposes `{ user, status }` via context + a `useAuth()` hook.
- **Dashboard route** — while `status === "loading"` show a loader; if unauthenticated, redirect to `/login`; else render.
- On login/register success, set the token and route to `/dashboard`.

This provider is foundational and reused by every later authenticated screen.

## File structure (new/changed)

```
apps/web/
├── app/
│   ├── globals.css            # trimmed: reset + import tokens + base body/type
│   ├── tokens.css             # NEW: all design tokens
│   ├── layout.tsx             # fonts swapped (Bricolage + Inter)
│   ├── page.tsx               # restyled landing
│   ├── (auth)/login/page.tsx  # restyled
│   ├── (auth)/register/page.tsx # restyled
│   └── (app)/dashboard/page.tsx # NEW: authenticated dashboard
├── components/
│   ├── Logo.tsx + Logo.module.css        # restyled
│   ├── StackedCardMark.tsx + .module.css # NEW signature glyph
│   ├── ui/ (PaperCard, Button, Badge, ProgressBar, TextField, PasswordField) each + .module.css
│   ├── shell/ (AppShell, TopNav) each + .module.css
│   └── auth/AuthProvider.tsx              # NEW
├── lib/ (api.ts, accessToken.ts)  # mostly unchanged; add getMe/refresh helpers if useful
├── postcss.config.mjs   # REMOVED
└── package.json         # tailwind deps removed
```

## Backend notes / discovered gaps

- **No "list all decks for a user" endpoint.** The API exposes `GET /notebooks/{id}/decks` and single-deck routes, but no top-level deck list. So the Phase 1 dashboard lists **notebooks** (`GET /notebooks`), which is real, available data and correctly establishes the card pattern. A top-level deck dashboard (the "Your Decks" surface from the mockups, with card counts / mastery / due-today) requires backend additions (a decks-with-stats endpoint) and lands in a later phase.
- No changes to the API are made in this phase.

## Testing & verification

- **Component render tests** are not currently set up in `apps/web` (no test runner). Phase 1 does **not** introduce a test framework; verification is manual + typecheck/lint:
  - `pnpm lint` and `tsc` (via `next build`) pass.
  - Manual verification with the API running: landing renders; register → login flow works and lands on the dashboard; dashboard loads notebooks (or empty state); reload keeps the session via silent refresh; unauthenticated visit to `/dashboard` redirects to `/login`.
- Introducing a proper component test setup (Vitest + Testing Library) is noted as a candidate for a future phase but is out of scope here to keep Phase 1 focused.

## Success criteria

1. Tailwind fully removed; all styling lives in `tokens.css` + `*.module.css`; no styling utilities in JSX.
2. Bricolage + Inter load and render.
3. Landing, login, register all restyled to the Paper Stack language and functional.
4. Authenticated `/dashboard` renders real notebooks in the paper style, with a stacked-card empty state, and survives reload via silent refresh.
5. It does not look like generic SaaS — the paper texture, hard ink shadows, colored cards, Bricolage headings, and stacked-card signature are all present and intentional.
```
