import { AuthLayout } from "@/components/auth/AuthLayout";
import { PasswordInput } from "@/components/auth/PasswordInput";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { setApiClientToken } from "@/lib/axiosClient";
import { loginUser } from "@/services/auth/authService";
import type { FormSubmitEvent } from "@/types/form";
import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

type LoginFormState = {
  email: string;
  password: string;
};

const INITIAL_FORM: LoginFormState = {
  email: "",
  password: "",
};

export default function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setAccessToken } = useAuth();

  // Pre-fill email from query param (e.g. redirected from register)
  const prefilledEmail = useMemo(
    () => new URLSearchParams(location.search).get("email")?.trim() ?? "",
    [location.search],
  );

  const [form, setForm] = useState<LoginFormState>({
    ...INITIAL_FORM,
    email: prefilledEmail,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    const email = form.email.trim();

    if (!email || !form.password) {
      toast.error("Please enter both email and password.");
      return;
    }

    setIsSubmitting(true);

    const result = await loginUser({ email, password: form.password });

    if (result.ok && result.accessToken) {
      setAccessToken(result.accessToken);
      setApiClientToken(result.accessToken);

      toast.success("Login successful.", {
        description: "Welcome back to Social Pulse.",
      });

      navigate(PATHS.ONBOARDING);
    } else {
      toast.error("Login failed.", { description: result.message });
    }

    setIsSubmitting(false);
  };

  return (
    <AuthLayout
      heroTitle={
        <>
          Welcome back to your{" "}
          <span className="text-primary">Social Pulse.</span>
        </>
      }
      heroBody="Sign in to reconnect with your communities and pick up where you left off."
      heroImageSrc="https://img.freepik.com/free-vector/flat-design-international-human-rights-day_23-2148711491.jpg?semt=ais_incoming&w=740&q=80"
      heroImageAlt="Login illustration"
    >
      <Card className="w-full rounded-3xl border border-outline-variant bg-surface-container-lowest shadow-lg p-6 sm:p-8">
        <CardContent className="space-y-6">
          {/* Heading */}
          <div className="space-y-1">
            <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
              Sign In
            </h1>
            <p className="text-sm text-on-surface-variant">
              Login with your existing account to continue.
            </p>
          </div>

          {/* Form */}
          <form className="space-y-5" onSubmit={handleSubmit}>
            {/* Email */}
            <div className="space-y-2">
              <Label htmlFor="login-email" className="text-on-surface">
                Email
              </Label>
              <Input
                id="login-email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, email: e.target.value }))
                }
                disabled={isSubmitting}
                placeholder="name@example.com"
                className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
              />
            </div>

            {/* Password with "Forgot password?" link in the label row */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="login-password" className="text-on-surface">
                  Password
                </Label>
                <Link
                  to={
                    form.email.trim()
                      ? `${PATHS.FORGOT_PASSWORD}?email=${encodeURIComponent(form.email.trim())}`
                      : PATHS.FORGOT_PASSWORD
                  }
                  className="text-xs text-primary font-semibold hover:underline"
                  tabIndex={-1}
                >
                  Forgot password?
                </Link>
              </div>
              <PasswordInput
                id="login-password"
                label=""
                autoComplete="current-password"
                value={form.password}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, password: e.target.value }))
                }
                disabled={isSubmitting}
                placeholder="P@ssw0rd"
              />
            </div>

            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full py-6 text-base"
            >
              {isSubmitting ? "Signing In..." : "Sign In"}
            </Button>
          </form>

          {/* Footer link */}
          <p className="text-center text-sm text-on-surface-variant">
            Need a new account?{" "}
            <Link
              to={PATHS.REGISTER}
              className="text-primary font-semibold hover:underline"
            >
              Create account
            </Link>
          </p>
        </CardContent>
      </Card>
    </AuthLayout>
  );
}
