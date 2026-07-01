import styles from "./ProgressBar.module.css";

export function ProgressBar({
  value,
  color = "blue",
}: {
  value: number;
  color?: "blue" | "teal" | "coral";
}) {
  const pct = Math.max(0, Math.min(100, value));
  return (
    <div className={styles.track}>
      <div
        className={`${styles.fill} ${styles[color]}`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}
