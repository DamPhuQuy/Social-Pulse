import Header from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { PATHS } from "@/constants/paths";
import { resetPassword, resendOtp } from "@/services/auth/authService";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ViewIcon, ViewOffSlashIcon, Key01Icon, Loading03Icon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import type { ComponentProps } from "react";

type FormSubmitEvent = Parameters<
  NonNullable<ComponentProps<"form">["onSubmit"]>
>[0];

const OTP_LENGTH = 6;
const RESEND_SECONDS = 60;

function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
}

export default function ResetPasswordPage() {
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
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [isSuccess, setIsSuccess] = useState(false);

  // Lưu email vào sessionStorage để giữ state khi reload
  useEffect(() => {
    if (email) sessionStorage.setItem("pendingResetEmail", email);
  }, [email]);

  // Countdown resend timer
  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = window.setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          window.clearInterval(id);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => window.clearInterval(id);
  }, [secondsLeft]);

  // Redirect sau khi đổi mật khẩu thành công
  useEffect(() => {
    if (!isSuccess) return;
    const id = window.setTimeout(() => {
      sessionStorage.removeItem("pendingResetEmail");
      navigate(
        `${PATHS.LOGIN}${email ? `?email=${encodeURIComponent(email)}` : ""}`,
      );
    }, 1500);
    return () => window.clearTimeout(id);
  }, [email, isSuccess, navigate]);

  const handleResend = useCallback(async () => {
    if (!email) return;
    setIsResending(true);
    const result = await resendOtp({ email });
    if (result.ok) {
      toast.success("New OTP sent!", {
        description: "Please check your email inbox.",
      });
      setOtp("");
      setSecondsLeft(RESEND_SECONDS);
    } else {
      toast.error("Could not resend OTP.", { description: result.message });
    }
    setIsResending(false);
  }, [email]);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    if (!email) {
      toast.error("Email context missing.", {
        description: "Please go back to Forgot Password.",
      });
      return;
    }

    if (otp.length !== OTP_LENGTH) {
      toast.error("Please enter the complete 6-digit OTP.");
      return;
    }

    if (!newPassword) {
      toast.error("Please enter your new password.");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);

    const result = await resetPassword({ email, otp, newPassword });

    if (result.ok) {
      setIsSuccess(true);
      toast.success("Password reset successfully!", {
        description: "Redirecting to Sign In...",
      });
    } else {
      toast.error("Reset failed.", { description: result.message });
      setOtp("");
    }

    setIsSubmitting(false);
  };

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
                  <HugeiconsIcon icon={Key01Icon} strokeWidth={1.5} className="size-8 text-primary" />
                </div>
              </div>

              {/* Heading */}
              <div className="space-y-2 text-center">
                <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
                  Reset Password
                </h1>
                <p className="text-sm text-on-surface-variant">
                  Enter the OTP sent to your email and choose a new password.
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

              <form className="space-y-5" onSubmit={handleSubmit}>
                {/* OTP Input */}
                <div className="space-y-3">
                  <Label className="text-on-surface">OTP Code</Label>
                  <div className="rounded-2xl border border-outline-variant bg-surface-container p-4">
                    <InputOTP
                      value={otp}
                      onChange={(value) => setOtp(value.replace(/\D/g, ""))}
                      maxLength={OTP_LENGTH}
                      containerClassName="justify-center"
                      disabled={isSubmitting || isResending || !email}
                    >
                      <InputOTPGroup className="gap-2 rounded-none">
                        {Array.from({ length: OTP_LENGTH }).map((_, i) => (
                          <InputOTPSlot
                            key={i}
                            index={i}
                            className="size-11 rounded-xl border border-outline-variant bg-surface-container-lowest text-base first:rounded-xl first:border"
                          />
                        ))}
                      </InputOTPGroup>
                    </InputOTP>
                  </div>

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
                      {isResending ? "Resending..." : "Resend OTP"}
                    </Button>
                  </div>
                </div>

                {/* New Password */}
                <div className="space-y-2">
                  <Label
                    htmlFor="reset-new-password"
                    className="text-on-surface"
                  >
                    New Password
                  </Label>
                  <div className="relative">
                    <Input
                      id="reset-new-password"
                      type={showNewPassword ? "text" : "password"}
                      autoComplete="new-password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      disabled={isSubmitting}
                      placeholder="At least 6 characters"
                      className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60 pr-10"
                    />
                    <button
                      type="button"
                      tabIndex={-1}
                      aria-label={
                        showNewPassword
                          ? "Hide new password"
                          : "Show new password"
                      }
                      onClick={() => setShowNewPassword((p) => !p)}
                      className="absolute inset-y-0 right-3 flex items-center text-on-surface-variant hover:text-on-surface transition-colors"
                    >
                      <HugeiconsIcon
                        icon={showNewPassword ? ViewOffSlashIcon : ViewIcon}
                        strokeWidth={2}
                        className="size-4"
                      />
                    </button>
                  </div>
                </div>

                {/* Confirm Password */}
                <div className="space-y-2">
                  <Label
                    htmlFor="reset-confirm-password"
                    className="text-on-surface"
                  >
                    Confirm Password
                  </Label>
                  <div className="relative">
                    <Input
                      id="reset-confirm-password"
                      type={showConfirmPassword ? "text" : "password"}
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      disabled={isSubmitting}
                      placeholder="Repeat your new password"
                      className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60 pr-10"
                    />
                    <button
                      type="button"
                      tabIndex={-1}
                      aria-label={
                        showConfirmPassword
                          ? "Hide confirm password"
                          : "Show confirm password"
                      }
                      onClick={() => setShowConfirmPassword((p) => !p)}
                      className="absolute inset-y-0 right-3 flex items-center text-on-surface-variant hover:text-on-surface transition-colors"
                    >
                      <HugeiconsIcon
                        icon={showConfirmPassword ? ViewOffSlashIcon : ViewIcon}
                        strokeWidth={2}
                        className="size-4"
                      />
                    </button>
                  </div>
                </div>

                <Button
                  type="submit"
                  id="reset-password-submit"
                  disabled={isSubmitting || isSuccess || !email}
                  className="w-full rounded-full py-6 text-base gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <HugeiconsIcon icon={Loading03Icon} strokeWidth={2} className="size-4 animate-spin" />
                      Resetting...
                    </>
                  ) : isSuccess ? (
                    "Password Reset!"
                  ) : (
                    "Reset Password"
                  )}
                </Button>
              </form>

              <p className="text-center text-sm text-on-surface-variant">
                <Link
                  to={PATHS.FORGOT_PASSWORD}
                  className="text-primary font-semibold hover:underline"
                >
                  ← Change email address
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
