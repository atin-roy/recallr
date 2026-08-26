"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/Logo";
import { logout } from "@/lib/api";
import { clearAccessToken } from "@/lib/accessToken";
import styles from "./Sidebar.module.css";

const upcoming = [
  { name: "Review", dot: "dotCoral" },
  { name: "Decks", dot: "dotLavender" },
  { name: "Notes", dot: "dotMint" },
] as const;

export function Sidebar({ userInitials = "" }: { userInitials?: string }) {
  const router = useRouter();
  const [signingOut, setSigningOut] = useState(false);

  async function handleSignOut() {
    setSigningOut(true);
    try {
      await logout();
    } catch {
      // Clear the local session even if the server call fails.
    }
    clearAccessToken();
    router.replace("/login");
  }

  return (
    <nav className={styles.sidebar} aria-label="Primary">
      <Link href="/dashboard" aria-label="recallr home" className={styles.brand}>
        <Logo size="lg" />
      </Link>

      <div className={styles.catalog}>
        <p className={styles.railLabel}>Catalog</p>
        <ul className={styles.links}>
          <li>
            <Link
              href="/dashboard"
              className={`${styles.link} ${styles.active}`}
              aria-current="page"
            >
              <span
                className={`${styles.dot} ${styles.dotTeal}`}
                aria-hidden="true"
              />
              Notebooks
            </Link>
          </li>
          {upcoming.map((item) => (
            <li key={item.name}>
              <span
                className={`${styles.link} ${styles.soonRow}`}
                aria-disabled="true"
              >
                <span
                  className={`${styles.dot} ${styles[item.dot]}`}
                  aria-hidden="true"
                />
                {item.name}
                <span className={styles.soonTag}>soon</span>
              </span>
            </li>
          ))}
        </ul>
      </div>

      <div className={styles.userBlock}>
        <span className={styles.avatar} aria-hidden="true">
          {userInitials}
        </span>
        <button
          type="button"
          className={styles.signOut}
          onClick={handleSignOut}
          disabled={signingOut}
        >
          {signingOut ? "Signing out…" : "Sign out"}
        </button>
      </div>
    </nav>
  );
}
