"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { PaperCard } from "@/components/ui/PaperCard";
import { TextField } from "@/components/ui/TextField";
import { LogoMark } from "@/components/LogoMark";
import {
  getNotebooks,
  createNotebook,
  type Notebook,
  type NotebookColor,
} from "@/lib/api";
import styles from "./page.module.css";

type Status = "loading" | "ready" | "error";

const COLORS: readonly NotebookColor[] = [
  "lavender",
  "teal",
  "yellow",
  "mint",
  "coral",
  "sky",
  "rose",
  "peach",
];

const dateFormat = new Intl.DateTimeFormat("en-GB", {
  day: "numeric",
  month: "short",
  year: "numeric",
});

export default function DashboardPage() {
  const [notebooks, setNotebooks] = useState<Notebook[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [formOpen, setFormOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [color, setColor] = useState<NotebookColor>("lavender");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const load = useCallback(() => {
    let cancelled = false;
    getNotebooks()
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

  useEffect(() => load(), [load]);

  function retry() {
    setStatus("loading");
    load();
  }

  function openForm() {
    setFormError(null);
    setColor(COLORS[notebooks.length % COLORS.length]);
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setName("");
    setDescription("");
    setFormError(null);
  }

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      const created = await createNotebook({
        name: name.trim(),
        description: description.trim() || undefined,
        color,
      });
      setNotebooks((current) => [created, ...current]);
      closeForm();
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Couldn't create the notebook.",
      );
    } finally {
      setSaving(false);
    }
  }

  const countLine =
    status !== "ready"
      ? "Catalog / Notebooks"
      : notebooks.length === 0
        ? "Nothing filed yet"
        : `${notebooks.length} notebook${notebooks.length === 1 ? "" : "s"} on file`;

  return (
    <div>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Catalog / Notebooks</p>
          <h1 className={styles.heading}>Your notebooks</h1>
          <p className={styles.countLine}>{countLine}</p>
        </div>
        {!formOpen && <Button onClick={openForm}>New notebook</Button>}
      </header>

      {formOpen && (
        <PaperCard className={styles.formCard}>
          <form onSubmit={handleCreate}>
            <p className={styles.formLabel}>New notebook</p>
            <div className={styles.formFields}>
              <TextField
                id="notebook-name"
                label="Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                maxLength={200}
                autoFocus
              />
              <TextField
                id="notebook-description"
                label="Description (optional)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
              <fieldset className={styles.swatchGroup}>
                <legend className={styles.swatchLegend}>Notebook color</legend>
                <div className={styles.swatches}>
                  {COLORS.map((option) => (
                    <label key={option} className={styles.swatch}>
                      <input
                        type="radio"
                        name="notebook-color"
                        value={option}
                        checked={color === option}
                        onChange={() => setColor(option)}
                        className={styles.swatchInput}
                      />
                      <span
                        className={`${styles.swatchChip} ${styles[option]}`}
                        aria-hidden="true"
                      />
                      <span className={styles.srOnly}>{option}</span>
                    </label>
                  ))}
                </div>
              </fieldset>
            </div>
            {formError && (
              <p className={styles.formError} role="alert">
                {formError}
              </p>
            )}
            <div className={styles.formActions}>
              <Button type="submit" disabled={saving || !name.trim()}>
                {saving ? "Filing…" : "Create notebook"}
              </Button>
              <Button type="button" variant="secondary" onClick={closeForm}>
                Cancel
              </Button>
            </div>
          </form>
        </PaperCard>
      )}

      {status === "loading" && (
        <p className={styles.state} role="status">
          Pulling the drawer…
        </p>
      )}

      {status === "error" && (
        <PaperCard className={styles.errorCard} role="alert">
          <p className={styles.errorTitle}>Couldn&apos;t open the catalog.</p>
          <p className={styles.errorText}>
            The notebooks list didn&apos;t load. Check that you&apos;re signed
            in, then retry.
          </p>
          <Button variant="secondary" onClick={retry}>
            Retry
          </Button>
        </PaperCard>
      )}

      {status === "ready" &&
        (notebooks.length === 0 ? (
          !formOpen && (
            <div className={styles.empty}>
              <LogoMark size={96} />
              <p className={styles.emptyTitle}>No notebooks yet</p>
              <p>Your decks and notes will live inside notebooks.</p>
              <Button onClick={openForm}>Create your first notebook</Button>
            </div>
          )
        ) : (
          <div className={styles.grid}>
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
          </div>
        ))}
    </div>
  );
}
