import Header from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { PATHS } from "@/constants/paths";
import { verifyEmailOtp } from "@/services/auth/authService";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 60;
const MAX_FAILED_ATTEMPTS = 3;

function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(
    remainingSeconds,
  ).padStart(2, "0")}`;
}

export default function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [otp, setOtp] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [isResending, setIsResending] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const lastSubmittedOtpRef = useRef<string | null>(null);
  const [isVerified, setIsVerified] = useState(false);

  const email = useMemo(() => {
    const stateValue = location.state as { email?: string } | null;
    const fromState = stateValue?.email?.trim() ?? "";

    if (fromState) {
      return fromState;
    }

    const fromQuery = new URLSearchParams(location.search).get("email")?.trim();

    if (fromQuery) {
      return fromQuery;
    }

    return sessionStorage.getItem("pendingVerificationEmail")?.trim() ?? "";
  }, [location.search, location.state]);

  const hasReachedMaxAttempts = failedAttempts >= MAX_FAILED_ATTEMPTS;

  useEffect(() => {
    if (!email) {
      return;
    }

    sessionStorage.setItem("pendingVerificationEmail", email);
  }, [email]);

  useEffect(() => {
    if (!isVerified) {
      return;
    }

    const timeoutId = globalThis.setTimeout(() => {
      navigate(`${PATHS.LOGIN}?email=${encodeURIComponent(email)}`);
    }, 1200);

    return () => globalThis.clearTimeout(timeoutId);
  }, [email, isVerified, navigate]);

  useEffect(() => {
    if (secondsLeft <= 0) {
      return;
    }

    const intervalId = globalThis.setInterval(() => {
      setSecondsLeft((previousSeconds) => {
        if (previousSeconds <= 1) {
          globalThis.clearInterval(intervalId);
          return 0;
        }

        return previousSeconds - 1;
      });
    }, 1000);

    return () => globalThis.clearInterval(intervalId);
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
        toast.success("OTP verified successfully.", {
          description: "Redirecting to Login page...",
        });
        sessionStorage.removeItem("pendingVerificationEmail");
        setIsVerifying(false);
        return;
      }

      const nextAttempts = failedAttempts + 1;

      setFailedAttempts(nextAttempts);

      if (nextAttempts <= MAX_FAILED_ATTEMPTS) {
        const remainingWarnings = MAX_FAILED_ATTEMPTS - nextAttempts;

        if (remainingWarnings > 0) {
          toast.error("OTP is invalid.", {
            description: `You have ${remainingWarnings} attempt(s) left.`,
          });
        } else {
          toast.error("OTP is invalid 3 times.", {
            description: "Please resend OTP and try again.",
          });
        }
      }

      if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
        toast.warning("Verification is temporarily locked.", {
          description: "Please click Resend OTP to continue.",
        });
      }

      setOtp("");
      lastSubmittedOtpRef.current = null;
      setIsVerifying(false);
    },
    [email, failedAttempts],
  );

  useEffect(() => {
    if (otp.length !== OTP_LENGTH) {
      return;
    }

    if (isVerifying || isResending || hasReachedMaxAttempts || !email) {
      return;
    }

    if (otp === lastSubmittedOtpRef.current) {
      return;
    }

    lastSubmittedOtpRef.current = otp;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void submitOtpVerification(otp);
  }, [
    email,
    hasReachedMaxAttempts,
    isResending,
    isVerifying,
    lastSubmittedOtpRef,
    otp,
    submitOtpVerification,
  ]);

  const handleResendOtp = async () => {
    setIsResending(true);

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

      <main className="flex-1 flex items-center justify-center px-6 pt-28 pb-10">
        <Card className="w-full max-w-xl rounded-3xl border border-outline-variant bg-surface-container-lowest p-6 shadow-lg sm:p-8">
          <CardContent className="space-y-6">
            <div className="space-y-2 text-center">
              <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
                Verify OTP
              </h1>
              <p className="text-sm text-on-surface-variant">
                Enter the 6-digit code sent to your email to complete
                registration.
              </p>
              {email ? (
                <p className="text-xs text-on-surface-variant">
                  Verification email:{" "}
                  <span className="font-medium text-on-surface">{email}</span>
                </p>
              ) : (
                <p className="text-xs text-destructive">
                  Missing email context. Please return to register first.
                </p>
              )}
            </div>

            <div className="space-y-5">
              <div className="rounded-2xl border border-outline-variant bg-surface-container p-4 sm:p-5">
                <InputOTP
                  value={otp}
                  onChange={(value) => {
                    setOtp(value.replace(/\D/g, ""));
                  }}
                  maxLength={OTP_LENGTH}
                  containerClassName="justify-center"
                  disabled={
                    isResending ||
                    isVerifying ||
                    hasReachedMaxAttempts ||
                    !email
                  }
                >
                  <InputOTPGroup className="gap-2 rounded-none">
                    <InputOTPSlot
                      index={0}
                      className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                    />
                    <InputOTPSlot
                      index={1}
                      className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                    />
                    <InputOTPSlot
                      index={2}
                      className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                    />
                    <InputOTPSlot
                      index={3}
                      className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                    />
                    <InputOTPSlot
                      index={4}
                      className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                    />
                    <InputOTPSlot
                      index={5}
                      className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                    />
                  </InputOTPGroup>
                </InputOTP>
              </div>

              <div className="flex items-center justify-between gap-3 text-sm">
                <p className="text-on-surface-variant">
                  {secondsLeft > 0
                    ? `Resend OTP in ${formatCountdown(secondsLeft)}`
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

              <p className="text-center text-xs text-on-surface-variant">
                {isVerifying
                  ? "Checking OTP with backend..."
                  : `Remaining warning attempts: ${Math.max(MAX_FAILED_ATTEMPTS - failedAttempts, 0)}`}
              </p>
            </div>

            <p className="text-center text-sm text-on-surface-variant">
              Need to use another email?{" "}
              <Link
                to={PATHS.REGISTER}
                className="text-primary font-semibold hover:underline"
              >
                Back to Register
              </Link>{" "}
              |{" "}
              <Link
                to={`${PATHS.LOGIN}${email ? `?email=${encodeURIComponent(email)}` : ""}`}
                className="text-primary font-semibold hover:underline"
              >
                Go to Login
              </Link>
            </p>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
