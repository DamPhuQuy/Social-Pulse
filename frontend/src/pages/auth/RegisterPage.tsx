import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { User, Mail, Lock, Eye, EyeOff, Activity, ArrowRight, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { PATHS } from "@/constants/paths";
import { registerUser } from "@/services/auth/authService";
import { InteractiveBackground } from "@/components/auth/InteractiveBackground";
import type { FormSubmitEvent } from "@/types/form";

export interface RegisterFormState {
  username: string;
  email: string;
  rawPassword: string;
  confirmPassword: string;
  acceptedTerms: boolean;
}

const INITIAL_FORM: RegisterFormState = {
  username: "",
  email: "",
  rawPassword: "",
  confirmPassword: "",
  acceptedTerms: false,
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<RegisterFormState>(INITIAL_FORM);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();
    const username = form.username.trim();
    const email = form.email.trim();

    if (!username || !email || !form.rawPassword || !form.confirmPassword) {
      toast.error("Please fill in all required fields.");
      return;
    }

    if (form.rawPassword !== form.confirmPassword) {
      toast.error("Passwords do not match.");
      return;
    }

    if (!form.acceptedTerms) {
      toast.error("Please accept the Terms and Privacy Policy.");
      return;
    }

    setIsSubmitting(true);
    const result = await registerUser({
      username,
      email,
      rawPassword: form.rawPassword,
      confirmPassword: form.confirmPassword,
    });

    if (result.ok) {
      toast.success("Registration successful.", { description: "Please verify your email." });
      sessionStorage.setItem("pendingVerificationEmail", email);
      navigate(`${PATHS.VERIFY_EMAIL}?email=${encodeURIComponent(email)}`);
    } else {
      toast.error("Registration failed.", { description: result.message });
    }
    setIsSubmitting(false);
  };

  return (
    <div className="min-h-screen w-full relative flex items-center justify-center font-['Outfit'] bg-white dark:bg-slate-950 transition-colors duration-500 overflow-hidden py-20">
      <InteractiveBackground />

      {/* Decorative Glows */}
      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 dark:bg-blue-900/10 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 dark:bg-purple-900/10 rounded-full blur-[160px] -z-10" />

      {/* Navigation / Logo */}
      <nav className="absolute top-0 left-0 w-full p-8 flex items-center justify-between z-50">
        <Link to={PATHS.ONBOARDING} className="flex items-center gap-3 group">
          <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
            <Activity className="text-white w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">SocialPulse</span>
        </Link>
        <Link to={PATHS.LOGIN} className="text-sm font-bold text-blue-600 dark:text-blue-400 hover:text-blue-700 transition-colors">
          Sign In Instead
        </Link>
      </nav>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        className="relative z-10 w-full max-w-xl px-6"
      >
        <div className="bg-white/70 dark:bg-slate-900/70 backdrop-blur-2xl border border-slate-300 dark:border-slate-800 rounded-[3rem] p-12 shadow-2xl shadow-blue-500/5">
          <div className="text-center mb-10">
            <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-3 tracking-tight">Create Account</h1>
            <p className="text-gray-500 dark:text-slate-400 font-medium">Join the next generation of social networking</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="username" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Username</Label>
              <div className="relative group">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="username"
                  placeholder="johndoe"
                  value={form.username}
                  onChange={(e) => setForm(prev => ({ ...prev, username: e.target.value }))}
                  className="pl-12 pr-4 py-6 bg-white/50 dark:bg-slate-800/50 border-gray-100 dark:border-slate-700 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all dark:text-white"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="email" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Email Address</Label>
              <div className="relative group">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="email"
                  type="email"
                  placeholder="name@example.com"
                  value={form.email}
                  onChange={(e) => setForm(prev => ({ ...prev, email: e.target.value }))}
                  className="pl-12 pr-4 py-6 bg-white/50 dark:bg-slate-800/50 border-gray-100 dark:border-slate-700 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all dark:text-white"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Password</Label>
              <div className="relative group">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={form.rawPassword}
                  onChange={(e) => setForm(prev => ({ ...prev, rawPassword: e.target.value }))}
                  className="pl-12 pr-12 py-6 bg-white/50 dark:bg-slate-800/50 border-gray-100 dark:border-slate-700 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all dark:text-white"
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

            <div className="space-y-2">
              <Label htmlFor="confirmPassword" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Confirm Password</Label>
              <div className="relative group">
                <CheckCircle2 className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={form.confirmPassword}
                  onChange={(e) => setForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
                  className="pl-12 pr-12 py-6 bg-white/50 dark:bg-slate-800/50 border-gray-100 dark:border-slate-700 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all dark:text-white"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div className="flex items-center gap-3 py-2">
              <Checkbox
                id="terms"
                checked={form.acceptedTerms}
                onCheckedChange={(checked) => setForm(prev => ({ ...prev, acceptedTerms: !!checked }))}
                className="w-6 h-6 border-2 border-slate-300 dark:border-slate-700 rounded-[4px] data-[state=checked]:bg-blue-600 data-[state=checked]:border-blue-600 transition-all cursor-pointer"
              />
              <Label htmlFor="terms" className="text-base text-gray-500 dark:text-slate-400 font-medium cursor-pointer select-none">
                I agree to the{" "}
                <Link to={PATHS.TERMS} className="text-blue-600 dark:text-blue-400 font-bold hover:underline">Terms of Service</Link>{" "}
                and{" "}
                <Link to={PATHS.PRIVACY} className="text-blue-600 dark:text-blue-400 font-bold hover:underline">Privacy Policy</Link>.
              </Label>
            </div>

            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-7 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-bold text-lg shadow-xl shadow-blue-500/20 dark:shadow-none transition-all flex items-center justify-center gap-2 group"
            >
              {isSubmitting ? "Creating account..." : (
                <>
                  Join the Pulse <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </Button>
          </form>

        </div>

        {/* Footer info */}
        <p className="text-center mt-10 text-[10px] text-gray-400 font-bold tracking-[0.3em] uppercase opacity-60">
          Neural-Linked Registration Sequence
        </p>
      </motion.div>
    </div>
  );
}
