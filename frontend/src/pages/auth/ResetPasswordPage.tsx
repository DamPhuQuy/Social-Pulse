import Header from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { resetPassword } from "@/services/auth/authService";
import {
  Key01Icon,
  Loading03Icon,
  ViewIcon,
  ViewOffSlashIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type { ComponentProps, ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

type FormSubmitEvent = Parameters<
  NonNullable<ComponentProps<"form">["onSubmit"]>
>[0];

const RESET_VERIFIED_FLAG_KEY = "pendingResetVerified";

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const navigationState = useMemo(
    () => location.state as { email?: string; verified?: boolean } | null,
    [location.state],
  );

  // Email được truyền qua từ ResetPasswordOtpPage sau khi verify OTP thành công
  const email = useMemo(() => {
    const fromState = navigationState?.email?.trim() ?? "";
    if (fromState) return fromState;
    return (
      new URLSearchParams(location.search).get("email")?.trim() ??
      sessionStorage.getItem("pendingResetEmail")?.trim() ??
      ""
    );
  }, [location.search, navigationState]);

  const isOtpVerified = useMemo(
    () =>
      Boolean(navigationState?.verified) ||
      sessionStorage.getItem(RESET_VERIFIED_FLAG_KEY) === "1",
    [navigationState],
  );

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  let submitContent: ReactNode = "Save New Password";

  if (isSubmitting) {
    submitContent = (
      <>
        <HugeiconsIcon
          icon={Loading03Icon}
          strokeWidth={2}
          className="size-4 animate-spin"
        />
        Saving...
      </>
    );
  } else if (isSuccess) {
    submitContent = "Password Saved!";
  }

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

  // Redirect về Login sau khi đặt mật khẩu thành công
  useEffect(() => {
    if (!isSuccess) return;
    const loginPath = email
      ? `${PATHS.LOGIN}?email=${encodeURIComponent(email)}`
      : PATHS.LOGIN;

    const id = globalThis.setTimeout(() => {
      sessionStorage.removeItem("pendingResetEmail");
      sessionStorage.removeItem(RESET_VERIFIED_FLAG_KEY);
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

    const result = await resetPassword({ email, newPassword });

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

      <main className="flex-1 flex items-center justify-center px-6 pt-28 pb-10">
        <div className="w-full max-w-md">
          <Card className="w-full rounded-3xl border border-outline-variant bg-surface-container-lowest p-6 shadow-lg sm:p-8">
            <CardContent className="space-y-6">
              {/* Icon */}
              <div className="flex justify-center">
                <div className="flex size-16 items-center justify-center rounded-full bg-primary/10">
                  <HugeiconsIcon
                    icon={Key01Icon}
                    strokeWidth={1.5}
                    className="size-8 text-primary"
                  />
                </div>
              </div>

              {/* Heading */}
              <div className="space-y-2 text-center">
                <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
                  Set New Password
                </h1>
                <p className="text-sm text-on-surface-variant">
                  Choose a strong new password for your account.
                </p>
                {email ? (
                  <p className="text-xs text-on-surface-variant">
                    Resetting password for:{" "}
                    <span className="font-medium text-on-surface">{email}</span>
                  </p>
                ) : (
                  <p className="text-xs text-destructive">
                    Missing email context.{" "}
                    <Link
                      to={PATHS.FORGOT_PASSWORD}
                      className="underline text-primary"
                    >
                      Start over
                    </Link>
                  </p>
                )}
              </div>

              <form className="space-y-5" onSubmit={handleSubmit}>
                {/* New Password */}
                <div className="space-y-2">
                  <Label htmlFor="new-password" className="text-on-surface">
                    New Password
                  </Label>
                  <div className="relative">
                    <Input
                      id="new-password"
                      type={showNewPassword ? "text" : "password"}
                      autoComplete="new-password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      disabled={isSubmitting || isSuccess}
                      placeholder="At least 6 characters"
                      className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60 pr-10"
                    />
                    <button
                      type="button"
                      tabIndex={-1}
                      aria-label={
                        showNewPassword ? "Hide password" : "Show password"
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
                  <Label htmlFor="confirm-password" className="text-on-surface">
                    Confirm Password
                  </Label>
                  <div className="relative">
                    <Input
                      id="confirm-password"
                      type={showConfirmPassword ? "text" : "password"}
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      disabled={isSubmitting || isSuccess}
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
                  disabled={
                    isSubmitting || isSuccess || !email || !isOtpVerified
                  }
                  className="w-full rounded-full py-6 text-base gap-2"
                >
                  {submitContent}
                </Button>
              </form>

              <p className="text-center text-sm text-on-surface-variant">
                <Link
                  to={PATHS.FORGOT_PASSWORD}
                  className="text-primary font-semibold hover:underline"
                >
                  ← Start over
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
