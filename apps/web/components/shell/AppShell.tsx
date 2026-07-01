import { TopNav } from "./TopNav";
import styles from "./AppShell.module.css";

export function AppShell({
  userInitials,
  children,
}: {
  userInitials?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={styles.shell}>
      <div className={styles.container}>
        <TopNav userInitials={userInitials} />
        <div className={styles.content}>{children}</div>
      </div>
    </div>
  );
}
