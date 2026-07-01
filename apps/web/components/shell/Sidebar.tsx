import Link from "next/link";
import { Logo } from "@/components/Logo";
import styles from "./Sidebar.module.css";

export function Sidebar({ userInitials = "" }: { userInitials?: string }) {
  return (
    <nav className={styles.sidebar} aria-label="Primary">
      <Link href="/dashboard" aria-label="recallr home" className={styles.brand}>
        <Logo />
      </Link>

      <ul className={styles.links}>
        <li>
          <Link
            href="/dashboard"
            className={`${styles.link} ${styles.active}`}
            aria-current="page"
          >
            <span className={styles.dot} aria-hidden="true" />
            Notebooks
          </Link>
        </li>
      </ul>

      <span className={styles.avatar} aria-hidden="true">
        {userInitials}
      </span>
    </nav>
  );
}
