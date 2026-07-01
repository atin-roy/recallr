import { StackedCardMark } from "./StackedCardMark";
import styles from "./Logo.module.css";

export function Logo() {
  return (
    <span className={styles.logo}>
      <StackedCardMark size={32} />
      <span className={styles.wordmark}>recallr</span>
    </span>
  );
}
