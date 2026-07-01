import Link from "next/link";
import { Logo } from "@/components/Logo";
import styles from "./TopNav.module.css";

export function TopNav({ userInitials = "" }: { userInitials?: string }) {
  return (
    <nav className={styles.nav}>
      <Link href="/dashboard" aria-label="recallr home">
        <Logo />
      </Link>
      <div className={styles.links}>
        <Link href="/dashboard" className={`${styles.link} ${styles.active}`}>
          Notebooks
        </Link>
        <span className={styles.avatar}>{userInitials}</span>
      </div>
    </nav>
  );
}
