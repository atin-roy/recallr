"use client";

import Link from "next/link";
import { useState } from "react";
import { Logo } from "@/components/Logo";

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <main className="relative min-h-screen flex items-center justify-center px-4 overflow-hidden bg-zinc-950">
      {/* Network SVG background */}
      <div
        className="pointer-events-none absolute inset-0 flex items-center justify-center opacity-40"
        style={{
          maskImage:
            "radial-gradient(ellipse at center, transparent 35%, black 75%)",
          WebkitMaskImage:
            "radial-gradient(ellipse at center, transparent 35%, black 75%)",
        }}
      >
        <svg
          aria-hidden="true"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 800 800"
          className="h-[130vh] min-h-[800px] w-auto max-w-none"
        >
          <g fill="none" stroke="rgba(139,92,246,0.15)" strokeWidth="1">
            <path d="M769 229L1037 260.9M927 880L731 737 520 660 309 538 40 599 295 764 126.5 879.5 40 599-197 493 102 382-31 229 126.5 79.5-69-63" />
            <path d="M-31 229L237 261 390 382 603 493 308.5 537.5 101.5 381.5M370 905L295 764" />
            <path d="M520 660L578 842 731 737 840 599 603 493 520 660 295 764 309 538 390 382 539 269 769 229 577.5 41.5 370 105 295 -36 126.5 79.5 237 261 102 382 40 599 -69 737 127 880" />
            <path d="M520-140L578.5 42.5 731-63M603 493L539 269 237 261 370 105M902 382L539 269M390 382L102 382" />
            <path d="M-222 42L126.5 79.5 370 105 539 269 577.5 41.5 927 80 769 229 902 382 603 493 731 737M295-36L577.5 41.5M578 842L295 764M40-201L127 80M102 382L-261 269" />
          </g>
          <g fill="rgba(167,139,250,0.3)">
            <circle cx="769" cy="229" r="4" />
            <circle cx="539" cy="269" r="4" />
            <circle cx="603" cy="493" r="4" />
            <circle cx="731" cy="737" r="4" />
            <circle cx="520" cy="660" r="4" />
            <circle cx="309" cy="538" r="4" />
            <circle cx="295" cy="764" r="4" />
            <circle cx="40" cy="599" r="4" />
            <circle cx="102" cy="382" r="4" />
            <circle cx="127" cy="80" r="4" />
            <circle cx="370" cy="105" r="4" />
            <circle cx="578" cy="42" r="4" />
            <circle cx="237" cy="261" r="4" />
            <circle cx="390" cy="382" r="4" />
          </g>
        </svg>
      </div>

      {/* Radial gradient overlay — corner warmth on top of SVG */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_10%,rgba(124,58,237,0.28),transparent_30%),radial-gradient(circle_at_85%_85%,rgba(79,70,229,0.22),transparent_35%)]"
      />

      {/* Card */}
      <div className="relative z-10 w-full max-w-sm rounded-2xl border border-white/10 bg-white/5 p-8 shadow-2xl shadow-black/40 backdrop-blur-xl">
        {/* Logo */}
        <div className="mb-6">
          <Logo />
        </div>

        <h1 className="text-2xl font-bold tracking-tight text-white">
          Welcome back
        </h1>
        <p className="mt-1.5 text-sm text-zinc-400">
          Sign in to continue your recall journey.
        </p>

        <form className="mt-7 space-y-4">
          {/* Email */}
          <div className="space-y-1.5">
            <label
              htmlFor="email"
              className="block text-xs font-bold uppercase tracking-[0.18em] text-zinc-400"
            >
              Email
            </label>
            <input
              id="email"
              name="email"
              type="email"
              placeholder="bhaskar@example.com"
              className="w-full rounded-lg border border-white/10 bg-white/[0.04] px-3.5 py-2.5 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none transition focus:border-violet-400/60 focus:bg-white/[0.06] focus:ring-4 focus:ring-violet-500/10"
            />
          </div>

          {/* Password */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label
                htmlFor="password"
                className="block text-xs font-bold uppercase tracking-[0.18em] text-zinc-400"
              >
                Password
              </label>
              <Link
                href="/forgot-password"
                className="text-xs font-medium text-violet-400 transition-colors duration-150 hover:text-violet-300"
              >
                Forgot password?
              </Link>
            </div>
            <div className="relative">
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                placeholder="••••••••"
                className="w-full rounded-lg border border-white/10 bg-white/[0.04] px-3.5 py-2.5 pr-11 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none transition focus:border-violet-400/60 focus:bg-white/[0.06] focus:ring-4 focus:ring-violet-500/10"
              />
              <button
                type="button"
                aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword}
                onClick={() => setShowPassword((v) => !v)}
                className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-zinc-500 transition-colors duration-150 hover:text-zinc-200"
              >
                {showPassword ? (
                  <svg
                    aria-hidden="true"
                    viewBox="0 0 24 24"
                    className="h-4 w-4"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="m2 2 20 20" />
                    <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                    <path d="M8.5 5.5A10.8 10.8 0 0 1 12 5c5 0 8.5 4.5 9.5 7a12.6 12.6 0 0 1-2.3 3.5" />
                    <path d="M15.5 18.5A10.8 10.8 0 0 1 12 19c-5 0-8.5-4.5-9.5-7a12.6 12.6 0 0 1 3-4" />
                  </svg>
                ) : (
                  <svg
                    aria-hidden="true"
                    viewBox="0 0 24 24"
                    className="h-4 w-4"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M2.5 12S6 5 12 5s9.5 7 9.5 7-3.5 7-9.5 7-9.5-7-9.5-7Z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="mt-2 w-full rounded-lg bg-gradient-to-r from-violet-600 to-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-violet-500/25 transition duration-200 hover:from-violet-500 hover:to-indigo-500 hover:shadow-violet-500/40 active:scale-[0.98]"
          >
            Sign in
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-zinc-500">
          Don&apos;t have an account?{" "}
          <Link
            href="/register"
            className="font-medium text-violet-400 transition-colors duration-150 hover:text-violet-300"
          >
            Create one
          </Link>
        </p>
      </div>
    </main>
  );
}
