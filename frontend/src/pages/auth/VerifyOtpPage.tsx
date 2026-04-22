import Header from "@/components/Header";
import { AuthCard } from "@/components/auth/AuthCard";
import { OtpBlock } from "@/components/auth/OtpBlock";
import { Button } from "@/components/ui/button";
import { PATHS } from "@/constants/paths";
import { verifyEmailOtp } from "@/services/auth/authService";
import { Mail01Icon } from "@hugeicons/core-free-icons";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 60;
const MAX_FAILED_ATTEMPTS = 3;

/** Formats a number of seconds as "MM:SS" */
function formatCountdown(seconds: number): string {
  const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
  const ss = String(seconds % 60).padStart(2, "0");
  return `${mm}:${ss}`;
}

export default function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();

  /** Resolve email from: navigation state → query param → sessionStorage */
  const email = useMemo(() => {
    const stateEmail = (location.state as { email?: string } | null)?.email?.trim();
    if (stateEmail) return stateEmail;

    const queryEmail = new URLSearchParams(location.search).get("email")?.trim();
    if (queryEmail) return queryEmail;

    return sessionStorage.getItem("pendingVerificationEmail")?.trim() ?? "";
  }, [location.search, location.state]);

  const [otp, setOtp] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [isResending, setIsResending] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [isVerified, setIsVerified] = useState(false);

  /** Track the last OTP we submitted to avoid double-submitting the same code */
  const lastSubmittedOtpRef = useRef<string | null>(null);

  const hasReachedMaxAttempts = failedAttempts >= MAX_FAILED_ATTEMPTS;

  // Persist email so the user doesn't lose context on page refresh
  useEffect(() => {
    if (email) sessionStorage.setItem("pendingVerificationEmail", email);
  }, [email]);

  // Redirect to Login after successful verification
  useEffect(() => {
    if (!isVerified) return;
    const id = globalThis.setTimeout(() => {
      navigate(`${PATHS.LOGIN}?email=${encodeURIComponent(email)}`);
    }, 1200);
    return () => globalThis.clearTimeout(id);
  }, [email, isVerified, navigate]);

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

  const submitOtpVerification = useCallback(
    async (otpCode: string) => {
      if (!email) {
        toast.error("Missing email context.", {
          description: "Please return to register and try again.",
        });
        return;
      }

      setIsVerifying(true);
      const result = await verifyEmailOtp({ email, otpCode });

      if (result.ok) {
        setIsVerified(true);
        sessionStorage.removeItem("pendingVerificationEmail");
        toast.success("OTP verified successfully.", {
          description: "Redirecting to Login page...",
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
          description: "Please click Resend OTP to get a new code.",
        });
      }

      setOtp("");
      lastSubmittedOtpRef.current = null;
      setIsVerifying(false);
    },
    [email, failedAttempts],
  );

  // Auto-submit when all 6 digits are entered
  useEffect(() => {
    if (otp.length !== OTP_LENGTH) return;
    if (isVerifying || isResending || hasReachedMaxAttempts || !email) return;
    if (otp === lastSubmittedOtpRef.current) return;

    lastSubmittedOtpRef.current = otp;
    void submitOtpVerification(otp);
  }, [email, hasReachedMaxAttempts, isResending, isVerifying, otp, submitOtpVerification]);

  const handleResendOtp = async () => {
    setIsResending(true);
    // Small delay for UX feedback
    await new Promise((resolve) => setTimeout(resolve, 700));
    setIsResending(false);
    setSecondsLeft(RESEND_SECONDS);
    setOtp("");
    setFailedAttempts(0);
    lastSubmittedOtpRef.current = null;
    setIsVerified(false);
    toast.success("A new OTP code has been sent.", {
      description: "Please check your email inbox.",
    });
  };

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-md">
          <AuthCard
            icon={Mail01Icon}
            heading="Verify OTP"
            description="Enter the 6-digit code sent to your email to complete registration."
            hint={
              email ? (
                <span>
                  Verification email:{" "}
                  <span className="font-medium text-on-surface">{email}</span>
                </span>
              ) : (
                <span className="text-destructive">
                  Missing email context. Please return to register first.
                </span>
              )
            }
          >
            <div className="space-y-5">
              {/* OTP input */}
              <OtpBlock
                value={otp}
                onChange={setOtp}
                length={OTP_LENGTH}
                disabled={isResending || isVerifying || hasReachedMaxAttempts || !email}
              />

              {/* Resend row */}
              <div className="flex items-center justify-between gap-3 text-sm">
                <p className="text-on-surface-variant">
                  {secondsLeft > 0
                    ? `Resend in ${formatCountdown(secondsLeft)}`
                    : "Didn't receive the code?"}
                </p>
                <Button
                  type="button"
                  variant="link"
                  className="h-auto p-0 text-primary"
                  disabled={secondsLeft > 0 || isResending}
                  onClick={handleResendOtp}
                >
                  {isResending ? "Resending..." : "Resend OTP"}
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
              <Link to={PATHS.REGISTER} className="text-primary font-semibold hover:underline">
                Back to Register
              </Link>
              {" | "}
              <Link
                to={`${PATHS.LOGIN}${email ? `?email=${encodeURIComponent(email)}` : ""}`}
                className="text-primary font-semibold hover:underline"
              >
                Go to Login
              </Link>
            </p>
          </AuthCard>
        </div>
      </main>
    </div>
  );
}
