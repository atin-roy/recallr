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
