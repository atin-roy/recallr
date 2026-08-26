"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { PaperCard } from "@/components/ui/PaperCard";
import { TextField } from "@/components/ui/TextField";
import type {
  Flashcard,
  FlashcardInput,
  FlashcardType,
  MatchingPair,
} from "@/lib/api";
import styles from "./FlashcardForm.module.css";

const TYPES: readonly { value: FlashcardType; label: string }[] = [
  { value: "BASIC", label: "Basic" },
  { value: "MCQ", label: "MCQ" },
  { value: "CLOZE", label: "Cloze" },
  { value: "TRUE_FALSE", label: "True / False" },
  { value: "MATCHING", label: "Matching" },
  { value: "ORDERING", label: "Ordering" },
  { value: "SHORT_ANSWER", label: "Short answer" },
];

const CLOZE_MARKER = /\{\{.*?\}\}/g;

type FlashcardFormProps = {
  mode: "create" | "edit";
  initial?: Flashcard;
  saving?: boolean;
  error?: string | null;
  onSubmit: (input: FlashcardInput) => void;
  onCancel: () => void;
};

export function FlashcardForm({
  mode,
  initial,
  saving = false,
  error = null,
  onSubmit,
  onCancel,
}: FlashcardFormProps) {
  const [type, setType] = useState<FlashcardType>(initial?.type ?? "BASIC");
  const [question, setQuestion] = useState(initial?.question ?? "");
  const [answer, setAnswer] = useState(
    initial?.type === "BASIC" ? initial.answer : "",
  );
  const [reverse, setReverse] = useState(
    initial?.type === "BASIC" ? initial.reverse : false,
  );
  const [options, setOptions] = useState<string[]>(
    initial?.type === "MCQ" ? initial.options : ["", ""],
  );
  const [correctOptionIndex, setCorrectOptionIndex] = useState(
    initial?.type === "MCQ" ? initial.correctOptionIndex : 0,
  );
  const [explanation, setExplanation] = useState(
    initial?.type === "MCQ" || initial?.type === "TRUE_FALSE"
      ? (initial.explanation ?? "")
      : "",
  );
  const [clozeAnswers, setClozeAnswers] = useState<string[]>(
    initial?.type === "CLOZE" ? initial.answers : [""],
  );
  const [correctAnswer, setCorrectAnswer] = useState(
    initial?.type === "TRUE_FALSE" ? initial.correctAnswer : true,
  );
  const [pairs, setPairs] = useState<MatchingPair[]>(
    initial?.type === "MATCHING"
      ? initial.pairs
      : [
          { prompt: "", matchValue: "" },
          { prompt: "", matchValue: "" },
        ],
  );
  const [items, setItems] = useState<string[]>(
    initial?.type === "ORDERING" ? initial.items : ["", ""],
  );
  const [modelAnswer, setModelAnswer] = useState(
    initial?.type === "SHORT_ANSWER" ? initial.modelAnswer : "",
  );

  const clozeMarkerCount = (question.match(CLOZE_MARKER) ?? []).length;
  const clozeAnswerCount = clozeAnswers.filter((a) => a.trim()).length;
  const clozeMismatch =
    type === "CLOZE" && clozeMarkerCount !== clozeAnswerCount;

  const typeValid =
    type === "BASIC"
      ? Boolean(answer.trim())
      : type === "MCQ"
        ? options.length >= 2 &&
          options.every((o) => o.trim()) &&
          correctOptionIndex < options.length
        : type === "CLOZE"
          ? clozeAnswerCount >= 1
          : type === "TRUE_FALSE"
            ? true
            : type === "MATCHING"
              ? pairs.filter((p) => p.prompt.trim() && p.matchValue.trim())
                  .length >= 2
              : type === "ORDERING"
                ? items.filter((i) => i.trim()).length >= 2
                : Boolean(modelAnswer.trim());
  const canSubmit = Boolean(question.trim()) && typeValid;

  function buildInput(): FlashcardInput {
    const q = question.trim();
    switch (type) {
      case "BASIC":
        return { type, question: q, answer: answer.trim(), reverse };
      case "MCQ":
        return {
          type,
          question: q,
          options: options.map((o) => o.trim()),
          correctOptionIndex,
          explanation: explanation.trim() || undefined,
        };
      case "CLOZE":
        return {
          type,
          question: q,
          answers: clozeAnswers.map((a) => a.trim()).filter(Boolean),
        };
      case "TRUE_FALSE":
        return {
          type,
          question: q,
          correctAnswer,
          explanation: explanation.trim() || undefined,
        };
      case "MATCHING":
        return {
          type,
          question: q,
          pairs: pairs
            .map((p) => ({
              prompt: p.prompt.trim(),
              matchValue: p.matchValue.trim(),
            }))
            .filter((p) => p.prompt && p.matchValue),
        };
      case "ORDERING":
        return {
          type,
          question: q,
          items: items.map((i) => i.trim()).filter(Boolean),
        };
      case "SHORT_ANSWER":
        return { type, question: q, modelAnswer: modelAnswer.trim() };
    }
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    onSubmit(buildInput());
  }

  const typeLabel = TYPES.find((t) => t.value === type)?.label ?? type;

  return (
    <PaperCard className={styles.formCard}>
      <form onSubmit={handleSubmit}>
        <p className={styles.formLabel}>
          {mode === "create" ? "New card" : `Edit ${typeLabel} card`}
        </p>
        <div className={styles.formFields}>
          {mode === "create" && (
            <fieldset className={styles.choiceGroup}>
              <legend className={styles.choiceLegend}>Card type</legend>
              <div className={styles.choices}>
                {TYPES.map((option) => (
                  <label key={option.value} className={styles.choice}>
                    <input
                      type="radio"
                      name="flashcard-type"
                      value={option.value}
                      checked={type === option.value}
                      onChange={() => setType(option.value)}
                      className={styles.choiceInput}
                    />
                    <span className={styles.choiceChip}>{option.label}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}

          <TextField
            id="flashcard-question"
            label={
              type === "CLOZE"
                ? "Question (mark blanks as {{answer}})"
                : "Question"
            }
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            required
            autoFocus
          />

          {type === "BASIC" && (
            <>
              <TextField
                id="basic-answer"
                label="Answer"
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
              />
              <label className={styles.checkboxLabel}>
                <input
                  type="checkbox"
                  checked={reverse}
                  onChange={(e) => setReverse(e.target.checked)}
                  className={styles.checkbox}
                />
                Also study in reverse
              </label>
            </>
          )}

          {type === "MCQ" && (
            <>
              <div className={styles.rows}>
                {options.map((option, index) => (
                  <div key={index} className={styles.row}>
                    <input
                      type="radio"
                      name="mcq-correct"
                      checked={correctOptionIndex === index}
                      onChange={() => setCorrectOptionIndex(index)}
                      className={styles.correctRadio}
                      aria-label={`Option ${index + 1} is the correct answer`}
                    />
                    <div className={styles.rowField}>
                      <TextField
                        id={`mcq-option-${index}`}
                        label={`Option ${index + 1}${
                          correctOptionIndex === index ? " (correct)" : ""
                        }`}
                        value={option}
                        onChange={(e) =>
                          setOptions(
                            options.map((o, i) =>
                              i === index ? e.target.value : o,
                            ),
                          )
                        }
                      />
                    </div>
                    <button
                      type="button"
                      className={styles.removeRow}
                      onClick={() => {
                        setOptions(options.filter((_, i) => i !== index));
                        if (correctOptionIndex === index)
                          setCorrectOptionIndex(0);
                        else if (correctOptionIndex > index)
                          setCorrectOptionIndex(correctOptionIndex - 1);
                      }}
                      disabled={options.length <= 2}
                      aria-label={`Remove option ${index + 1}`}
                    >
                      ✕
                    </button>
                  </div>
                ))}
                <button
                  type="button"
                  className={styles.addRow}
                  onClick={() => setOptions([...options, ""])}
                >
                  + Add option
                </button>
              </div>
              <TextField
                id="mcq-explanation"
                label="Explanation (optional)"
                value={explanation}
                onChange={(e) => setExplanation(e.target.value)}
              />
            </>
          )}

          {type === "CLOZE" && (
            <div className={styles.rows}>
              {clozeAnswers.map((value, index) => (
                <div key={index} className={styles.row}>
                  <div className={styles.rowField}>
                    <TextField
                      id={`cloze-answer-${index}`}
                      label={`Blank ${index + 1} answer`}
                      value={value}
                      onChange={(e) =>
                        setClozeAnswers(
                          clozeAnswers.map((a, i) =>
                            i === index ? e.target.value : a,
                          ),
                        )
                      }
                    />
                  </div>
                  <button
                    type="button"
                    className={styles.removeRow}
                    onClick={() =>
                      setClozeAnswers(clozeAnswers.filter((_, i) => i !== index))
                    }
                    disabled={clozeAnswers.length <= 1}
                    aria-label={`Remove blank ${index + 1} answer`}
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                type="button"
                className={styles.addRow}
                onClick={() => setClozeAnswers([...clozeAnswers, ""])}
              >
                + Add answer
              </button>
            </div>
          )}

          {type === "TRUE_FALSE" && (
            <>
              <fieldset className={styles.choiceGroup}>
                <legend className={styles.choiceLegend}>Correct answer</legend>
                <div className={styles.choices}>
                  <label className={styles.choice}>
                    <input
                      type="radio"
                      name="tf-answer"
                      checked={correctAnswer}
                      onChange={() => setCorrectAnswer(true)}
                      className={styles.choiceInput}
                    />
                    <span className={styles.choiceChip}>True</span>
                  </label>
                  <label className={styles.choice}>
                    <input
                      type="radio"
                      name="tf-answer"
                      checked={!correctAnswer}
                      onChange={() => setCorrectAnswer(false)}
                      className={styles.choiceInput}
                    />
                    <span className={styles.choiceChip}>False</span>
                  </label>
                </div>
              </fieldset>
              <TextField
                id="tf-explanation"
                label="Explanation (optional)"
                value={explanation}
                onChange={(e) => setExplanation(e.target.value)}
              />
            </>
          )}

          {type === "MATCHING" && (
            <div className={styles.rows}>
              {pairs.map((pair, index) => (
                <div key={index} className={styles.row}>
                  <div className={styles.rowField}>
                    <TextField
                      id={`pair-prompt-${index}`}
                      label={`Prompt ${index + 1}`}
                      value={pair.prompt}
                      onChange={(e) =>
                        setPairs(
                          pairs.map((p, i) =>
                            i === index ? { ...p, prompt: e.target.value } : p,
                          ),
                        )
                      }
                    />
                  </div>
                  <div className={styles.rowField}>
                    <TextField
                      id={`pair-match-${index}`}
                      label={`Match ${index + 1}`}
                      value={pair.matchValue}
                      onChange={(e) =>
                        setPairs(
                          pairs.map((p, i) =>
                            i === index
                              ? { ...p, matchValue: e.target.value }
                              : p,
                          ),
                        )
                      }
                    />
                  </div>
                  <button
                    type="button"
                    className={styles.removeRow}
                    onClick={() => setPairs(pairs.filter((_, i) => i !== index))}
                    disabled={pairs.length <= 2}
                    aria-label={`Remove pair ${index + 1}`}
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                type="button"
                className={styles.addRow}
                onClick={() =>
                  setPairs([...pairs, { prompt: "", matchValue: "" }])
                }
              >
                + Add pair
              </button>
            </div>
          )}

          {type === "ORDERING" && (
            <div className={styles.rows}>
              {items.map((item, index) => (
                <div key={index} className={styles.row}>
                  <div className={styles.rowField}>
                    <TextField
                      id={`ordering-item-${index}`}
                      label={`Item ${index + 1}`}
                      value={item}
                      onChange={(e) =>
                        setItems(
                          items.map((it, i) =>
                            i === index ? e.target.value : it,
                          ),
                        )
                      }
                    />
                  </div>
                  <button
                    type="button"
                    className={styles.removeRow}
                    onClick={() => setItems(items.filter((_, i) => i !== index))}
                    disabled={items.length <= 2}
                    aria-label={`Remove item ${index + 1}`}
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                type="button"
                className={styles.addRow}
                onClick={() => setItems([...items, ""])}
              >
                + Add item
              </button>
              <p className={styles.fieldNote}>
                List the items in correct order.
              </p>
            </div>
          )}

          {type === "SHORT_ANSWER" && (
            <TextField
              id="short-answer"
              label="Model answer"
              value={modelAnswer}
              onChange={(e) => setModelAnswer(e.target.value)}
            />
          )}
        </div>

        {clozeMismatch && (
          <p className={styles.formWarning}>
            {clozeMarkerCount} blank marker
            {clozeMarkerCount === 1 ? "" : "s"} in the question,{" "}
            {clozeAnswerCount} answer{clozeAnswerCount === 1 ? "" : "s"}. They
            pair up in order.
          </p>
        )}
        {error && (
          <p className={styles.formError} role="alert">
            {error}
          </p>
        )}
        <div className={styles.formActions}>
          <Button type="submit" disabled={saving || !canSubmit}>
            {saving
              ? mode === "create"
                ? "Filing…"
                : "Saving…"
              : mode === "create"
                ? "Add card"
                : "Save changes"}
          </Button>
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        </div>
      </form>
    </PaperCard>
  );
}
