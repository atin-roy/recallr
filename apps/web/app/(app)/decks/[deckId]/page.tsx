"use client";

import { use, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { PaperCard } from "@/components/ui/PaperCard";
import { LogoMark } from "@/components/LogoMark";
import { FlashcardForm } from "@/components/flashcards/FlashcardForm";
import { FlashcardGrid } from "@/components/flashcards/FlashcardGrid";
import {
  getDeck,
  getNotebook,
  getFlashcards,
  createFlashcard,
  updateFlashcard,
  deleteFlashcard,
  type Deck,
  type Flashcard,
  type FlashcardInput,
  type Notebook,
} from "@/lib/api";
import styles from "./page.module.css";

type Status = "loading" | "ready" | "error" | "notFound";
type FormState =
  | { kind: "closed" }
  | { kind: "create" }
  | { kind: "edit"; flashcard: Flashcard };

export default function DeckPage({
  params,
}: {
  params: Promise<{ deckId: string }>;
}) {
  const { deckId } = use(params);

  const [deck, setDeck] = useState<Deck | null>(null);
  const [notebook, setNotebook] = useState<Notebook | null>(null);
  const [flashcards, setFlashcards] = useState<Flashcard[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [form, setForm] = useState<FormState>({ kind: "closed" });
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<{
    id: string;
    message: string;
  } | null>(null);

  const load = useCallback(() => {
    let cancelled = false;
    Promise.all([getDeck(deckId), getFlashcards(deckId)])
      .then(([d, cards]) => {
        if (cancelled) return;
        setDeck(d);
        setFlashcards(cards);
        setStatus("ready");
        // Breadcrumb only, if this fails the crumb just says "Notebook".
        if (d.notebookId) {
          getNotebook(d.notebookId)
            .then((nb) => {
              if (!cancelled) setNotebook(nb);
            })
            .catch(() => {});
        }
      })
      .catch((error) => {
        if (cancelled) return;
        const message = error instanceof Error ? error.message : "";
        setStatus(/not.*found|404/i.test(message) ? "notFound" : "error");
      });
    return () => {
      cancelled = true;
    };
  }, [deckId]);

  useEffect(() => load(), [load]);

  function retry() {
    setStatus("loading");
    load();
  }

  function openCreate() {
    setFormError(null);
    setForm({ kind: "create" });
  }

  function openEdit(flashcard: Flashcard) {
    setFormError(null);
    setForm({ kind: "edit", flashcard });
  }

  function closeForm() {
    setForm({ kind: "closed" });
    setFormError(null);
  }

  async function handleSubmit(input: FlashcardInput) {
    setSaving(true);
    setFormError(null);
    try {
      if (form.kind === "create") {
        const created = await createFlashcard(deckId, input);
        setFlashcards((current) => [created, ...current]);
      } else if (form.kind === "edit") {
        const updated = await updateFlashcard(deckId, form.flashcard.id, input);
        setFlashcards((current) =>
          current.map((c) => (c.id === updated.id ? updated : c)),
        );
      }
      closeForm();
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Couldn't save the card.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(flashcard: Flashcard) {
    setDeletingId(flashcard.id);
    setDeleteError(null);
    try {
      await deleteFlashcard(deckId, flashcard.id);
      setFlashcards((current) => current.filter((c) => c.id !== flashcard.id));
      if (form.kind === "edit" && form.flashcard.id === flashcard.id) {
        closeForm();
      }
    } catch (error) {
      setDeleteError({
        id: flashcard.id,
        message:
          error instanceof Error ? error.message : "Couldn't delete the card.",
      });
    } finally {
      setDeletingId(null);
    }
  }

  if (status === "notFound") {
    return (
      <PaperCard className={styles.errorCard} role="alert">
        <p className={styles.errorTitle}>That deck isn&apos;t in the catalog.</p>
        <p className={styles.errorText}>
          The link may be out of date. Head back to your notebooks.
        </p>
        <Link href="/dashboard" className={styles.backLink}>
          Back to notebooks
        </Link>
      </PaperCard>
    );
  }

  const countLine =
    status !== "ready"
      ? "Catalog / Cards"
      : flashcards.length === 0
        ? "No cards yet"
        : `${flashcards.length} card${flashcards.length === 1 ? "" : "s"} in this deck`;

  return (
    <div>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>
            <Link href="/dashboard" className={styles.crumbLink}>
              Notebooks
            </Link>
            {deck?.notebookId && (
              <>
                {" / "}
                <Link
                  href={`/notebooks/${deck.notebookId}`}
                  className={styles.crumbLink}
                >
                  {notebook?.name ?? "Notebook"}
                </Link>
              </>
            )}
            {deck ? ` / ${deck.name}` : ""}
          </p>
          <h1 className={styles.heading}>{deck?.name ?? "Deck"}</h1>
          {deck?.description && (
            <p className={styles.description}>{deck.description}</p>
          )}
          <p className={styles.countLine}>{countLine}</p>
        </div>
        {form.kind === "closed" && status === "ready" && (
          <Button onClick={openCreate}>New card</Button>
        )}
      </header>

      {form.kind === "create" && (
        <FlashcardForm
          mode="create"
          saving={saving}
          error={formError}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      )}
      {form.kind === "edit" && (
        <FlashcardForm
          key={form.flashcard.id}
          mode="edit"
          initial={form.flashcard}
          saving={saving}
          error={formError}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      )}

      {status === "loading" && (
        <p className={styles.state} role="status">
          Shuffling the deck…
        </p>
      )}

      {status === "error" && (
        <PaperCard className={styles.errorCard} role="alert">
          <p className={styles.errorTitle}>Couldn&apos;t open this deck.</p>
          <p className={styles.errorText}>
            The cards didn&apos;t load. Check that you&apos;re signed in, then
            retry.
          </p>
          <Button variant="secondary" onClick={retry}>
            Retry
          </Button>
        </PaperCard>
      )}

      {status === "ready" &&
        (flashcards.length === 0 ? (
          form.kind === "closed" && (
            <div className={styles.empty}>
              <LogoMark size={96} />
              <p className={styles.emptyTitle}>No cards yet</p>
              <p>Flashcards you add to this deck will show up here.</p>
              <Button onClick={openCreate}>Add your first card</Button>
            </div>
          )
        ) : (
          <FlashcardGrid
            flashcards={flashcards}
            onEdit={openEdit}
            onDelete={handleDelete}
            deletingId={deletingId}
            deleteError={deleteError}
          />
        ))}
    </div>
  );
}
