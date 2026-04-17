import Header from "@/components/Header";
import { AuthCard } from "@/components/auth/AuthCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { forgotPassword } from "@/services/auth/authService";
import type { FormSubmitEvent } from "@/types/form";
import {
  ArrowRight01Icon,
  Loading03Icon,
  Mail01Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

export default function ForgotPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Pre-fill email if redirected from Login with ?email=...
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
        description: "Please check your email inbox for the password reset code.",
      });

      navigate(
        `${PATHS.RESET_PASSWORD}?email=${encodeURIComponent(trimmedEmail)}`,
        { state: { email: trimmedEmail } },
      );
    } else {
      toast.error("Failed to send reset code.", { description: result.message });
    }

    setIsSubmitting(false);
  };

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-md">
          <AuthCard
            icon={Mail01Icon}
            heading="Forgot Password"
            description="Enter your account email and we'll send you an OTP code to reset your password."
          >
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
          </AuthCard>
        </div>
      </main>
    </div>
  );
}
