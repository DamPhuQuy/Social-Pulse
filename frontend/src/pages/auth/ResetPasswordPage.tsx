import Header from "@/components/Header";
import { AuthCard } from "@/components/auth/AuthCard";
import { PasswordInput } from "@/components/auth/PasswordInput";
import { Button } from "@/components/ui/button";
import { PATHS } from "@/constants/paths";
import { resetPassword } from "@/services/auth/authService";
import type { FormSubmitEvent } from "@/types/form";
import { Key01Icon, Loading03Icon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

const RESET_VERIFIED_FLAG_KEY = "pendingResetVerified";
const RESET_OTP_CODE_KEY = "pendingResetOtpCode";

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const navigationState = useMemo(
    () => location.state as { email?: string; verified?: boolean } | null,
    [location.state],
  );

  /** Resolve email from: navigation state → query param → sessionStorage */
  const email = useMemo(() => {
    const fromState = navigationState?.email?.trim() ?? "";
    if (fromState) return fromState;
    return (
      new URLSearchParams(location.search).get("email")?.trim() ??
      sessionStorage.getItem("pendingResetEmail")?.trim() ??
      ""
    );
  }, [location.search, navigationState]);

  /** True when OTP has been verified (either in navigation state or sessionStorage) */
  const isOtpVerified = useMemo(
    () =>
      Boolean(navigationState?.verified) ||
      sessionStorage.getItem(RESET_VERIFIED_FLAG_KEY) === "1",
    [navigationState],
  );

  /** The OTP code stored in sessionStorage after verification */
  const otpCode = useMemo(
    () => sessionStorage.getItem(RESET_OTP_CODE_KEY)?.trim() ?? "",
    [],
  );

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  // Guard: redirect to OTP verification if not yet verified
  useEffect(() => {
    if (!email || isOtpVerified) return;
    toast.error("OTP verification required.", {
      description: "Please verify OTP before setting a new password.",
    });
    navigate(`${PATHS.RESET_PASSWORD}?email=${encodeURIComponent(email)}`, {
      replace: true,
      state: { email },
    });
  }, [email, isOtpVerified, navigate]);

  // Redirect to Login after successful password reset
  useEffect(() => {
    if (!isSuccess) return;
    const loginPath = email
      ? `${PATHS.LOGIN}?email=${encodeURIComponent(email)}`
      : PATHS.LOGIN;

    const id = globalThis.setTimeout(() => {
      sessionStorage.removeItem("pendingResetEmail");
      sessionStorage.removeItem(RESET_VERIFIED_FLAG_KEY);
      sessionStorage.removeItem(RESET_OTP_CODE_KEY);
      navigate(loginPath);
    }, 1500);

    return () => globalThis.clearTimeout(id);
  }, [email, isSuccess, navigate]);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    if (!email) {
      toast.error("Email context missing.", {
        description: "Please start over from Forgot Password.",
      });
      return;
    }

    if (!isOtpVerified) {
      toast.error("OTP not verified.", {
        description: "Please complete OTP verification first.",
      });
      return;
    }

    if (!otpCode || otpCode.length !== 6) {
      toast.error("OTP context missing.", {
        description: "Please verify OTP again before resetting password.",
      });
      return;
    }

    if (!newPassword) {
      toast.error("Please enter your new password.");
      return;
    }

    if (newPassword.length < 6) {
      toast.error("Password must be at least 6 characters.");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);

    const result = await resetPassword({ email, otpCode, newPassword });

    if (result.ok) {
      setIsSuccess(true);
      toast.success("Password reset successfully!", {
        description: "Redirecting to Sign In...",
      });
    } else {
      toast.error("Reset failed.", { description: result.message });
    }

    setIsSubmitting(false);
  };

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-md">
          <AuthCard
            icon={Key01Icon}
            heading="Set New Password"
            description="Choose a strong new password for your account."
            hint={
              email ? (
                <span>
                  Resetting password for:{" "}
                  <span className="font-medium text-on-surface">{email}</span>
                </span>
              ) : (
                <span className="text-destructive">
                  Missing email context.{" "}
                  <Link to={PATHS.FORGOT_PASSWORD} className="underline text-primary">
                    Start over
                  </Link>
                </span>
              )
            }
          >
            <form className="space-y-5" onSubmit={handleSubmit}>
              <PasswordInput
                id="new-password"
                label="New Password"
                autoComplete="new-password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                disabled={isSubmitting || isSuccess}
                placeholder="At least 6 characters"
              />

              <PasswordInput
                id="confirm-password"
                label="Confirm Password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                disabled={isSubmitting || isSuccess}
                placeholder="Repeat your new password"
              />

              <Button
                type="submit"
                id="reset-password-submit"
                disabled={isSubmitting || isSuccess || !email || !isOtpVerified}
                className="w-full rounded-full py-6 text-base gap-2"
              >
                {isSubmitting ? (
                  <>
                    <HugeiconsIcon icon={Loading03Icon} strokeWidth={2} className="size-4 animate-spin" />
                    Saving...
                  </>
                ) : isSuccess ? (
                  "Password Saved!"
                ) : (
                  "Save New Password"
                )}
              </Button>
            </form>

            {/* Navigation links */}
            <p className="text-center text-sm text-on-surface-variant">
              <Link to={PATHS.FORGOT_PASSWORD} className="text-primary font-semibold hover:underline">
                ← Start over
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
