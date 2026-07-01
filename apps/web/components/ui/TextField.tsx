import styles from "./TextField.module.css";

type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  id: string;
};

export function TextField({ label, id, ...rest }: Props) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      <input id={id} className={styles.input} {...rest} />
    </div>
  );
}
