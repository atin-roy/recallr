"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/Logo";
import AuthBackground from "@/components/auth/AuthBackground";
import AuthCard from "@/components/auth/AuthCard";
import TextField from "@/components/auth/TextField";
import PasswordField from "@/components/auth/PasswordField";
import { apiFetch } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (event: React.SubmitEvent<HTMLFormElement>) => {
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
    <main className="relative min-h-screen flex items-center justify-center px-4 overflow-hidden bg-zinc-950">
      <AuthBackground />

      <AuthCard>
        <div className="mb-6">
          <Logo />
        </div>

        <h1 className="text-2xl font-bold tracking-tight text-white">
          Create an account
        </h1>
        <p className="mt-1.5 text-sm text-zinc-400">
          Turn study material into active recall.
        </p>

        <form className="mt-7 space-y-4" onSubmit={handleSubmit}>
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
          />

          <PasswordField
            id="confirmPassword"
            name="confirmPassword"
            label="Confirm Password"
            required
          />

          {success && (
            <p className="rounded-lg border border-green-500/20 bg-green-500/10 px-3.5 py-2.5 text-sm text-green-400">
              Account created! Redirecting to login…
            </p>
          )}

          {error && (
            <p className="rounded-lg border border-red-500/20 bg-red-500/10 px-3.5 py-2.5 text-sm text-red-400">
              {error}
            </p>
          )}

          <button
            type="submit"
            className="mt-2 w-full rounded-lg bg-linear-to-r from-violet-600 to-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-violet-500/25 transition duration-200 hover:from-violet-500 hover:to-indigo-500 hover:shadow-violet-500/40 active:scale-[0.98]"
          >
            Create account
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-zinc-500">
          Already have an account?{" "}
          <Link
            href="/login"
            className="font-medium text-violet-400 transition-colors duration-150 hover:text-violet-300"
          >
            Sign in
          </Link>
        </p>
      </AuthCard>
    </main>
  );
}
