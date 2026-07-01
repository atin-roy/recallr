"use client";

import { useState } from "react";
import styles from "./PasswordField.module.css";

type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  id: string;
  labelAddon?: React.ReactNode;
};

export function PasswordField({ label, id, labelAddon, ...rest }: Props) {
  const [show, setShow] = useState(false);
  return (
    <div className={styles.field}>
      <div className={styles.labelRow}>
        <label className={styles.label} htmlFor={id}>
          {label}
        </label>
        {labelAddon}
      </div>
      <div className={styles.wrap}>
        <input
          id={id}
          type={show ? "text" : "password"}
          className={styles.input}
          {...rest}
        />
        <button
          type="button"
          className={styles.toggle}
          onClick={() => setShow((s) => !s)}
        >
          {show ? "Hide" : "Show"}
        </button>
      </div>
    </div>
  );
}
