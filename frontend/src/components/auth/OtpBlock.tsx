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
    <div className="p-2 sm:p-4">
      <InputOTP
        value={value}
        onChange={(val) => onChange(val.replace(/\D/g, ""))}
        maxLength={length}
        containerClassName="justify-center"
        disabled={disabled}
      >
        <InputOTPGroup className="gap-3">
          {Array.from({ length }, (_, index) => (
            <InputOTPSlot
              key={index}
              index={index}
              className="size-14 rounded-xl border-[3px] border-gray-400 dark:border-slate-700 bg-white dark:bg-slate-900 text-2xl font-black text-gray-900 dark:text-white shadow-sm transition-all duration-200 data-[active=true]:border-blue-600 dark:data-[active=true]:border-blue-400 data-[active=true]:bg-blue-50/50 dark:data-[active=true]:bg-blue-900/20 data-[active=true]:ring-4 data-[active=true]:ring-blue-600/15 data-[active=true]:scale-105"
            />
          ))}
        </InputOTPGroup>
      </InputOTP>
    </div>
  );
}
