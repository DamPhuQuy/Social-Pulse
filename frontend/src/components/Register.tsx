import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { PATHS } from "@/constants/paths";
import { registerUser } from "@/services/authService";
import type { ComponentProps } from "react";
import { useState } from "react";
import { Link } from "react-router-dom";

type RegisterFormState = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  acceptedTerms: boolean;
};

type FormSubmitEvent = Parameters<
  NonNullable<ComponentProps<"form">["onSubmit"]>
>[0];

const INITIAL_FORM: RegisterFormState = {
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
  acceptedTerms: false,
};

function extractResponsePreview(data: unknown): string | null {
  if (!data) {
    return null;
  }

  try {
    return JSON.stringify(data, null, 2);
  } catch {
    return null;
  }
}

export default function RegisterForm() {
  const [form, setForm] = useState<RegisterFormState>(INITIAL_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [responseType, setResponseType] = useState<"success" | "error" | null>(
    null,
  );
  const [responseMessage, setResponseMessage] = useState<string | null>(null);
  const [responsePreview, setResponsePreview] = useState<string | null>(null);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    const username = form.username.trim();
    const email = form.email.trim();

    if (!username || !email || !form.password || !form.confirmPassword) {
      setResponseType("error");
      setResponseMessage("Please fill in all required fields.");
      setResponsePreview(null);
      return;
    }

    if (form.password !== form.confirmPassword) {
      setResponseType("error");
      setResponseMessage("Password and confirmation do not match.");
      setResponsePreview(null);
      return;
    }

    if (!form.acceptedTerms) {
      setResponseType("error");
      setResponseMessage(
        "You must accept Terms and Privacy Policy to continue.",
      );
      setResponsePreview(null);
      return;
    }

    setIsSubmitting(true);
    setResponseType(null);
    setResponseMessage(null);
    setResponsePreview(null);

    const result = await registerUser({
      username,
      email,
      password: form.password,
    });

    setResponseType(result.ok ? "success" : "error");
    setResponseMessage(result.message);
    setResponsePreview(extractResponsePreview(result.data));
    setIsSubmitting(false);
  };

  return (
    <Card className="w-full rounded-2xl border border-outline-variant bg-surface-container-lowest p-6 shadow-lg sm:p-8 font-body text-on-surface">
      <CardContent className="space-y-6">
        {/* Header */}
        <div className="space-y-2 text-center sm:text-left">
          <h2 className="font-headline text-2xl sm:text-3xl font-bold tracking-tight text-on-surface">
            Create Account
          </h2>
        </div>

        {/* Form */}
        <form className="space-y-5" onSubmit={handleSubmit}>
          {/* Username */}
          <div className="space-y-2">
            <Label htmlFor="username" className="text-on-surface">
              Username
            </Label>
            <Input
              id="username"
              value={form.username}
              onChange={(event) =>
                setForm((previous) => ({
                  ...previous,
                  username: event.target.value,
                }))
              }
              disabled={isSubmitting}
              autoComplete="username"
              placeholder="phuquy123"
              className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
            />
          </div>

          {/* Email */}
          <div className="space-y-2">
            <Label htmlFor="email" className="text-on-surface">
              Email
            </Label>
            <Input
              id="email"
              type="email"
              value={form.email}
              onChange={(event) =>
                setForm((previous) => ({
                  ...previous,
                  email: event.target.value,
                }))
              }
              disabled={isSubmitting}
              autoComplete="email"
              placeholder="name@example.com"
              className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
            />
          </div>

          {/* Password */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="password" className="text-on-surface">
                Password
              </Label>
              <Input
                id="password"
                type="password"
                value={form.password}
                onChange={(event) =>
                  setForm((previous) => ({
                    ...previous,
                    password: event.target.value,
                  }))
                }
                disabled={isSubmitting}
                autoComplete="new-password"
                className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirm" className="text-on-surface">
                Confirm
              </Label>
              <Input
                id="confirm"
                type="password"
                value={form.confirmPassword}
                onChange={(event) =>
                  setForm((previous) => ({
                    ...previous,
                    confirmPassword: event.target.value,
                  }))
                }
                disabled={isSubmitting}
                autoComplete="new-password"
                className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
              />
            </div>
          </div>

          {/* Terms */}
          <div className="flex items-start gap-2">
            <Checkbox
              className="border-black"
              id="terms"
              checked={form.acceptedTerms}
              disabled={isSubmitting}
              onCheckedChange={(checked) =>
                setForm((previous) => ({
                  ...previous,
                  acceptedTerms: checked === true,
                }))
              }
            />

            <Label
              htmlFor="terms"
              className="text-sm text-on-surface-variant leading-relaxed"
            >
              I agree to the{" "}
              <Link
                to={PATHS.TERMS}
                className="text-primary font-medium hover:underline cursor-pointer"
              >
                Terms
              </Link>{" "}
              and{" "}
              <Link
                to={PATHS.PRIVACY}
                className="text-primary font-medium hover:underline cursor-pointer"
              >
                Privacy Policy
              </Link>
            </Label>
          </div>

          {/* Submit */}
          <Button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-full bg-primary py-6 text-base hover:bg-primary-dim"
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

          {/* Social */}
          <div className="grid grid-cols-2 gap-3">
            <Button
              variant="outline"
              className="border-outline-variant bg-surface-container-lowest text-on-surface hover:bg-surface-container"
            >
              Google
            </Button>
            <Button
              variant="outline"
              className="border-outline-variant bg-surface-container-lowest text-on-surface hover:bg-surface-container"
            >
              GitHub
            </Button>
          </div>

          {responseType && responseMessage ? (
            <Alert
              variant={responseType === "error" ? "destructive" : "default"}
              className={
                responseType === "success"
                  ? "border-primary/25 bg-primary-fixed/20 text-on-surface"
                  : ""
              }
            >
              <AlertTitle>
                {responseType === "success"
                  ? "Registration response"
                  : "Registration failed"}
              </AlertTitle>
              <AlertDescription className="mt-1 text-on-surface-variant space-y-2">
                <p>{responseMessage}</p>
                {responsePreview ? (
                  <pre className="overflow-auto rounded-md border border-outline-variant bg-surface-container px-3 py-2 text-xs text-on-surface">
                    {responsePreview}
                  </pre>
                ) : null}
              </AlertDescription>
            </Alert>
          ) : null}
        </form>

        {/* Footer */}
        <p className="text-sm text-center text-on-surface-variant">
          Already have an account?{" "}
          <Link
            to={PATHS.LOGIN}
            className="text-primary font-semibold hover:underline cursor-pointer"
          >
            Sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
