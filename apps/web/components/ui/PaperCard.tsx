import styles from "./PaperCard.module.css";

type Props = React.HTMLAttributes<HTMLDivElement> & {
  color?: "cream" | "lavender" | "teal" | "yellow";
  interactive?: boolean;
};

export function PaperCard({
  color = "cream",
  interactive = false,
  className,
  children,
  ...rest
}: Props) {
  const classes = [
    styles.card,
    styles[color],
    interactive ? styles.interactive : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <div className={classes} {...rest}>
      {children}
    </div>
  );
}
