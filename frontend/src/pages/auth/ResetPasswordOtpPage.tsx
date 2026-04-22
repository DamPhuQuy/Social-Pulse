import Header from "@/components/Header";
import { AuthCard } from "@/components/auth/AuthCard";
import { OtpBlock } from "@/components/auth/OtpBlock";
import { Button } from "@/components/ui/button";
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
const RESET_OTP_CODE_KEY = "pendingResetOtpCode";

/** Formats a number of seconds as "MM:SS" */
function formatCountdown(seconds: number): string {
  const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
  const ss = String(seconds % 60).padStart(2, "0");
  return `${mm}:${ss}`;
}

export default function ResetPasswordOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();

  /** Resolve email from: navigation state → query param → sessionStorage */
  const email = useMemo(() => {
    const stateEmail = (location.state as { email?: string } | null)?.email?.trim();
    if (stateEmail) return stateEmail;

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

  // Persist email so the user doesn't lose context on page refresh
  useEffect(() => {
    if (email) sessionStorage.setItem("pendingResetEmail", email);
  }, [email]);

  // Resend countdown timer
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

  // Navigate to the new-password page after OTP is verified
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
      const result = await verifyResetOtp({ email, otpCode });

      if (result.ok) {
        sessionStorage.setItem(RESET_VERIFIED_FLAG_KEY, "1");
        sessionStorage.setItem(RESET_OTP_CODE_KEY, otpCode);
        setIsVerified(true);
        toast.success("OTP verified!", {
          description: "Redirecting to set your new password...",
        });
        setIsVerifying(false);
        return;
      }

      const nextAttempts = failedAttempts + 1;
      setFailedAttempts(nextAttempts);

      const attemptsLeft = MAX_FAILED_ATTEMPTS - nextAttempts;

      if (attemptsLeft > 0) {
        toast.error("Invalid OTP.", {
          description: `You have ${attemptsLeft} attempt(s) left.`,
        });
      } else {
        toast.error("Too many failed attempts.", {
          description: "Please resend OTP and try again.",
        });
      }

      setOtp("");
      setIsVerifying(false);
    },
    [email, failedAttempts],
  );

  const handleVerifyOtp = useCallback(() => {
    if (
      otp.length !== OTP_LENGTH ||
      isVerifying ||
      isResending ||
      hasReachedMaxAttempts ||
      !email ||
      isVerified
    ) {
      return;
    }
    void submitOtpVerification(otp);
  }, [email, hasReachedMaxAttempts, isResending, isVerified, isVerifying, otp, submitOtpVerification]);

  const handleResend = useCallback(async () => {
    if (!email) return;
    setIsResending(true);
    const result = await resendOtp({ email });
    if (result.ok) {
      sessionStorage.removeItem(RESET_VERIFIED_FLAG_KEY);
      sessionStorage.removeItem(RESET_OTP_CODE_KEY);
      setOtp("");
      setFailedAttempts(0);
      setSecondsLeft(RESEND_SECONDS);
      toast.success("New OTP sent!", {
        description: "Please check your email inbox.",
      });
    } else {
      toast.error("Could not resend OTP.", { description: result.message });
    }
    setIsResending(false);
  }, [email]);

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-md">
          <AuthCard
            icon={SquareLock02Icon}
            heading="Verify OTP"
            description="Enter the 6-digit code sent to your email to continue."
            hint={
              email ? (
                <span>
                  Code sent to:{" "}
                  <span className="font-medium text-on-surface">{email}</span>
                </span>
              ) : (
                <span className="text-destructive">
                  Missing email context.{" "}
                  <Link to={PATHS.FORGOT_PASSWORD} className="underline text-primary">
                    Go back
                  </Link>
                </span>
              )
            }
          >
            <div className="space-y-4">
              {/* OTP input */}
              <OtpBlock
                value={otp}
                onChange={setOtp}
                length={OTP_LENGTH}
                disabled={isVerifying || isResending || hasReachedMaxAttempts || !email || isVerified}
              />

              {/* Verify button */}
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
                    <HugeiconsIcon icon={Loading03Icon} strokeWidth={2} className="size-4 animate-spin" />
                    Verifying OTP...
                  </>
                ) : (
                  <>
                    Verify OTP
                    <HugeiconsIcon icon={ArrowRight01Icon} strokeWidth={2} className="size-4" />
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
                      <HugeiconsIcon icon={Loading03Icon} strokeWidth={2} className="size-3 animate-spin" />
                      Resending...
                    </span>
                  ) : (
                    "Resend OTP"
                  )}
                </Button>
              </div>

              {/* Status line */}
              <p className="text-center text-xs text-on-surface-variant">
                {isVerifying
                  ? "Verifying your code..."
                  : `${Math.max(MAX_FAILED_ATTEMPTS - failedAttempts, 0)} attempt(s) remaining`}
              </p>
            </div>

            {/* Navigation links */}
            <p className="text-center text-sm text-on-surface-variant">
              <Link to={PATHS.FORGOT_PASSWORD} className="text-primary font-semibold hover:underline">
                ← Change email
              </Link>
              {" · "}
              <Link to={PATHS.LOGIN} className="text-primary font-semibold hover:underline">
                Back to Sign In
              </Link>
            </p>
          </AuthCard>
        </div>
      </main>
    </div>
  );
}
