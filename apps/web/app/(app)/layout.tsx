"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthProvider, useAuth } from "@/components/auth/AuthProvider";
import { AppShell } from "@/components/shell/AppShell";
import { StackedCardMark } from "@/components/StackedCardMark";
import styles from "./loading-shell.module.css";

function Gate({ children }: { children: React.ReactNode }) {
  const { user, status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "guest") router.replace("/login");
  }, [status, router]);

  if (status !== "authed" || !user) {
    return (
      <div className={styles.center}>
        <StackedCardMark size={56} />
        <span className={styles.label}>Loading…</span>
      </div>
    );
  }

  const initials = user.email.slice(0, 2).toUpperCase();
  return <AppShell userInitials={initials}>{children}</AppShell>;
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <Gate>{children}</Gate>
    </AuthProvider>
  );
}
