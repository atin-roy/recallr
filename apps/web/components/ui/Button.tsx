import styles from "./Button.module.css";

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary";
};

export function Button({ variant = "primary", className, ...rest }: Props) {
  const classes = [styles.btn, styles[variant], className ?? ""]
    .filter(Boolean)
    .join(" ");
  return <button className={classes} {...rest} />;
}
