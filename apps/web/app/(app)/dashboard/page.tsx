"use client";

import { useEffect, useState } from "react";
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
    return (
      <p className={styles.state} role="status">
        Loading your notebooks…
      </p>
    );
  }
  if (status === "error") {
    return (
      <p className={styles.state} role="alert">
        Couldn&apos;t load your notebooks. Try again.
      </p>
    );
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
