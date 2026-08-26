"use client";

import { Fragment, useState } from "react";
import type { Flashcard } from "@/lib/api";
import styles from "./FlashCard.module.css";

const TYPE_LABELS: Record<Flashcard["type"], string> = {
  BASIC: "Basic",
  MCQ: "MCQ",
  CLOZE: "Cloze",
  TRUE_FALSE: "True / False",
  MATCHING: "Matching",
  ORDERING: "Ordering",
  SHORT_ANSWER: "Short answer",
};

const TYPE_COLORS: Record<Flashcard["type"], string> = {
  BASIC: "lavender",
  MCQ: "sky",
  CLOZE: "mint",
  TRUE_FALSE: "yellow",
  MATCHING: "rose",
  ORDERING: "peach",
  SHORT_ANSWER: "teal",
};

const CLOZE_MARKER = /\{\{.*?\}\}/g;

// The front of a cloze card must not leak the answers written inside {{...}}.
function clozeBlanked(question: string) {
  return question.replace(CLOZE_MARKER, "____");
}

function ClozeFilled({
  question,
  answers,
}: {
  question: string;
  answers: string[];
}) {
  const parts = question.split(CLOZE_MARKER);
  return (
    <>
      {parts.map((part, i) => (
        <Fragment key={i}>
          {part}
          {i < parts.length - 1 && (
            <mark className={styles.clozeAnswer}>{answers[i] ?? "?"}</mark>
          )}
        </Fragment>
      ))}
    </>
  );
}

function AnswerBody({ flashcard }: { flashcard: Flashcard }) {
  switch (flashcard.type) {
    case "BASIC":
      return (
        <>
          <p className={styles.answerText}>{flashcard.answer}</p>
          {flashcard.reverse && (
            <p className={styles.answerNote}>Also studied in reverse</p>
          )}
        </>
      );
    case "MCQ":
      return (
        <>
          <ol className={styles.optionList}>
            {flashcard.options.map((option, i) => (
              <li
                key={i}
                className={
                  i === flashcard.correctOptionIndex
                    ? styles.correctOption
                    : undefined
                }
              >
                {option}
              </li>
            ))}
          </ol>
          {flashcard.explanation && (
            <p className={styles.answerNote}>{flashcard.explanation}</p>
          )}
        </>
      );
    case "CLOZE":
      return (
        <p className={styles.answerText}>
          <ClozeFilled
            question={flashcard.question}
            answers={flashcard.answers}
          />
        </p>
      );
    case "TRUE_FALSE":
      return (
        <>
          <p className={styles.answerText}>
            {flashcard.correctAnswer ? "True" : "False"}
          </p>
          {flashcard.explanation && (
            <p className={styles.answerNote}>{flashcard.explanation}</p>
          )}
        </>
      );
    case "MATCHING":
      return (
        <ul className={styles.pairList}>
          {flashcard.pairs.map((pair, i) => (
            <li key={i}>
              {pair.prompt} <span className={styles.pairArrow}>→</span>{" "}
              {pair.matchValue}
            </li>
          ))}
        </ul>
      );
    case "ORDERING":
      return (
        <ol className={styles.optionList}>
          {flashcard.items.map((item, i) => (
            <li key={i}>{item}</li>
          ))}
        </ol>
      );
    case "SHORT_ANSWER":
      return <p className={styles.answerText}>{flashcard.modelAnswer}</p>;
  }
}

type FlashCardProps = {
  flashcard: Flashcard;
  onEdit: (flashcard: Flashcard) => void;
  onDelete: (flashcard: Flashcard) => void;
  deleting?: boolean;
  deleteError?: string | null;
};

export function FlashCard({
  flashcard,
  onEdit,
  onDelete,
  deleting = false,
  deleteError = null,
}: FlashCardProps) {
  const [flipped, setFlipped] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const front =
    flashcard.type === "CLOZE"
      ? clozeBlanked(flashcard.question)
      : flashcard.question;

  return (
    <div className={styles.scene}>
      <div className={`${styles.card} ${flipped ? styles.flipped : ""}`}>
        <div className={styles.face} inert={flipped || undefined}>
          <button
            type="button"
            className={styles.flipButton}
            onClick={() => setFlipped(true)}
            aria-label="Show answer"
          />
          <p
            className={`${styles.typeTag} ${styles[TYPE_COLORS[flashcard.type]]}`}
          >
            {TYPE_LABELS[flashcard.type]}
          </p>
          <p className={styles.question}>{front}</p>

          {deleteError && (
            <p className={styles.error} role="alert">
              {deleteError}
            </p>
          )}
          {confirming && (
            <p className={styles.hint}>Gone for good once deleted.</p>
          )}

          <div className={styles.actions}>
            {confirming ? (
              <>
                <button
                  type="button"
                  className={styles.dangerAction}
                  onClick={() => onDelete(flashcard)}
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
                  onClick={() => onEdit(flashcard)}
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

        <div
          className={`${styles.face} ${styles.back}`}
          inert={!flipped || undefined}
        >
          <button
            type="button"
            className={styles.flipButton}
            onClick={() => setFlipped(false)}
            aria-label="Show question"
          />
          <p className={`${styles.typeTag} ${styles.answerTag}`}>Answer</p>
          <div className={styles.answerBody}>
            <AnswerBody flashcard={flashcard} />
          </div>
        </div>
      </div>
    </div>
  );
}
