import Header from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { forgotPassword } from "@/services/auth/authService";
import {
  ArrowRight01Icon,
  Loading03Icon,
  Mail01Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type { ComponentProps } from "react";
import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

type FormSubmitEvent = Parameters<
  NonNullable<ComponentProps<"form">["onSubmit"]>
>[0];

export default function ForgotPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const prefilledEmail = useMemo(
    () => new URLSearchParams(location.search).get("email")?.trim() ?? "",
    [location.search],
  );

  const [email, setEmail] = useState(prefilledEmail);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      toast.error("Please enter your email address.");
      return;
    }

    setIsSubmitting(true);

    const result = await forgotPassword({ email: trimmedEmail });

    if (result.ok) {
      sessionStorage.setItem("pendingResetEmail", trimmedEmail);
      sessionStorage.removeItem("pendingResetVerified");

      toast.success("OTP sent!", {
        description:
          "Please check your email inbox for the password reset code.",
      });
      // Chuyển sang ResetPasswordOtpPage để verify OTP
      navigate(
        `${PATHS.RESET_PASSWORD}?email=${encodeURIComponent(trimmedEmail)}`,
        { state: { email: trimmedEmail } },
      );
    } else {
      toast.error("Failed to send reset code.", {
        description: result.message,
      });
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
                    icon={Mail01Icon}
                    strokeWidth={1.5}
                    className="size-8 text-primary"
                  />
                </div>
              </div>

              {/* Heading */}
              <div className="space-y-2 text-center">
                <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
                  Forgot Password
                </h1>
                <p className="text-sm text-on-surface-variant">
                  Enter your account email and we&apos;ll send you an OTP code
                  to reset your password.
                </p>
              </div>

              {/* Form */}
              <form className="space-y-5" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <Label htmlFor="forgot-email" className="text-on-surface">
                    Email address
                  </Label>
                  <Input
                    id="forgot-email"
                    type="email"
                    autoComplete="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={isSubmitting}
                    placeholder="name@example.com"
                    className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
                  />
                </div>

                <Button
                  type="submit"
                  id="forgot-password-submit"
                  disabled={isSubmitting}
                  className="w-full rounded-full py-6 text-base gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <HugeiconsIcon
                        icon={Loading03Icon}
                        strokeWidth={2}
                        className="size-4 animate-spin"
                      />
                      Sending OTP...
                    </>
                  ) : (
                    <>
                      Send Reset Code
                      <HugeiconsIcon
                        icon={ArrowRight01Icon}
                        strokeWidth={2}
                        className="size-4"
                      />
                    </>
                  )}
                </Button>
              </form>

              <p className="text-center text-sm text-on-surface-variant">
                Remember your password?{" "}
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
