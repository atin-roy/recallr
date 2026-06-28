"use client";

import { useState } from "react";
import type React from "react";

type PasswordFieldProps = {
  id: string;
  name: string;
  label: string;
  labelAddon?: React.ReactNode;
  required?: boolean;
};

export default function PasswordField({ id, name, label, labelAddon, required }: PasswordFieldProps) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <label
          htmlFor={id}
          className="block text-xs font-bold uppercase tracking-[0.18em] text-zinc-400"
        >
          {label}
        </label>
        {labelAddon}
      </div>
      <div className="relative">
        <input
          id={id}
          name={name}
          type={showPassword ? "text" : "password"}
          placeholder="••••••••"
          required={required}
          className="w-full rounded-lg border border-white/10 bg-white/[0.04] px-3.5 py-2.5 pr-11 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none transition focus:border-violet-400/60 focus:bg-white/[0.06] focus:ring-4 focus:ring-violet-500/10"
        />
        <button
          type="button"
          aria-label={showPassword ? `Hide ${label}` : `Show ${label}`}
          aria-pressed={showPassword}
          onClick={() => setShowPassword((current) => !current)}
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
  );
}
