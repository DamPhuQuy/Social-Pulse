import Header from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { setApiClientToken } from "@/lib/axiosClient";
import { loginUser } from "@/services/authService";
import type { ComponentProps } from "react";
import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";

type LoginFormState = {
  email: string;
  password: string;
};

type FormSubmitEvent = Parameters<
  NonNullable<ComponentProps<"form">["onSubmit"]>
>[0];

const INITIAL_FORM: LoginFormState = {
  email: "",
  password: "",
};

export default function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setAccessToken } = useAuth();
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

    const result = await loginUser({ email: email, password: form.password });

    if (result.ok && result.accessToken) {
      // Lưu Access Token vào React state (in-memory) và axios client
      setAccessToken(result.accessToken);
      setApiClientToken(result.accessToken);

      toast.success("Login successful.", {
        description: "Welcome back to Social Pulse.",
      });

      navigate(PATHS.ONBOARDING);
    } else {
      toast.error("Login failed.", {
        description: result.message,
      });
    }

    setIsSubmitting(false);
  };

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 grid lg:grid-cols-2">
        <div className="flex items-center justify-center p-8 bg-surface">
          <div className="w-full max-w-md">
            <Card className="w-full rounded-3xl border border-outline-variant bg-surface-container-lowest p-6 shadow-lg sm:p-8">
              <CardContent className="space-y-6">
                <div className="space-y-2 text-center sm:text-left">
                  <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
                    Sign In
                  </h1>
                  <p className="text-sm text-on-surface-variant">
                    Login with your existing account to continue.
                  </p>
                </div>

                <form className="space-y-5" onSubmit={handleSubmit}>
                  <div className="space-y-2">
                    <Label htmlFor="login-email" className="text-on-surface">
                      Email
                    </Label>
                    <Input
                      id="login-email"
                      type="email"
                      autoComplete="email"
                      value={form.email}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          email: event.target.value,
                        }))
                      }
                      disabled={isSubmitting}
                      placeholder="name@example.com"
                      className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="login-password" className="text-on-surface">
                      Password
                    </Label>
                    <Input
                      id="login-password"
                      type="password"
                      autoComplete="current-password"
                      value={form.password}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          password: event.target.value,
                        }))
                      }
                      disabled={isSubmitting}
                      placeholder="P@ssw0rd"
                      className="border-outline-variant bg-surface-container-lowest placeholder:text-on-surface-variant focus-visible:border-primary focus-visible:ring-primary-fixed/60"
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
          </div>
        </div>

        <div className="hidden lg:flex flex-col items-center justify-center p-12 bg-surface-container-low">
          <div className="max-w-md text-center space-y-6">
            <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
              Welcome back to your{" "}
              <span className="text-primary">Social Pulse.</span>
            </h1>

            <p className="text-on-surface-variant text-lg">
              Sign in to reconnect with your communities and pick up where you
              left off.
            </p>

            <div className="pt-8">
              <img
                src="https://img.freepik.com/free-vector/flat-design-international-human-rights-day_23-2148711491.jpg?semt=ais_incoming&w=740&q=80"
                alt="Login Illustration"
                className="w-full h-auto drop-shadow-xl rounded-2xl"
              />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
