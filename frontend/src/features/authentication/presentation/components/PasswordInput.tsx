import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { ViewIcon, ViewOffSlashIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type { ComponentProps } from "react";
import { useState } from "react";

type PasswordInputProps = Omit<ComponentProps<typeof Input>, "type"> & {
  /**
   * The label text shown above the input.
   * Pass an empty string to skip rendering the label element
   * (useful when you need a custom label row, e.g. with a "Forgot password?" link).
   */
  label: string;
  /** The HTML id used to link the label and input */
  id: string;
};

/**
 * A controlled password input with a built-in visibility toggle button.
 *
 * Usage:
 * ```tsx
 * <PasswordInput
 *   id="login-password"
 *   label="Password"
 *   value={form.password}
 *   onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
 *   disabled={isSubmitting}
 *   placeholder="P@ssw0rd"
 * />
 * ```
 */
export function PasswordInput({ label, id, className = "", ...rest }: PasswordInputProps) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="space-y-2">
      {label && (
        <Label htmlFor={id} className="text-on-surface">
          {label}
        </Label>
      )}

      <div className="relative">
        <Input
          id={id}
          type={showPassword ? "text" : "password"}
          className={`border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60 pr-10 ${className}`}
          {...rest}
        />

        <button
          type="button"
          tabIndex={-1}
          aria-label={showPassword ? "Hide password" : "Show password"}
          onClick={() => setShowPassword((prev) => !prev)}
          className="absolute inset-y-0 right-3 flex items-center text-on-surface-variant hover:text-on-surface transition-colors"
        >
          <HugeiconsIcon
            icon={showPassword ? ViewOffSlashIcon : ViewIcon}
            strokeWidth={2}
            className="size-4"
          />
        </button>
      </div>
    </div>
  );
}
