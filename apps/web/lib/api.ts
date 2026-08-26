import {
  getAccessToken,
  setAccessToken,
  clearAccessToken,
} from "@/lib/accessToken";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

// Shared across concurrent 401s so only one refresh call goes out.
let refreshPromise: Promise<void> | null = null;

async function refreshAccessToken() {
  refreshPromise ??= refreshSession()
    .then(({ accessToken }) => setAccessToken(accessToken))
    .catch((error) => {
      clearAccessToken();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getAccessToken();

  let res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers ?? {}),
    },
    credentials: "include",
  });

  // Access tokens are short-lived; on 401, refresh once and retry.
  if (res.status === 401 && !path.startsWith("/auth/")) {
    await refreshAccessToken();
    res = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${getAccessToken()}`,
        ...(options.headers ?? {}),
      },
      credentials: "include",
    });
  }

  if (!res.ok) {
    let message = "Something went wrong";

    try {
      const error = await res.json();
      message = error.message ?? message;
    } catch {
      // response was not JSON
    }
    throw new Error(message);
  }

  // Some endpoints (e.g. logout) respond with an empty body.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export type Me = { id: string; email: string };

export type NotebookColor =
  | "lavender"
  | "teal"
  | "yellow"
  | "mint"
  | "coral"
  | "sky"
  | "rose"
  | "peach";

export type Notebook = {
  id: string;
  name: string;
  description?: string | null;
  color?: NotebookColor | null;
  createdAt: string;
  updatedAt: string;
};

export function refreshSession() {
  return apiFetch<{ accessToken: string }>("/auth/refresh", { method: "POST" });
}

export function getMe() {
  return apiFetch<Me>("/auth/me");
}

export function logout() {
  return apiFetch<void>("/auth/logout", { method: "POST" });
}

export function getNotebooks() {
  return apiFetch<Notebook[]>("/notebooks");
}

export function createNotebook(input: {
  name: string;
  description?: string;
  color?: NotebookColor;
}) {
  return apiFetch<Notebook>("/notebooks", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateNotebook(
  id: string,
  input: { name: string; description?: string; color?: NotebookColor },
) {
  return apiFetch<Notebook>(`/notebooks/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteNotebook(id: string) {
  return apiFetch<void>(`/notebooks/${id}`, { method: "DELETE" });
}

export type Deck = {
  id: string;
  notebookId: string | null;
  name: string;
  description?: string | null;
  cardCount: number;
  createdAt: string;
  updatedAt: string;
};

export function getNotebook(id: string) {
  return apiFetch<Notebook>(`/notebooks/${id}`);
}

export function getDecksByNotebook(notebookId: string) {
  return apiFetch<Deck[]>(`/notebooks/${notebookId}/decks`);
}

export function createDeck(
  notebookId: string,
  input: { name: string; description?: string },
) {
  return apiFetch<Deck>(`/notebooks/${notebookId}/decks`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateDeck(
  deckId: string,
  input: { name: string; description?: string },
) {
  return apiFetch<Deck>(`/decks/${deckId}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteDeck(deckId: string) {
  return apiFetch<void>(`/decks/${deckId}`, { method: "DELETE" });
}

export function getDeck(deckId: string) {
  return apiFetch<Deck>(`/decks/${deckId}`);
}

export type MatchingPair = { prompt: string; matchValue: string };

// Mirrors the backend's Jackson polymorphism: every request/response carries a
// "type" discriminator, and the payload fields depend on it.
export type FlashcardPayload =
  | { type: "BASIC"; answer: string; reverse: boolean }
  | {
      type: "MCQ";
      options: string[];
      correctOptionIndex: number;
      explanation?: string | null;
    }
  | { type: "CLOZE"; answers: string[] }
  | { type: "TRUE_FALSE"; correctAnswer: boolean; explanation?: string | null }
  | { type: "MATCHING"; pairs: MatchingPair[] }
  | { type: "ORDERING"; items: string[] }
  | { type: "SHORT_ANSWER"; modelAnswer: string };

export type FlashcardType = FlashcardPayload["type"];

export type Flashcard = FlashcardPayload & {
  id: string;
  deckId: string;
  question: string;
  createdAt: string;
  updatedAt: string;
};

export type FlashcardInput = FlashcardPayload & { question: string };

export function getFlashcards(deckId: string) {
  return apiFetch<Flashcard[]>(`/decks/${deckId}/flashcards`);
}

export function createFlashcard(deckId: string, input: FlashcardInput) {
  return apiFetch<Flashcard>(`/decks/${deckId}/flashcards`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateFlashcard(
  deckId: string,
  flashcardId: string,
  input: FlashcardInput,
) {
  return apiFetch<Flashcard>(`/decks/${deckId}/flashcards/${flashcardId}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteFlashcard(deckId: string, flashcardId: string) {
  return apiFetch<void>(`/decks/${deckId}/flashcards/${flashcardId}`, {
    method: "DELETE",
  });
}
