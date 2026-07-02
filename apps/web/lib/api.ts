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
