"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/Logo";
import { PaperCard } from "@/components/ui/PaperCard";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { PasswordField } from "@/components/ui/PasswordField";
import { apiFetch } from "@/lib/api";
import styles from "./page.module.css";

export default function RegisterPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    const formData = new FormData(event.currentTarget);

    const email = formData.get("email");
    const password = formData.get("password");
    const confirmPassword = formData.get("confirmPassword");
    if (
      typeof email !== "string" ||
      typeof password !== "string" ||
      typeof confirmPassword !== "string"
    )
      return;

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    try {
      await apiFetch("/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setSuccess(true);
      setTimeout(() => router.push("/login"), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    }
  };

  return (
    <main className={styles.main}>
      <PaperCard className={styles.card}>
        <div className={styles.logo}>
          <Logo />
        </div>
        <h1 className={styles.title}>Create your account</h1>
        <p className={styles.subtitle}>Start building your recall habit.</p>

        <form className={styles.form} onSubmit={handleSubmit}>
          <TextField
            id="email"
            name="email"
            type="email"
            label="Email"
            placeholder="roy@example.com"
            required
          />
          <PasswordField id="password" name="password" label="Password" required />
          <PasswordField
            id="confirmPassword"
            name="confirmPassword"
            label="Confirm Password"
            required
          />
          {success && (
            <p className={styles.success}>Account created! Redirecting to login&hellip;</p>
          )}
          {error && <p className={styles.error}>{error}</p>}
          <Button type="submit">Create account</Button>
        </form>

        <p className={styles.footer}>
          Already have an account?{" "}
          <Link href="/login" className={styles.link}>
            Sign in
          </Link>
        </p>
      </PaperCard>
    </main>
  );
}
