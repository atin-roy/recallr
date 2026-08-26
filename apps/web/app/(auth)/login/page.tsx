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
import { setAccessToken } from "@/lib/accessToken";
import styles from "./page.module.css";

type LoginResponse = { accessToken: string };

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    const formData = new FormData(event.currentTarget);
    const email = formData.get("email");
    const password = formData.get("password");
    if (typeof email !== "string" || typeof password !== "string") return;

    try {
      const data = await apiFetch<LoginResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setAccessToken(data.accessToken);
      router.push("/dashboard");
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
        <h1 className={styles.title}>Welcome back</h1>
        <p className={styles.subtitle}>Sign in to continue your recall journey.</p>

        <form className={styles.form} onSubmit={handleSubmit}>
          <TextField
            id="email"
            name="email"
            type="email"
            label="Email"
            placeholder="roy@example.com"
            required
          />
          <PasswordField
            id="password"
            name="password"
            label="Password"
            required
            labelAddon={
              <Link href="/forgot-password" className={styles.link}>
                Forgot password?
              </Link>
            }
          />
          {error && (
            <p className={styles.error} role="alert">
              {error}
            </p>
          )}
          <Button type="submit">Sign in</Button>
        </form>

        <p className={styles.footer}>
          Don&apos;t have an account?{" "}
          <Link href="/register" className={styles.link}>
            Create one
          </Link>
        </p>
      </PaperCard>
    </main>
  );
}
