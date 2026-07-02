import { LogoMark } from "./LogoMark";
import styles from "./Logo.module.css";

export function Logo({ size = "md" }: { size?: "md" | "lg" }) {
  return (
    <span className={`${styles.logo} ${size === "lg" ? styles.lg : ""}`}>
      <LogoMark size={size === "lg" ? 44 : 32} />
      <span className={styles.wordmark}>recallr</span>
    </span>
  );
}
