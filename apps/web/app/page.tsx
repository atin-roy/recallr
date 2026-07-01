import Link from "next/link";
import { Logo } from "@/components/Logo";
import { StackedCardMark } from "@/components/StackedCardMark";
import styles from "./page.module.css";

export default function Home() {
  return (
    <main className={styles.main}>
      <div className={styles.nav}>
        <Logo />
      </div>
      <section className={styles.hero}>
        <div className={styles.copy}>
          <h1 className={styles.title}>Remember more. Cram less.</h1>
          <p className={styles.lead}>
            recallr turns your notes into decks of flashcards and schedules them
            so you review exactly when you&apos;re about to forget.
          </p>
          <div className={styles.actions}>
            <Link href="/register">
              <span className={styles.primary}>Get started</span>
            </Link>
            <Link href="/login">
              <span className={styles.secondary}>Sign in</span>
            </Link>
          </div>
        </div>
        <div className={styles.art}>
          <StackedCardMark size={220} />
        </div>
      </section>
    </main>
  );
}
