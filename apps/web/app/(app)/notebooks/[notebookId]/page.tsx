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

  function retry() {
    setStatus("loading");
    load();
  }

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
          key={form.deck.id}
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
          <Button variant="secondary" onClick={retry}>
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
