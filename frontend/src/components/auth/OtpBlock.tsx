import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";

type OtpBlockProps = {
  /** Current OTP value */
  value: string;
  /** Called whenever the user changes a digit */
  onChange: (value: string) => void;
  /** Total number of OTP digits (default: 6) */
  length?: number;
  /** Disable all slots (e.g. while verifying or after max attempts) */
  disabled?: boolean;
};

/**
 * Renders a row of OTP digit slots inside a styled container.
 *
 * Usage:
 * ```tsx
 * <OtpBlock
 *   value={otp}
 *   onChange={(val) => setOtp(val.replace(/\D/g, ""))}
 *   disabled={isVerifying || hasReachedMaxAttempts}
 * />
 * ```
 */
export function OtpBlock({ value, onChange, length = 6, disabled = false }: OtpBlockProps) {
  return (
    <div className="rounded-2xl border border-outline-variant bg-surface-container p-4 sm:p-5">
      <InputOTP
        value={value}
        onChange={(val) => onChange(val.replace(/\D/g, ""))}
        maxLength={length}
        containerClassName="justify-center"
        disabled={disabled}
      >
        <InputOTPGroup className="gap-2 rounded-none">
          {Array.from({ length }, (_, index) => (
            <InputOTPSlot
              key={index}
              index={index}
              className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
            />
          ))}
        </InputOTPGroup>
      </InputOTP>
    </div>
  );
}
