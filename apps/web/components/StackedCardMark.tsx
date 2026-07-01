import styles from "./StackedCardMark.module.css";

export function StackedCardMark({ size = 34 }: { size?: number }) {
  return (
    <span
      className={styles.mark}
      style={{ width: size, height: size * 0.9 }}
      aria-hidden="true"
    >
      <span />
      <span />
      <span />
    </span>
  );
}
