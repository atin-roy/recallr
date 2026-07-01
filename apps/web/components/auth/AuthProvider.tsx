"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { getAccessToken, setAccessToken } from "@/lib/accessToken";
import { refreshSession, getMe, type Me } from "@/lib/api";

type AuthState = { user: Me | null; status: "loading" | "authed" | "guest" };

const AuthContext = createContext<AuthState>({ user: null, status: "loading" });

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({ user: null, status: "loading" });

  useEffect(() => {
    let cancelled = false;
    async function hydrate() {
      try {
        if (!getAccessToken()) {
          const { accessToken } = await refreshSession();
          setAccessToken(accessToken);
        }
        const user = await getMe();
        if (!cancelled) setState({ user, status: "authed" });
      } catch {
        if (!cancelled) setState({ user: null, status: "guest" });
      }
    }
    hydrate();
    return () => {
      cancelled = true;
    };
  }, []);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}
