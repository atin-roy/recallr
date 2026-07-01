import { Sidebar } from "./Sidebar";
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
      <Sidebar userInitials={userInitials} />
      <main className={styles.main}>{children}</main>
    </div>
  );
}
