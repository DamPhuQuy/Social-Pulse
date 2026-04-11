import Header from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { resendOtp, verifyResetOtp } from "@/services/auth/authService";
import {
  ArrowRight01Icon,
  Loading03Icon,
  SquareLock02Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 60;
const MAX_FAILED_ATTEMPTS = 3;
const RESET_VERIFIED_FLAG_KEY = "pendingResetVerified";

function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
}

export default function ResetPasswordOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const email = useMemo(() => {
    const stateValue = location.state as { email?: string } | null;
    const fromState = stateValue?.email?.trim() ?? "";
    if (fromState) return fromState;
    return (
      new URLSearchParams(location.search).get("email")?.trim() ??
      sessionStorage.getItem("pendingResetEmail")?.trim() ??
      ""
    );
  }, [location.search, location.state]);

  const [otp, setOtp] = useState("");
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [isVerified, setIsVerified] = useState(false);

  const hasReachedMaxAttempts = failedAttempts >= MAX_FAILED_ATTEMPTS;

  // Lưu email vào sessionStorage
  useEffect(() => {
    if (email) sessionStorage.setItem("pendingResetEmail", email);
  }, [email]);

  // Countdown resend timer
  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = globalThis.setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          globalThis.clearInterval(id);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => globalThis.clearInterval(id);
  }, [secondsLeft]);

  // Redirect sang trang đặt mật khẩu mới sau khi OTP hợp lệ
  useEffect(() => {
    if (!isVerified) return;
    const id = globalThis.setTimeout(() => {
      navigate(
        `${PATHS.RESET_PASSWORD_NEW}?email=${encodeURIComponent(email)}`,
        { state: { email, verified: true } },
      );
    }, 900);
    return () => globalThis.clearTimeout(id);
  }, [email, isVerified, navigate]);

  const submitOtpVerification = useCallback(
    async (otpCode: string) => {
      if (!email) {
        toast.error("Missing email context.", {
          description: "Please return to Forgot Password.",
        });
        return;
      }

      setIsVerifying(true);
      const result = await verifyResetOtp({ email, otp: otpCode });

      if (result.ok) {
        sessionStorage.setItem(RESET_VERIFIED_FLAG_KEY, "1");
        setIsVerified(true);
        toast.success("OTP verified!", {
          description: "Redirecting to set your new password...",
        });
        setIsVerifying(false);
        return;
      }

      const nextAttempts = failedAttempts + 1;
      setFailedAttempts(nextAttempts);

      if (nextAttempts < MAX_FAILED_ATTEMPTS) {
        toast.error("Invalid OTP.", {
          description: `You have ${MAX_FAILED_ATTEMPTS - nextAttempts} attempt(s) left.`,
        });
      } else {
        toast.error("OTP invalid 3 times.", {
          description: "Please resend OTP and try again.",
        });
      }

      setOtp("");
      setIsVerifying(false);
    },
    [email, failedAttempts],
  );

  const handleVerifyOtp = useCallback(() => {
    if (otp.length !== OTP_LENGTH) {
      toast.error("Please enter the full 6-digit OTP.");
      return;
    }

    if (
      isVerifying ||
      isResending ||
      hasReachedMaxAttempts ||
      !email ||
      isVerified
    ) {
      return;
    }

    void submitOtpVerification(otp);
  }, [
    email,
    hasReachedMaxAttempts,
    isResending,
    isVerified,
    isVerifying,
    otp,
    submitOtpVerification,
  ]);

  const handleResend = useCallback(async () => {
    if (!email) return;
    setIsResending(true);
    const result = await resendOtp({ email });
    if (result.ok) {
      toast.success("New OTP sent!", {
        description: "Please check your email inbox.",
      });
      sessionStorage.removeItem(RESET_VERIFIED_FLAG_KEY);
      setOtp("");
      setFailedAttempts(0);
      setSecondsLeft(RESEND_SECONDS);
    } else {
      toast.error("Could not resend OTP.", { description: result.message });
    }
    setIsResending(false);
  }, [email]);

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 flex items-center justify-center px-6 pt-28 pb-10">
        <div className="w-full max-w-md">
          <Card className="w-full rounded-3xl border border-outline-variant bg-surface-container-lowest p-6 shadow-lg sm:p-8">
            <CardContent className="space-y-6">
              {/* Icon */}
              <div className="flex justify-center">
                <div className="flex size-16 items-center justify-center rounded-full bg-primary/10">
                  <HugeiconsIcon
                    icon={SquareLock02Icon}
                    strokeWidth={1.5}
                    className="size-8 text-primary"
                  />
                </div>
              </div>

              {/* Heading */}
              <div className="space-y-2 text-center">
                <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
                  Verify OTP
                </h1>
                <p className="text-sm text-on-surface-variant">
                  Enter the 6-digit code sent to your email to continue.
                </p>
                {email ? (
                  <p className="text-xs text-on-surface-variant">
                    Code sent to:{" "}
                    <span className="font-medium text-on-surface">{email}</span>
                  </p>
                ) : (
                  <p className="text-xs text-destructive">
                    Missing email context.{" "}
                    <Link
                      to={PATHS.FORGOT_PASSWORD}
                      className="underline text-primary"
                    >
                      Go back
                    </Link>
                  </p>
                )}
              </div>

              {/* OTP Input */}
              <div className="space-y-3">
                <Label className="text-on-surface">OTP Code</Label>
                <div className="rounded-2xl border border-outline-variant bg-surface-container p-4 sm:p-5">
                  <InputOTP
                    value={otp}
                    onChange={(value) => setOtp(value.replaceAll(/\D/g, ""))}
                    maxLength={OTP_LENGTH}
                    containerClassName="justify-center"
                    disabled={
                      isVerifying ||
                      isResending ||
                      hasReachedMaxAttempts ||
                      !email ||
                      isVerified
                    }
                  >
                    <InputOTPGroup className="gap-2 rounded-none">
                      {Array.from(
                        { length: OTP_LENGTH },
                        (_, slotIndex) => slotIndex,
                      ).map((slotIndex) => (
                        <InputOTPSlot
                          key={slotIndex}
                          index={slotIndex}
                          className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                        />
                      ))}
                    </InputOTPGroup>
                  </InputOTP>
                </div>

                <Button
                  type="button"
                  onClick={handleVerifyOtp}
                  disabled={
                    otp.length !== OTP_LENGTH ||
                    isVerifying ||
                    isResending ||
                    hasReachedMaxAttempts ||
                    !email ||
                    isVerified
                  }
                  className="w-full rounded-full py-6 text-base gap-2"
                >
                  {isVerifying ? (
                    <>
                      <HugeiconsIcon
                        icon={Loading03Icon}
                        strokeWidth={2}
                        className="size-4 animate-spin"
                      />
                      Verifying OTP...
                    </>
                  ) : (
                    <>
                      Verify OTP
                      <HugeiconsIcon
                        icon={ArrowRight01Icon}
                        strokeWidth={2}
                        className="size-4"
                      />
                    </>
                  )}
                </Button>

                {/* Resend row */}
                <div className="flex items-center justify-between text-sm">
                  <p className="text-on-surface-variant">
                    {secondsLeft > 0
                      ? `Resend in ${formatCountdown(secondsLeft)}`
                      : "Didn't receive the code?"}
                  </p>
                  <Button
                    type="button"
                    variant="link"
                    className="h-auto p-0 text-primary"
                    disabled={secondsLeft > 0 || isResending || !email}
                    onClick={handleResend}
                  >
                    {isResending ? (
                      <span className="flex items-center gap-1">
                        <HugeiconsIcon
                          icon={Loading03Icon}
                          strokeWidth={2}
                          className="size-3 animate-spin"
                        />
                        Resending...
                      </span>
                    ) : (
                      "Resend OTP"
                    )}
                  </Button>
                </div>

                <p className="text-center text-xs text-on-surface-variant">
                  {isVerifying
                    ? "Verifying OTP..."
                    : `Remaining attempts: ${Math.max(MAX_FAILED_ATTEMPTS - failedAttempts, 0)}`}
                </p>
              </div>

              <p className="text-center text-sm text-on-surface-variant">
                <Link
                  to={PATHS.FORGOT_PASSWORD}
                  className="text-primary font-semibold hover:underline"
                >
                  ← Change email
                </Link>
                {" · "}
                <Link
                  to={PATHS.LOGIN}
                  className="text-primary font-semibold hover:underline"
                >
                  Back to Sign In
                </Link>
              </p>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
