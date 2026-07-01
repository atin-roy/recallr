# Recallr Frontend Phase 1 — Design System & App Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic dark-SaaS frontend with the "Paper Stack" visual language, a CSS-Modules-and-tokens styling architecture, restyled landing/auth, and one authenticated data-backed dashboard.

**Architecture:** Next.js 16 App Router + React 19. Tailwind is removed; all styling lives in a global `app/tokens.css` (CSS custom properties = single source of design truth) plus colocated `*.module.css` files. Reusable brutalist primitives (PaperCard, Button, etc.) are composed into an app shell, restyled auth pages, a landing page, and a notebooks dashboard guarded by a client-side auth provider that silently rehydrates the session from the refresh cookie.

**Tech Stack:** Next.js 16.2.9, React 19.2.4, TypeScript, `next/font/google` (Bricolage Grotesque + Inter), CSS Modules. Package manager: **pnpm**. Existing helpers: `lib/api.ts` (`apiFetch<T>`), `lib/accessToken.ts` (in-memory token).

## Verification approach (read first)

`apps/web` has **no test runner**, and per the spec Phase 1 does **not** introduce one. So there is no unit-test TDD cycle. Instead, **every task's verification cycle is:**

1. **Typecheck + lint + build:** `pnpm --dir apps/web lint` and `pnpm --dir apps/web exec tsc --noEmit` (fast) — must pass with no errors.
2. **Manual render check:** with the dev server running (`pnpm --dir apps/web dev`) and the API up (`http://localhost:8080`), open the affected route and confirm the described visual result.
3. **Commit.**

All shell commands assume repo root `/home/atin/projects/recallr` as CWD unless noted. Run pnpm from repo root with `--dir apps/web` (do not `cd`).

## Global Constraints

- **No styling in JSX.** No utility class strings, no inline `style` for design values. Components import a colocated `*.module.css` and reference `styles.name`. Design values come from `tokens.css` variables. (One narrow exception: setting a dynamic value like a progress width, e.g. `style={{ width: pct + "%" }}`.)
- **No Tailwind.** The `tailwindcss` / `@tailwindcss/postcss` deps, `postcss.config.mjs`, and Tailwind directives are removed.
- **Fonts:** Bricolage Grotesque = headings/titles (`--font-display`); Inter = body/labels (`--font-body`).
- **Palette (exact):** ink `#1a1a1a`, paper `#f4efe3`, cream `#fffdf7`, teal `#22c1bf`, lavender `#e7ddfb`, blue `#4f6df5`, yellow `#f5c518`, coral `#e0574a`, mint `#c9f0dc`.
- **Brutalist primitives:** thick ink borders (~2.5px), **hard offset shadows with no blur** (`Npx Npx 0 var(--ink)`), small radius (~10px), buttons depress on `:active`.
- **Signature (stacked cards):** used ONLY in `StackedCardMark` (logo, empty states, loaders). Everything else is calm/flat — no tilt.
- **Commit style:** conventional commits. (A repo `commit-msg` hook strips any Claude co-author trailer automatically; do not add one.)
- **API base:** `process.env.NEXT_PUBLIC_API_URL` (already set to `http://localhost:8080/api/v1` in `apps/web/.env.local`).

## File Structure

```
apps/web/
├── app/
│   ├── tokens.css                      # NEW — all design tokens
│   ├── globals.css                     # MODIFY — reset + import tokens + base body/type
│   ├── layout.tsx                      # MODIFY — swap fonts to Bricolage + Inter
│   ├── page.tsx                        # MODIFY — restyled landing
│   ├── (auth)/login/page.tsx           # MODIFY — restyle + redirect to /dashboard
│   ├── (auth)/register/page.tsx        # MODIFY — restyle + redirect to /dashboard
│   └── (app)/
│       ├── layout.tsx                  # NEW — AuthProvider + AppShell + guard
│       └── dashboard/page.tsx          # NEW — notebooks dashboard
├── components/
│   ├── Logo.tsx + Logo.module.css      # MODIFY — new fonts + StackedCardMark
│   ├── StackedCardMark.tsx + .module.css   # NEW — signature glyph
│   ├── ui/
│   │   ├── PaperCard.tsx + .module.css      # NEW
│   │   ├── Button.tsx + .module.css         # NEW
│   │   ├── Badge.tsx + .module.css          # NEW
│   │   ├── ProgressBar.tsx + .module.css    # NEW
│   │   ├── TextField.tsx + .module.css      # NEW (replaces components/auth/TextField)
│   │   └── PasswordField.tsx + .module.css  # NEW (replaces components/auth/PasswordField)
│   ├── shell/
│   │   ├── AppShell.tsx + .module.css       # NEW
│   │   └── TopNav.tsx + .module.css         # NEW
│   └── auth/AuthProvider.tsx           # NEW
├── lib/api.ts                          # MODIFY — add getMe/refresh helpers
├── lib/accessToken.ts                  # unchanged
├── postcss.config.mjs                  # DELETE
└── package.json                        # MODIFY — remove tailwind deps
```

Old files removed once unused: `components/auth/AuthBackground.tsx`, `components/auth/AuthCard.tsx`, `components/auth/TextField.tsx`, `components/auth/PasswordField.tsx` (deleted in Task 4 after the auth pages stop importing them).

---

### Task 1: Styling foundation (tokens, globals, fonts, remove Tailwind)

**Files:**
- Create: `apps/web/app/tokens.css`
- Modify: `apps/web/app/globals.css`
- Modify: `apps/web/app/layout.tsx`
- Delete: `apps/web/postcss.config.mjs`
- Modify: `apps/web/package.json`

**Interfaces:**
- Produces: CSS variables consumed everywhere — `--ink --paper --cream --teal --lavender --blue --yellow --coral --mint`, `--border-w --border --shadow-hard --shadow-hard-sm --shadow-hard-lg`, `--radius`, `--space-1..8`, `--font-display --font-body`. Font CSS vars `--font-bricolage` / `--font-inter` are set on `<html>` by `layout.tsx`.

- [ ] **Step 1: Create `app/tokens.css`**

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
  --mint: #c9f0dc;

  /* borders & hard offset shadows */
  --border-w: 2.5px;
  --border: var(--border-w) solid var(--ink);
  --shadow-hard-sm: 4px 4px 0 var(--ink);
  --shadow-hard: 6px 6px 0 var(--ink);
  --shadow-hard-lg: 8px 8px 0 var(--ink);

  /* radius & spacing */
  --radius: 10px;
  --radius-sm: 7px;
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-6: 24px;
  --space-8: 32px;

  /* type */
  --font-display: var(--font-bricolage), system-ui, sans-serif;
  --font-body: var(--font-inter), system-ui, sans-serif;
}
```

- [ ] **Step 2: Replace `app/globals.css` entirely**

```css
@import "./tokens.css";

*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  padding: 0;
  min-height: 100%;
}

body {
  background-color: var(--paper);
  background-image: radial-gradient(circle, rgba(26, 26, 26, 0.05) 1px, transparent 1px);
  background-size: 14px 14px;
  color: var(--ink);
  font-family: var(--font-body);
  -webkit-font-smoothing: antialiased;
}

h1, h2, h3, h4, h5 {
  font-family: var(--font-display);
  margin: 0;
}

a {
  color: inherit;
}
```

- [ ] **Step 3: Swap fonts in `app/layout.tsx`**

Replace the whole file with:

```tsx
import type { Metadata } from "next";
import { Bricolage_Grotesque, Inter } from "next/font/google";
import "./globals.css";

const bricolage = Bricolage_Grotesque({
  variable: "--font-bricolage",
  subsets: ["latin"],
  weight: ["600", "700", "800"],
});

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: "recallr",
  description: "Spaced-repetition study, built on flashcards.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${bricolage.variable} ${inter.variable}`}>
      <body>{children}</body>
    </html>
  );
}
```

- [ ] **Step 4: Remove Tailwind**

Delete `apps/web/postcss.config.mjs`. In `apps/web/package.json`, remove the `"@tailwindcss/postcss"` and `"tailwindcss"` entries from `devDependencies`. Then run:

```bash
pnpm --dir apps/web install
```

- [ ] **Step 5: Verify (build + manual)**

```bash
pnpm --dir apps/web lint
pnpm --dir apps/web exec tsc --noEmit
pnpm --dir apps/web build
```
Expected: all pass, no Tailwind-related errors. (Auth pages still use Tailwind classes at this point — those classes now simply do nothing; that's fine, they're restyled in Task 4. The build must still succeed.)
Manual: `pnpm --dir apps/web dev`, open `http://localhost:3000` — background is warm paper with a faint dot grid.

- [ ] **Step 6: Commit**

```bash
git add apps/web/app/tokens.css apps/web/app/globals.css apps/web/app/layout.tsx apps/web/package.json apps/web/pnpm-lock.yaml
git rm apps/web/postcss.config.mjs
git commit -m "feat(web): styling foundation — tokens, paper base, Bricolage+Inter, remove Tailwind"
```

---

### Task 2: StackedCardMark + Logo restyle

**Files:**
- Create: `apps/web/components/StackedCardMark.tsx`, `apps/web/components/StackedCardMark.module.css`
- Modify: `apps/web/components/Logo.tsx`
- Create: `apps/web/components/Logo.module.css`

**Interfaces:**
- Produces: `StackedCardMark` — `export function StackedCardMark({ size = 34 }: { size?: number })`. `Logo` — `export function Logo()` renders the mark + wordmark "recallr".

- [ ] **Step 1: Create `components/StackedCardMark.module.css`**

```css
.mark {
  position: relative;
  flex-shrink: 0;
}
.mark span {
  position: absolute;
  width: 66%;
  height: 52%;
  border: 2.2px solid var(--ink);
  border-radius: 4px;
  top: 26%;
  left: 12%;
}
.mark span:nth-child(1) {
  background: var(--blue);
  transform: rotate(-14deg);
}
.mark span:nth-child(2) {
  background: var(--lavender);
  transform: rotate(6deg);
  top: 12%;
  left: 18%;
}
.mark span:nth-child(3) {
  background: var(--teal);
  transform: rotate(-2deg);
  top: 4%;
  left: 16%;
}
```

- [ ] **Step 2: Create `components/StackedCardMark.tsx`**

```tsx
import styles from "./StackedCardMark.module.css";

export function StackedCardMark({ size = 34 }: { size?: number }) {
  return (
    <span
      className={styles.mark}
      style={{ width: size, height: size * 0.9 }}
      aria-hidden="true"
    >
      <span />
      <span />
      <span />
    </span>
  );
}
```

- [ ] **Step 3: Create `components/Logo.module.css`**

```css
.logo {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
}
.wordmark {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 1.5rem;
  letter-spacing: -0.5px;
  color: var(--ink);
}
```

- [ ] **Step 4: Replace `components/Logo.tsx`**

```tsx
import { StackedCardMark } from "./StackedCardMark";
import styles from "./Logo.module.css";

export function Logo() {
  return (
    <span className={styles.logo}>
      <StackedCardMark size={32} />
      <span className={styles.wordmark}>recallr</span>
    </span>
  );
}
```

- [ ] **Step 5: Verify**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit
```
Expected: pass. Manual check happens in Task 4/5 where Logo is rendered.

- [ ] **Step 6: Commit**

```bash
git add apps/web/components/StackedCardMark.tsx apps/web/components/StackedCardMark.module.css apps/web/components/Logo.tsx apps/web/components/Logo.module.css
git commit -m "feat(web): StackedCardMark signature glyph + restyled Logo"
```

---

### Task 3: Core primitives (PaperCard, Button, Badge, ProgressBar)

**Files:**
- Create: `apps/web/components/ui/PaperCard.tsx` + `.module.css`
- Create: `apps/web/components/ui/Button.tsx` + `.module.css`
- Create: `apps/web/components/ui/Badge.tsx` + `.module.css`
- Create: `apps/web/components/ui/ProgressBar.tsx` + `.module.css`

**Interfaces:**
- Produces:
  - `PaperCard`: `export function PaperCard(props: React.HTMLAttributes<HTMLDivElement> & { color?: "cream" | "lavender" | "teal" | "yellow"; interactive?: boolean })`
  - `Button`: `export function Button(props: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" })`
  - `Badge`: `export function Badge({ children }: { children: React.ReactNode })`
  - `ProgressBar`: `export function ProgressBar({ value, color = "blue" }: { value: number; color?: "blue" | "teal" | "coral" })`

- [ ] **Step 1: `components/ui/PaperCard.module.css`**

```css
.card {
  background: var(--cream);
  border: 3px solid var(--ink);
  border-radius: 12px;
  box-shadow: var(--shadow-hard);
}
.interactive {
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.interactive:hover {
  transform: translateY(-4px);
  box-shadow: 10px 10px 0 var(--ink);
}
.cream { background: var(--cream); }
.lavender { background: var(--lavender); }
.teal { background: var(--teal); }
.yellow { background: var(--yellow); }
```

- [ ] **Step 2: `components/ui/PaperCard.tsx`**

```tsx
import styles from "./PaperCard.module.css";

type Props = React.HTMLAttributes<HTMLDivElement> & {
  color?: "cream" | "lavender" | "teal" | "yellow";
  interactive?: boolean;
};

export function PaperCard({
  color = "cream",
  interactive = false,
  className,
  children,
  ...rest
}: Props) {
  const classes = [
    styles.card,
    styles[color],
    interactive ? styles.interactive : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <div className={classes} {...rest}>
      {children}
    </div>
  );
}
```

- [ ] **Step 3: `components/ui/Button.module.css`**

```css
.btn {
  font-family: var(--font-body);
  font-weight: 700;
  font-size: 1rem;
  color: var(--ink);
  border: var(--border);
  border-radius: 8px;
  padding: 12px 22px;
  cursor: pointer;
  box-shadow: var(--shadow-hard-sm);
  transition: transform 0.08s ease, box-shadow 0.08s ease;
}
.btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hard);
}
.btn:active {
  transform: translate(3px, 3px);
  box-shadow: 2px 2px 0 var(--ink);
}
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
  box-shadow: var(--shadow-hard-sm);
}
.primary { background: var(--yellow); }
.secondary { background: var(--cream); }
```

- [ ] **Step 4: `components/ui/Button.tsx`**

```tsx
import styles from "./Button.module.css";

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary";
};

export function Button({ variant = "primary", className, ...rest }: Props) {
  const classes = [styles.btn, styles[variant], className ?? ""]
    .filter(Boolean)
    .join(" ");
  return <button className={classes} {...rest} />;
}
```

- [ ] **Step 5: `components/ui/Badge.module.css`**

```css
.badge {
  display: inline-block;
  font-family: var(--font-body);
  font-weight: 700;
  font-size: 0.72rem;
  letter-spacing: 0.3px;
  background: var(--cream);
  border: 2px solid var(--ink);
  border-radius: 20px;
  padding: 4px 11px;
  box-shadow: 2px 2px 0 var(--ink);
}
```

- [ ] **Step 6: `components/ui/Badge.tsx`**

```tsx
import styles from "./Badge.module.css";

export function Badge({ children }: { children: React.ReactNode }) {
  return <span className={styles.badge}>{children}</span>;
}
```

- [ ] **Step 7: `components/ui/ProgressBar.module.css`**

```css
.track {
  height: 16px;
  border-radius: 20px;
  background: var(--paper);
  border: 2px solid var(--ink);
  box-shadow: inset 2px 2px 0 rgba(26, 26, 26, 0.35);
  overflow: hidden;
}
.fill {
  height: 100%;
  border-right: 2px solid var(--ink);
}
.blue { background: var(--blue); }
.teal { background: var(--teal); }
.coral { background: var(--coral); }
```

- [ ] **Step 8: `components/ui/ProgressBar.tsx`**

```tsx
import styles from "./ProgressBar.module.css";

export function ProgressBar({
  value,
  color = "blue",
}: {
  value: number;
  color?: "blue" | "teal" | "coral";
}) {
  const pct = Math.max(0, Math.min(100, value));
  return (
    <div className={styles.track}>
      <div
        className={`${styles.fill} ${styles[color]}`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}
```

- [ ] **Step 9: Verify**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit
```
Expected: pass.

- [ ] **Step 10: Commit**

```bash
git add apps/web/components/ui
git commit -m "feat(web): core primitives — PaperCard, Button, Badge, ProgressBar"
```

---

### Task 4: Form fields + restyle auth pages

**Files:**
- Create: `apps/web/components/ui/TextField.tsx` + `.module.css`
- Create: `apps/web/components/ui/PasswordField.tsx` + `.module.css`
- Modify: `apps/web/app/(auth)/login/page.tsx`
- Modify: `apps/web/app/(auth)/register/page.tsx`
- Delete (after): `apps/web/components/auth/TextField.tsx`, `PasswordField.tsx`, `AuthCard.tsx`, `AuthBackground.tsx`

**Interfaces:**
- Consumes: `PaperCard`, `Button` (Task 3), `Logo` (Task 2).
- Produces:
  - `TextField`: `export function TextField(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string; id: string })`
  - `PasswordField`: `export function PasswordField(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string; id: string; labelAddon?: React.ReactNode })`

- [ ] **Step 1: Read the existing register page** to mirror its data flow.

Run: `cat apps/web/app/\(auth\)/register/page.tsx`
Expected: note the endpoint it calls and field names, so the restyle preserves behavior.

- [ ] **Step 2: `components/ui/TextField.module.css`**

```css
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.labelRow {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.label {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 0.85rem;
}
.input {
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--ink);
  background: var(--cream);
  border: var(--border);
  border-radius: var(--radius-control);
  padding: 11px 13px;
  box-shadow: inset 2px 2px 0 rgba(26, 26, 26, 0.12);
}
.input:focus {
  outline: none;
  box-shadow: var(--shadow-hard-sm);
}
.input::placeholder {
  color: rgba(26, 26, 26, 0.4);
}
```

- [ ] **Step 3: `components/ui/TextField.tsx`**

```tsx
import styles from "./TextField.module.css";

type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  id: string;
};

export function TextField({ label, id, ...rest }: Props) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      <input id={id} className={styles.input} {...rest} />
    </div>
  );
}
```

- [ ] **Step 4: `components/ui/PasswordField.module.css`**

```css
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.labelRow {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.label {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 0.85rem;
}
.wrap {
  position: relative;
  display: flex;
}
.input {
  flex: 1;
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--ink);
  background: var(--cream);
  border: var(--border);
  border-radius: var(--radius-control);
  padding: 11px 46px 11px 13px;
  box-shadow: inset 2px 2px 0 rgba(26, 26, 26, 0.12);
}
.input:focus {
  outline: none;
  box-shadow: var(--shadow-hard-sm);
}
.toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 0.78rem;
  cursor: pointer;
  color: var(--ink);
}
```

- [ ] **Step 5: `components/ui/PasswordField.tsx`**

```tsx
"use client";

import { useState } from "react";
import styles from "./PasswordField.module.css";

type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  id: string;
  labelAddon?: React.ReactNode;
};

export function PasswordField({ label, id, labelAddon, ...rest }: Props) {
  const [show, setShow] = useState(false);
  return (
    <div className={styles.field}>
      <div className={styles.labelRow}>
        <label className={styles.label} htmlFor={id}>
          {label}
        </label>
        {labelAddon}
      </div>
      <div className={styles.wrap}>
        <input
          id={id}
          type={show ? "text" : "password"}
          className={styles.input}
          {...rest}
        />
        <button
          type="button"
          className={styles.toggle}
          onClick={() => setShow((s) => !s)}
        >
          {show ? "Hide" : "Show"}
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 6: Restyle `app/(auth)/login/page.tsx`**

Keep the existing `handleSubmit` logic exactly, but change the redirect target to `/dashboard` and swap the markup. Replace the file with:

```tsx
"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/Logo";
import { PaperCard } from "@/components/ui/PaperCard";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { PasswordField } from "@/components/ui/PasswordField";
import { apiFetch } from "@/lib/api";
import { setAccessToken } from "@/lib/accessToken";
import styles from "./page.module.css";

type LoginResponse = { accessToken: string };

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    const formData = new FormData(event.currentTarget);
    const email = formData.get("email");
    const password = formData.get("password");
    if (typeof email !== "string" || typeof password !== "string") return;

    try {
      const data = await apiFetch<LoginResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setAccessToken(data.accessToken);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    }
  };

  return (
    <main className={styles.main}>
      <PaperCard className={styles.card}>
        <div className={styles.logo}>
          <Logo />
        </div>
        <h1 className={styles.title}>Welcome back</h1>
        <p className={styles.subtitle}>Sign in to continue your recall journey.</p>

        <form className={styles.form} onSubmit={handleSubmit}>
          <TextField
            id="email"
            name="email"
            type="email"
            label="Email"
            placeholder="roy@example.com"
            required
          />
          <PasswordField
            id="password"
            name="password"
            label="Password"
            required
            labelAddon={
              <Link href="/forgot-password" className={styles.link}>
                Forgot password?
              </Link>
            }
          />
          {error && <p className={styles.error}>{error}</p>}
          <Button type="submit">Sign in</Button>
        </form>

        <p className={styles.footer}>
          Don&apos;t have an account?{" "}
          <Link href="/register" className={styles.link}>
            Create one
          </Link>
        </p>
      </PaperCard>
    </main>
  );
}
```

- [ ] **Step 7: Create `app/(auth)/login/page.module.css`**

```css
.main {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
}
.card {
  width: 100%;
  max-width: 420px;
  padding: var(--space-8);
}
.logo {
  margin-bottom: var(--space-6);
}
.title {
  font-size: 1.8rem;
  font-weight: 800;
}
.subtitle {
  margin: 6px 0 0;
  color: rgba(26, 26, 26, 0.7);
  font-size: 0.95rem;
}
.form {
  margin-top: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.form button {
  margin-top: var(--space-2);
}
.error {
  margin: 0;
  background: var(--coral);
  color: var(--cream);
  border: var(--border-thin);
  border-radius: var(--radius-control);
  padding: 10px 12px;
  font-size: 0.9rem;
}
.footer {
  margin-top: var(--space-6);
  text-align: center;
  font-size: 0.9rem;
  color: rgba(26, 26, 26, 0.7);
}
.link {
  font-weight: 600;
  text-decoration: underline;
  text-decoration-thickness: 2px;
}
```

- [ ] **Step 8: Restyle `app/(auth)/register/page.tsx` and add `page.module.css`**

Mirror the login structure exactly (same imports, same `styles`, a `register/page.module.css` identical to login's). Preserve the register page's existing submit logic and field set from Step 1; only change: markup uses `PaperCard`/`TextField`/`PasswordField`/`Button`, heading text "Create your account" + subtitle "Start building your recall habit.", the footer links to `/login` ("Already have an account? Sign in"), and on success it sets the access token (if the register response returns one) then `router.push("/dashboard")` — matching whatever the current register flow does per Step 1. If the current register flow redirects to `/login` instead of auto-logging-in, keep that behavior and route to `/login`.

Create `app/(auth)/register/page.module.css` with the same content as login's `page.module.css` (Step 7).

- [ ] **Step 9: Delete the obsolete auth components**

```bash
git rm apps/web/components/auth/TextField.tsx apps/web/components/auth/PasswordField.tsx apps/web/components/auth/AuthCard.tsx apps/web/components/auth/AuthBackground.tsx
```

- [ ] **Step 10: Verify (build + manual)**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit && pnpm --dir apps/web build
```
Expected: pass, and no remaining imports of the deleted `components/auth/*` files (build fails if any remain — fix them).
Manual: open `http://localhost:3000/login` and `/register` — paper card, ink border + hard shadow, Logo with stacked-card glyph, Bricolage heading, yellow pressable "Sign in" button that depresses on click, Show/Hide password toggle works.

- [ ] **Step 11: Commit**

```bash
git add apps/web/components/ui apps/web/app/\(auth\)
git commit -m "feat(web): paper-style form fields + restyled login/register"
```

---

### Task 5: App shell (AppShell + TopNav)

**Files:**
- Create: `apps/web/components/shell/TopNav.tsx` + `.module.css`
- Create: `apps/web/components/shell/AppShell.tsx` + `.module.css`

**Interfaces:**
- Consumes: `Logo` (Task 2).
- Produces:
  - `TopNav`: `export function TopNav({ userInitials }: { userInitials?: string })`
  - `AppShell`: `export function AppShell({ userInitials, children }: { userInitials?: string; children: React.ReactNode })`

- [ ] **Step 1: `components/shell/TopNav.module.css`**

```css
.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--cream);
  border: var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-hard-sm);
  padding: 10px 18px;
}
.links {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}
.link {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 0.92rem;
  padding: 7px 14px;
  border-radius: var(--radius-sm);
  border: 2.2px solid transparent;
  text-decoration: none;
}
.active {
  background: var(--yellow);
  border-color: var(--ink);
  box-shadow: 2.5px 2.5px 0 var(--ink);
}
.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--coral);
  border: 2.2px solid var(--ink);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
  font-weight: 700;
  color: var(--cream);
  font-size: 0.95rem;
  margin-left: 14px;
  box-shadow: var(--shadow-hard-xs);
}
```

- [ ] **Step 2: `components/shell/TopNav.tsx`**

```tsx
import Link from "next/link";
import { Logo } from "@/components/Logo";
import styles from "./TopNav.module.css";

export function TopNav({ userInitials = "" }: { userInitials?: string }) {
  return (
    <nav className={styles.nav}>
      <Link href="/dashboard" aria-label="recallr home">
        <Logo />
      </Link>
      <div className={styles.links}>
        <Link href="/dashboard" className={`${styles.link} ${styles.active}`}>
          Notebooks
        </Link>
        <span className={styles.avatar}>{userInitials}</span>
      </div>
    </nav>
  );
}
```

- [ ] **Step 3: `components/shell/AppShell.module.css`**

```css
.shell {
  min-height: 100vh;
  padding: var(--space-6);
}
.container {
  max-width: 1040px;
  margin: 0 auto;
}
.content {
  margin-top: var(--space-8);
}
```

- [ ] **Step 4: `components/shell/AppShell.tsx`**

```tsx
import { TopNav } from "./TopNav";
import styles from "./AppShell.module.css";

export function AppShell({
  userInitials,
  children,
}: {
  userInitials?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={styles.shell}>
      <div className={styles.container}>
        <TopNav userInitials={userInitials} />
        <div className={styles.content}>{children}</div>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Verify**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit
```
Expected: pass. (Rendered in Task 6/8.)

- [ ] **Step 6: Commit**

```bash
git add apps/web/components/shell
git commit -m "feat(web): app shell — TopNav + AppShell layout"
```

---

### Task 6: Auth provider + guarded (app) layout

**Files:**
- Modify: `apps/web/lib/api.ts`
- Create: `apps/web/components/auth/AuthProvider.tsx`
- Create: `apps/web/app/(app)/layout.tsx`
- Create: `apps/web/app/(app)/loading-shell.module.css`

**Interfaces:**
- Consumes: `apiFetch` (`lib/api.ts`), `setAccessToken`/`getAccessToken` (`lib/accessToken.ts`), `AppShell` (Task 5), `StackedCardMark` (Task 2).
- Produces:
  - `lib/api.ts` adds: `refreshSession(): Promise<{ accessToken: string }>` and `getMe(): Promise<Me>` where `type Me = { id: string; email: string }`.
  - `AuthProvider`: `export function AuthProvider({ children }: { children: React.ReactNode })` + `export function useAuth(): { user: Me | null; status: "loading" | "authed" | "guest" }`.

- [ ] **Step 1: Add helpers to `lib/api.ts`**

Append to the file:

```tsx
export type Me = { id: string; email: string };

export function refreshSession() {
  return apiFetch<{ accessToken: string }>("/auth/refresh", { method: "POST" });
}

export function getMe() {
  return apiFetch<Me>("/auth/me");
}
```

- [ ] **Step 2: Create `components/auth/AuthProvider.tsx`**

```tsx
"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { getAccessToken, setAccessToken } from "@/lib/accessToken";
import { refreshSession, getMe, type Me } from "@/lib/api";

type AuthState = { user: Me | null; status: "loading" | "authed" | "guest" };

const AuthContext = createContext<AuthState>({ user: null, status: "loading" });

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({ user: null, status: "loading" });

  useEffect(() => {
    let cancelled = false;
    async function hydrate() {
      try {
        if (!getAccessToken()) {
          const { accessToken } = await refreshSession();
          setAccessToken(accessToken);
        }
        const user = await getMe();
        if (!cancelled) setState({ user, status: "authed" });
      } catch {
        if (!cancelled) setState({ user: null, status: "guest" });
      }
    }
    hydrate();
    return () => {
      cancelled = true;
    };
  }, []);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}
```

- [ ] **Step 3: Create `app/(app)/loading-shell.module.css`**

```css
.center {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
}
.label {
  font-family: var(--font-body);
  font-weight: 600;
  color: rgba(26, 26, 26, 0.7);
}
```

- [ ] **Step 4: Create `app/(app)/layout.tsx`**

```tsx
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthProvider, useAuth } from "@/components/auth/AuthProvider";
import { AppShell } from "@/components/shell/AppShell";
import { StackedCardMark } from "@/components/StackedCardMark";
import styles from "./loading-shell.module.css";

function Gate({ children }: { children: React.ReactNode }) {
  const { user, status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "guest") router.replace("/login");
  }, [status, router]);

  if (status !== "authed" || !user) {
    return (
      <div className={styles.center}>
        <StackedCardMark size={56} />
        <span className={styles.label}>Loading…</span>
      </div>
    );
  }

  const initials = user.email.slice(0, 2).toUpperCase();
  return <AppShell userInitials={initials}>{children}</AppShell>;
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <Gate>{children}</Gate>
    </AuthProvider>
  );
}
```

- [ ] **Step 5: Verify**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit
```
Expected: pass. (End-to-end manual check in Task 8, once a page exists under `(app)`.)

- [ ] **Step 6: Commit**

```bash
git add apps/web/lib/api.ts apps/web/components/auth/AuthProvider.tsx apps/web/app/\(app\)/layout.tsx apps/web/app/\(app\)/loading-shell.module.css
git commit -m "feat(web): AuthProvider session hydration + guarded (app) layout"
```

---

### Task 7: Restyle landing page

**Files:**
- Modify: `apps/web/app/page.tsx`
- Create: `apps/web/app/page.module.css`

**Interfaces:**
- Consumes: `Logo` (Task 2), `Button` (Task 3), `StackedCardMark` (Task 2).

- [ ] **Step 1: Replace `app/page.tsx`**

```tsx
import Link from "next/link";
import { Logo } from "@/components/Logo";
import { StackedCardMark } from "@/components/StackedCardMark";
import styles from "./page.module.css";

export default function Home() {
  return (
    <main className={styles.main}>
      <div className={styles.nav}>
        <Logo />
      </div>
      <section className={styles.hero}>
        <div className={styles.copy}>
          <h1 className={styles.title}>Remember more. Cram less.</h1>
          <p className={styles.lead}>
            recallr turns your notes into decks of flashcards and schedules them
            so you review exactly when you&apos;re about to forget.
          </p>
          <div className={styles.actions}>
            <Link href="/register">
              <span className={styles.primary}>Get started</span>
            </Link>
            <Link href="/login">
              <span className={styles.secondary}>Sign in</span>
            </Link>
          </div>
        </div>
        <div className={styles.art}>
          <StackedCardMark size={220} />
        </div>
      </section>
    </main>
  );
}
```

- [ ] **Step 2: Create `app/page.module.css`**

```css
.main {
  max-width: 1040px;
  margin: 0 auto;
  padding: var(--space-6);
}
.nav {
  padding: var(--space-2) 0 var(--space-8);
}
.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  align-items: center;
  gap: var(--space-8);
  padding-top: var(--space-8);
}
.title {
  font-size: 3.2rem;
  font-weight: 800;
  line-height: 1.02;
  letter-spacing: -1px;
}
.lead {
  margin-top: var(--space-4);
  font-size: 1.15rem;
  max-width: 34ch;
  color: rgba(26, 26, 26, 0.8);
}
.actions {
  margin-top: var(--space-8);
  display: flex;
  gap: var(--space-4);
}
.primary,
.secondary {
  display: inline-block;
  font-family: var(--font-body);
  font-weight: 700;
  border: var(--border);
  border-radius: var(--radius-control);
  padding: 12px 22px;
  box-shadow: var(--shadow-hard-sm);
  transition: transform 0.08s ease, box-shadow 0.08s ease;
}
.primary { background: var(--yellow); }
.secondary { background: var(--cream); }
.primary:hover,
.secondary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hard);
}
.art {
  display: flex;
  justify-content: center;
}
@media (max-width: 720px) {
  .hero { grid-template-columns: 1fr; }
  .art { display: none; }
  .title { font-size: 2.4rem; }
}
```

- [ ] **Step 3: Verify (build + manual)**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit && pnpm --dir apps/web build
```
Expected: pass.
Manual: open `http://localhost:3000/` — Bricolage hero headline, paper background, large `StackedCardMark`, pressable "Get started" (→ `/register`) and "Sign in" (→ `/login`) buttons. Nothing looks like the old Next.js template.

- [ ] **Step 4: Commit**

```bash
git add apps/web/app/page.tsx apps/web/app/page.module.css
git commit -m "feat(web): restyled landing page (Paper Stack hero)"
```

---

### Task 8: Notebooks dashboard

**Files:**
- Create: `apps/web/app/(app)/dashboard/page.tsx` + `page.module.css`

**Interfaces:**
- Consumes: `apiFetch` (`lib/api.ts`), `PaperCard` (Task 3), `Button` (Task 3), `StackedCardMark` (Task 2). Renders inside the guarded `(app)` layout (Task 6).
- Notebook shape from `GET /notebooks`: `type Notebook = { id: string; name: string; description?: string | null }`.

- [ ] **Step 1: Create `app/(app)/dashboard/page.module.css`**

```css
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}
.heading {
  font-size: 2.1rem;
  font-weight: 800;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-6);
}
.card {
  padding: var(--space-6);
  min-height: 130px;
}
.name {
  font-size: 1.25rem;
  font-weight: 700;
}
.desc {
  margin-top: var(--space-2);
  color: rgba(26, 26, 26, 0.7);
  font-size: 0.95rem;
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
.state {
  font-family: var(--font-body);
  color: rgba(26, 26, 26, 0.7);
  padding: var(--space-8) 0;
  text-align: center;
}
```

- [ ] **Step 2: Create `app/(app)/dashboard/page.tsx`**

```tsx
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { PaperCard } from "@/components/ui/PaperCard";
import { StackedCardMark } from "@/components/StackedCardMark";
import { apiFetch } from "@/lib/api";
import styles from "./page.module.css";

type Notebook = { id: string; name: string; description?: string | null };
type Status = "loading" | "ready" | "error";

const COLORS = ["lavender", "teal", "yellow", "cream"] as const;

export default function DashboardPage() {
  const [notebooks, setNotebooks] = useState<Notebook[]>([]);
  const [status, setStatus] = useState<Status>("loading");

  useEffect(() => {
    let cancelled = false;
    apiFetch<Notebook[]>("/notebooks")
      .then((data) => {
        if (!cancelled) {
          setNotebooks(data);
          setStatus("ready");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (status === "loading") {
    return <p className={styles.state}>Loading your notebooks…</p>;
  }
  if (status === "error") {
    return <p className={styles.state}>Couldn&apos;t load your notebooks. Try again.</p>;
  }

  return (
    <div>
      <div className={styles.header}>
        <h1 className={styles.heading}>Your notebooks</h1>
      </div>

      {notebooks.length === 0 ? (
        <div className={styles.empty}>
          <StackedCardMark size={96} />
          <p className={styles.emptyTitle}>No notebooks yet</p>
          <p>Your decks and notes will live inside notebooks.</p>
        </div>
      ) : (
        <div className={styles.grid}>
          {notebooks.map((nb, i) => (
            <PaperCard
              key={nb.id}
              interactive
              color={COLORS[i % COLORS.length]}
              className={styles.card}
            >
              <div className={styles.name}>{nb.name}</div>
              {nb.description && <p className={styles.desc}>{nb.description}</p>}
            </PaperCard>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Verify (build + manual, end-to-end)**

```bash
pnpm --dir apps/web lint && pnpm --dir apps/web exec tsc --noEmit && pnpm --dir apps/web build
```
Expected: pass.
Manual (API must be running at `http://localhost:8080`):
1. Visit `/dashboard` while logged out → redirected to `/login`.
2. Log in → lands on `/dashboard` inside the app shell (TopNav with Logo + avatar initials).
3. If the account has notebooks, they render as colored paper cards that lift on hover; if none, the stacked-card empty state shows.
4. Reload `/dashboard` → session silently rehydrates (no redirect to login) via `/auth/refresh`.

- [ ] **Step 4: Commit**

```bash
git add apps/web/app/\(app\)/dashboard
git commit -m "feat(web): notebooks dashboard with stacked-card empty state"
```

---

## Notes for the implementer

- **`next/font` variable names:** `Bricolage_Grotesque` and `Inter` are the exact `next/font/google` exports. If a requested weight fails to resolve, check the font's available weights in `node_modules/next/dist/compiled/@next/font` guidance or drop the specific weight.
- **Route groups** `(auth)` and `(app)` do not affect URLs — `/login`, `/register`, `/dashboard` are the real paths.
- **`AGENTS.md` reminder:** this is Next.js 16 with breaking changes; if any App Router API behaves unexpectedly, read `node_modules/next/dist/docs/` before improvising.
- **The API returns errors as `{ message }`** (see `apiFetch`), already surfaced as thrown `Error`.
```
