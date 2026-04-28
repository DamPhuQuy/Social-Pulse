import React, { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Mail, ArrowRight, Activity, ChevronLeft, KeyRound } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PATHS } from "@/constants/paths";
import { forgotPassword } from "@/services/auth/authService";
import { InteractiveBackground } from "@/components/auth/InteractiveBackground";
import type { FormSubmitEvent } from "@/types/form";

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
      toast.success("OTP sent!", { description: "Please check your email inbox for the reset code." });
      navigate(`${PATHS.RESET_PASSWORD}?email=${encodeURIComponent(trimmedEmail)}`);
    } else {
      toast.error("Failed to send reset code.", { description: result.message });
    }
    setIsSubmitting(false);
  };

  return (
    <div className="min-h-screen w-full relative flex items-center justify-center font-['Outfit'] bg-white overflow-hidden">
      <InteractiveBackground />

      {/* Decorative Glows */}
      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 rounded-full blur-[160px] -z-10" />

      {/* Navigation / Logo */}
      <nav className="absolute top-0 left-0 w-full p-8 flex items-center justify-between z-50">
        <Link to={PATHS.ONBOARDING} className="flex items-center gap-3 group">
          <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
            <Activity className="text-white w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-gray-900">SocialPulse</span>
        </Link>
        <Link to={PATHS.LOGIN} className="flex items-center gap-2 text-sm font-bold text-gray-600 hover:text-blue-600 transition-colors group">
          <ChevronLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
          Back to Login
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
            <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
              <KeyRound className="w-8 h-8 text-blue-600" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900 mb-3 tracking-tight">Forgot Password?</h1>
            <p className="text-gray-500 font-medium max-w-sm mx-auto">
              No worries, it happens. Enter your email and we'll send you a reset code.
            </p>
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
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-12 pr-4 py-6 bg-white/50 border-gray-100 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all"
                  required
                />
              </div>
            </div>

            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-7 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-bold text-lg shadow-xl shadow-blue-500/20 transition-all flex items-center justify-center gap-2 group"
            >
              {isSubmitting ? "Sending Code..." : (
                <>
                  Send Reset Code <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </Button>
          </form>
        </div>

        {/* Footer info */}
        <p className="text-center mt-10 text-[10px] text-gray-400 font-bold tracking-[0.3em] uppercase opacity-60">
          Encrypted Reset Protocol Active
        </p>
      </motion.div>
    </div>
  );
}
