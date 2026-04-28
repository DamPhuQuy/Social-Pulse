import React, { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { LogIn, Mail, Lock, Eye, EyeOff, Activity, ArrowRight } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { setApiClientToken } from "@/lib/axiosClient";
import { loginUser } from "@/services/auth/authService";
import { InteractiveBackground } from "@/components/auth/InteractiveBackground";
import type { FormSubmitEvent } from "@/types/form";

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

  const prefilledEmail = useMemo(
    () => new URLSearchParams(location.search).get("email")?.trim() ?? "",
    [location.search],
  );

  const [form, setForm] = useState<LoginFormState>({
    ...INITIAL_FORM,
    email: prefilledEmail,
  });
  const [showPassword, setShowPassword] = useState(false);
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
      toast.success("Login successful.", { description: "Welcome back to Social Pulse." });
      navigate(PATHS.ONBOARDING);
    } else {
      toast.error("Login failed.", { description: result.message });
    }
    setIsSubmitting(false);
  };

  return (
    <div className="min-h-screen w-full relative flex items-center justify-center font-['Outfit'] bg-white overflow-hidden">
      {/* Shared Interactive Background */}
      <InteractiveBackground />

      {/* Decorative Glows */}
      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 rounded-full blur-[160px] -z-10" />

      <nav className="absolute top-0 left-0 w-full p-8 flex items-center justify-between z-50">
        <Link to={PATHS.ONBOARDING} className="flex items-center gap-3 group">
          <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
            <Activity className="text-white w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-gray-900">SocialPulse</span>
        </Link>
        <Link to={PATHS.REGISTER} className="text-sm font-bold text-blue-600 hover:text-blue-700 transition-colors">
          Create Account
        </Link>
      </nav>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        className="relative z-10 w-full max-w-xl px-6"
      >
        <div className="bg-white/70 backdrop-blur-2xl border border-white/50 rounded-[3rem] p-12 shadow-2xl shadow-blue-500/5">
          <div className="text-center mb-10">
            <h1 className="text-4xl font-bold text-gray-900 mb-3 tracking-tight">Welcome back</h1>
            <p className="text-gray-500 font-medium">Reconnect with your social pulse</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="email" className="text-sm font-bold text-gray-700 ml-1">Email Address</Label>
              <div className="relative group">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="email"
                  type="email"
                  placeholder="name@example.com"
                  value={form.email}
                  onChange={(e) => setForm(prev => ({ ...prev, email: e.target.value }))}
                  className="pl-12 pr-4 py-6 bg-white/50 border-gray-100 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between ml-1">
                <Label htmlFor="password" className="text-sm font-bold text-gray-700">Password</Label>
              </div>
              <div className="relative group">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={form.password}
                  onChange={(e) => setForm(prev => ({ ...prev, password: e.target.value }))}
                  className="pl-12 pr-12 py-6 bg-white/50 border-gray-100 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-7 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-bold text-lg shadow-xl shadow-blue-500/20 transition-all flex items-center justify-center gap-2 group"
            >
              {isSubmitting ? "Signing in..." : (
                <>
                  Sign In <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </Button>
          </form>

          <div className="mt-10 pt-8 border-t border-gray-100 text-center">
            <p className="text-sm text-gray-500 font-medium">
              Having trouble?{" "}
              <Link to={PATHS.FORGOT_PASSWORD} className="text-blue-600 font-bold hover:underline">
                Reset your password
              </Link>
            </p>
          </div>
        </div>

        {/* Footer info */}
        <p className="text-center mt-10 text-[10px] text-gray-400 font-bold tracking-[0.3em] uppercase opacity-60">
          Secure AI-Powered Authentication
        </p>
      </motion.div>
    </div>
  );
}
