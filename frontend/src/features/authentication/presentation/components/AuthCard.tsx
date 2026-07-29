import { Card, CardContent } from "@/shared/components/ui/card";
import { HugeiconsIcon } from "@hugeicons/react";
import type { ComponentProps, ReactNode } from "react";

type AuthCardProps = {
  /** Optional icon to display in the coloured circle at the top */
  icon?: ComponentProps<typeof HugeiconsIcon>["icon"];
  /** The main heading text */
  heading: string;
  /** A short description shown below the heading */
  description?: string;
  /** Optional small line (e.g. "Code sent to: user@example.com") */
  hint?: ReactNode;
  /** Form and action elements */
  children: ReactNode;
};

/**
 * The standard card shell used by every single-column auth page
 * (Forgot Password, Verify OTP, Reset OTP, Reset Password).
 *
 * Provides consistent icon, heading, description, and hint layout
 * so each page only needs to supply content.
 */
export function AuthCard({ icon, heading, description, hint, children }: AuthCardProps) {
  return (
    <Card className="w-full rounded-3xl border border-outline-variant bg-surface-container-lowest shadow-lg p-6 sm:p-8">
      <CardContent className="space-y-6">
        {/* Optional icon circle */}
        {icon && (
          <div className="flex justify-center">
            <div className="flex size-16 items-center justify-center rounded-full bg-primary/10">
              <HugeiconsIcon
                icon={icon}
                strokeWidth={1.5}
                className="size-8 text-primary"
              />
            </div>
          </div>
        )}

        {/* Heading block */}
        <div className="space-y-2 text-center">
          <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
            {heading}
          </h1>

          {description && (
            <p className="text-sm text-on-surface-variant">{description}</p>
          )}

          {hint && <div className="text-xs text-on-surface-variant">{hint}</div>}
        </div>

        {/* Page-specific content */}
        {children}
      </CardContent>
    </Card>
  );
}
