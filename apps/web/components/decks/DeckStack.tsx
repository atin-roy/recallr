"use client";

import { useState } from "react";
import Link from "next/link";
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
          <Link href={`/decks/${deck.id}`} className={styles.topLink}>
            <p className={styles.meta}>
              Updated {dateFormat.format(new Date(deck.updatedAt))}
            </p>
            <div className={styles.name}>{deck.name}</div>
            {deck.description && (
              <p className={styles.desc}>{deck.description}</p>
            )}
            <p className={styles.countLine}>
              <span className={styles.countPill}>
                {deck.cardCount} card{deck.cardCount === 1 ? "" : "s"}
              </span>
            </p>
          </Link>

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
