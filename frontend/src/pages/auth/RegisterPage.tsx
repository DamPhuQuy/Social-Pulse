import { AuthLayout } from "@/components/auth/AuthLayout";
import { PasswordInput } from "@/components/auth/PasswordInput";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { PATHS } from "@/constants/paths";
import { registerUser } from "@/services/auth/authService";
import type { FormSubmitEvent } from "@/types/form";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";

type RegisterFormState = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  acceptedTerms: boolean;
};

const INITIAL_FORM: RegisterFormState = {
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
  acceptedTerms: false,
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<RegisterFormState>(INITIAL_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    const username = form.username.trim();
    const email = form.email.trim();

    if (!username || !email || !form.password || !form.confirmPassword) {
      toast.error("Please fill in all required fields.");
      return;
    }

    if (form.password !== form.confirmPassword) {
      toast.error("Password and confirmation do not match.");
      return;
    }

    if (!form.acceptedTerms) {
      toast.error("You must accept Terms and Privacy Policy to continue.");
      return;
    }

    setIsSubmitting(true);

    const result = await registerUser({
      username,
      email,
      password: form.password,
      confirmPassword: form.confirmPassword,
    });

    if (result.ok) {
      toast.success("Registration successful.", {
        description: "Please check your email and verify OTP.",
      });
      sessionStorage.setItem("pendingVerificationEmail", email);
      setIsSubmitting(false);
      navigate(`${PATHS.VERIFY_EMAIL}?email=${encodeURIComponent(email)}`, {
        state: { email },
      });
      return;
    }

    toast.error("Registration failed.", { description: result.message });
    setIsSubmitting(false);
  };

  return (
    <AuthLayout
      heroTitle={
        <>
          Your journey starts with a{" "}
          <span className="text-primary">single profile.</span>
        </>
      }
      heroBody="Join our community today and start socializing with like-minded individuals!"
      heroImageSrc="https://static.vecteezy.com/system/resources/previews/010/925/404/non_2x/registration-page-name-and-password-field-fill-in-form-menu-bar-corporate-website-create-account-user-information-flat-design-modern-illustration-vector.jpg"
      heroImageAlt="Registration illustration"
    >
      <Card className="w-full rounded-3xl border border-outline-variant bg-surface-container-lowest shadow-lg p-6 sm:p-8 font-body text-on-surface">
        <CardContent className="space-y-6">
          {/* Heading */}
          <div className="space-y-1">
            <h1 className="font-headline text-2xl sm:text-3xl font-bold tracking-tight text-on-surface">
              Create Account
            </h1>
          </div>

          {/* Form */}
          <form className="space-y-5" onSubmit={handleSubmit}>
            {/* Username */}
            <div className="space-y-2">
              <Label htmlFor="reg-username" className="text-on-surface">
                Username
              </Label>
              <Input
                id="reg-username"
                value={form.username}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, username: e.target.value }))
                }
                disabled={isSubmitting}
                autoComplete="username"
                placeholder="phuquy123"
                className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
              />
            </div>

            {/* Email */}
            <div className="space-y-2">
              <Label htmlFor="reg-email" className="text-on-surface">
                Email
              </Label>
              <Input
                id="reg-email"
                type="email"
                value={form.email}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, email: e.target.value }))
                }
                disabled={isSubmitting}
                autoComplete="email"
                placeholder="name@example.com"
                className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
              />
            </div>

            {/* Password row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <PasswordInput
                id="reg-password"
                label="Password"
                value={form.password}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, password: e.target.value }))
                }
                disabled={isSubmitting}
                autoComplete="new-password"
              />

              <PasswordInput
                id="reg-confirm"
                label="Confirm"
                value={form.confirmPassword}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    confirmPassword: e.target.value,
                  }))
                }
                disabled={isSubmitting}
                autoComplete="new-password"
              />
            </div>

            {/* Terms checkbox */}
            <div className="flex items-start gap-2">
              <Checkbox
                id="reg-terms"
                checked={form.acceptedTerms}
                disabled={isSubmitting}
                onCheckedChange={(checked) =>
                  setForm((prev) => ({
                    ...prev,
                    acceptedTerms: checked === true,
                  }))
                }
              />
              <Label
                htmlFor="reg-terms"
                className="text-sm text-on-surface-variant leading-relaxed"
              >
                I agree to the{" "}
                <Link
                  to={PATHS.TERMS}
                  className="text-primary font-medium hover:underline"
                >
                  Terms
                </Link>{" "}
                and{" "}
                <Link
                  to={PATHS.PRIVACY}
                  className="text-primary font-medium hover:underline"
                >
                  Privacy Policy
                </Link>
              </Label>
            </div>

            {/* Submit */}
            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full py-6 text-base"
            >
              {isSubmitting ? "Creating Account..." : "Create Account"}
            </Button>

            {/* Divider */}
            <div className="relative py-2">
              <Separator />
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="bg-surface-container-lowest px-3 text-xs text-on-surface-variant uppercase">
                  Or continue with
                </span>
              </div>
            </div>

            {/* Social buttons */}
            <div className="grid grid-cols-2 gap-3">
              <Button
                variant="outline"
                type="button"
                className="border-outline-variant bg-surface-container-lowest text-on-surface hover:bg-surface-container"
              >
                Google
              </Button>
              <Button
                variant="outline"
                type="button"
                className="border-outline-variant bg-surface-container-lowest text-on-surface hover:bg-surface-container"
              >
                GitHub
              </Button>
            </div>
          </form>

          {/* Footer */}
          <p className="text-sm text-center text-on-surface-variant">
            Already have an account?{" "}
            <Link
              to={PATHS.LOGIN}
              className="text-primary font-semibold hover:underline"
            >
              Sign in
            </Link>
          </p>
        </CardContent>
      </Card>
    </AuthLayout>
  );
}
