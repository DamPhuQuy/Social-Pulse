import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Mail, Activity, ChevronLeft, ShieldCheck, RefreshCcw } from "lucide-react";
import { toast } from "sonner";
import { PATHS } from "@/constants/paths";
import { verifyEmailOtp, resendOtp } from "@/services/auth/authService";
import { InteractiveBackground } from "@/components/auth/InteractiveBackground";
import { OtpBlock } from "@/components/auth/OtpBlock";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 60;
const MAX_FAILED_ATTEMPTS = 3;

function formatCountdown(seconds: number): string {
  const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
  const ss = String(seconds % 60).padStart(2, "0");
  return `${mm}:${ss}`;
}

export default function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const email = useMemo(() => {
    const stateEmail = (location.state as { email?: string } | null)?.email?.trim();
    if (stateEmail) return stateEmail;
    const queryEmail = new URLSearchParams(location.search).get("email")?.trim();
    if (queryEmail) return queryEmail;
    return sessionStorage.getItem("pendingVerificationEmail")?.trim() ?? "";
  }, [location.search, location.state]);

  const [otp, setOtp] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);
  const [isResending, setIsResending] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [isVerified, setIsVerified] = useState(false);

  const lastSubmittedOtpRef = useRef<string | null>(null);
  const hasReachedMaxAttempts = failedAttempts >= MAX_FAILED_ATTEMPTS;

  useEffect(() => {
    if (email) sessionStorage.setItem("pendingVerificationEmail", email);
  }, [email]);

  // Redirect to Login after successful verification (As requested: Register -> Verify -> Login)
  useEffect(() => {
    if (!isVerified) return;
    const id = setTimeout(() => {
      navigate(`${PATHS.LOGIN}?email=${encodeURIComponent(email)}`);
    }, 2000);
    return () => clearTimeout(id);
  }, [email, isVerified, navigate]);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(id);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [secondsLeft]);

  const submitOtpVerification = useCallback(
    async (otpCode: string) => {
      if (!email) {
        toast.error("Missing email context.");
        return;
      }

      setIsVerifying(true);
      const result = await verifyEmailOtp({ email, otpCode });

      if (result.ok) {
        setIsVerified(true);
        sessionStorage.removeItem("pendingVerificationEmail");
        toast.success("Identity Verified", { description: "Your account is now active. Redirecting to Login..." });
      } else {
        const nextAttempts = failedAttempts + 1;
        setFailedAttempts(nextAttempts);
        const attemptsLeft = MAX_FAILED_ATTEMPTS - nextAttempts;
        
        if (attemptsLeft > 0) {
          toast.error("Invalid Code", { description: `You have ${attemptsLeft} attempt(s) remaining.` });
        } else {
          toast.error("Verification Locked", { description: "Please resend a new OTP code." });
        }
        setOtp("");
        lastSubmittedOtpRef.current = null;
      }
      setIsVerifying(false);
    },
    [email, failedAttempts]
  );

  useEffect(() => {
    if (otp.length !== OTP_LENGTH) return;
    if (isVerifying || isResending || hasReachedMaxAttempts || !email || isVerified) return;
    if (otp === lastSubmittedOtpRef.current) return;

    lastSubmittedOtpRef.current = otp;
    void submitOtpVerification(otp);
  }, [email, hasReachedMaxAttempts, isResending, isVerifying, otp, submitOtpVerification, isVerified]);

  const handleResendOtp = async () => {
    if (!email) return;
    setIsResending(true);
    const result = await resendOtp({ email });
    setIsResending(false);

    if (result.ok) {
      setSecondsLeft(RESEND_SECONDS);
      setOtp("");
      setFailedAttempts(0);
      lastSubmittedOtpRef.current = null;
      setIsVerified(false);
      toast.success("New code sent", { description: "Please check your inbox." });
    } else {
      toast.error("Failed to resend", { description: result.message });
    }
  };

  return (
    <div className="min-h-screen w-full relative flex items-center justify-center font-['Outfit'] bg-white dark:bg-slate-950 transition-colors duration-500 overflow-hidden">
      <InteractiveBackground />

      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 dark:bg-blue-900/10 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 dark:bg-purple-900/10 rounded-full blur-[160px] -z-10" />

      <nav className="absolute top-0 left-0 w-full p-8 flex items-center justify-between z-50">
        <Link to={PATHS.ONBOARDING} className="flex items-center gap-3 group">
          <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
            <Activity className="text-white w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">SocialPulse</span>
        </Link>
        <Link to={PATHS.REGISTER} className="flex items-center gap-2 text-sm font-bold text-gray-600 dark:text-slate-400 hover:text-blue-600 transition-colors group">
          <ChevronLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
          Back to Register
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
            <div className="w-16 h-16 bg-blue-50 dark:bg-blue-900/20 rounded-2xl flex items-center justify-center mx-auto mb-6">
              {isVerified ? (
                <ShieldCheck className="w-8 h-8 text-green-500" />
              ) : (
                <Mail className="w-8 h-8 text-blue-600 dark:text-blue-400" />
              )}
            </div>
            <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-3 tracking-tight">
              {isVerified ? "Verified!" : "Check Email"}
            </h1>
            <p className="text-gray-500 dark:text-slate-400 font-medium max-w-sm mx-auto">
              {isVerified 
                ? "Your account is ready. Taking you to the login screen..."
                : `We've sent a 6-digit code to ${email}. Enter it below to continue.`}
            </p>
          </div>

          <div className="space-y-8">
            <OtpBlock
              value={otp}
              onChange={setOtp}
              length={OTP_LENGTH}
              disabled={isResending || isVerifying || hasReachedMaxAttempts || !email || isVerified}
            />

            <div className="flex flex-col items-center gap-4">
              <div className="flex items-center justify-center gap-2 text-sm font-medium">
                <span className="text-gray-400">
                  {secondsLeft > 0 ? `Code expires in ${formatCountdown(secondsLeft)}` : "Didn't get a code?"}
                </span>
                <button
                  type="button"
                  disabled={secondsLeft > 0 || isResending || isVerified}
                  onClick={handleResendOtp}
                  className="text-blue-600 font-bold hover:underline disabled:opacity-50 flex items-center gap-1"
                >
                  {isResending ? (
                    <RefreshCcw className="w-3 h-3 animate-spin" />
                  ) : null}
                  Resend Code
                </button>
              </div>

              {!isVerified && (
                <p className="text-[11px] font-bold text-gray-400 uppercase tracking-widest">
                  {isVerifying ? "Securing connection..." : `${MAX_FAILED_ATTEMPTS - failedAttempts} attempts left`}
                </p>
              )}
            </div>
          </div>
        </div>

        <p className="text-center mt-10 text-[10px] text-gray-400 font-bold tracking-[0.3em] uppercase opacity-60">
          Pulse-Sync Verification Active
        </p>
      </motion.div>
    </div>
  );
}
