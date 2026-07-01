import { LogoMark } from "./LogoMark";
import styles from "./Logo.module.css";

export function Logo() {
  return (
    <span className={styles.logo}>
      <LogoMark size={32} />
      <span className={styles.wordmark}>recallr</span>
    </span>
  );
}
