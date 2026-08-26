"use client";

import type { Flashcard } from "@/lib/api";
import { FlashCard } from "./FlashCard";
import styles from "./FlashcardGrid.module.css";

type FlashcardGridProps = {
  flashcards: Flashcard[];
  onEdit: (flashcard: Flashcard) => void;
  onDelete: (flashcard: Flashcard) => void;
  deletingId: string | null;
  deleteError: { id: string; message: string } | null;
};

export function FlashcardGrid({
  flashcards,
  onEdit,
  onDelete,
  deletingId,
  deleteError,
}: FlashcardGridProps) {
  return (
    <div className={styles.grid}>
      {flashcards.map((card) => (
        <FlashCard
          key={card.id}
          flashcard={card}
          onEdit={onEdit}
          onDelete={onDelete}
          deleting={deletingId === card.id}
          deleteError={deleteError?.id === card.id ? deleteError.message : null}
        />
      ))}
    </div>
  );
}
