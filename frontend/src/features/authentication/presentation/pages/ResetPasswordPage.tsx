import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Lock, ArrowRight, Activity, ChevronLeft, KeyRound, CheckCircle2, Eye, EyeOff } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { PATHS } from "@/shared/constants/paths";
import { resetPassword } from "@/features/authentication/infrastructure/api/authService";
import { InteractiveBackground } from "@/features/authentication/presentation/components/InteractiveBackground";
import type { FormSubmitEvent } from "@/core/types/form";

const RESET_VERIFIED_FLAG_KEY = "pendingResetVerified";
const RESET_OTP_CODE_KEY = "pendingResetOtpCode";

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const navigationState = useMemo(
    () => location.state as { email?: string; verified?: boolean } | null,
    [location.state],
  );

  const email = useMemo(() => {
    const fromState = navigationState?.email?.trim() ?? "";
    if (fromState) return fromState;
    return (
      new URLSearchParams(location.search).get("email")?.trim() ??
      sessionStorage.getItem("pendingResetEmail")?.trim() ??
      ""
    );
  }, [location.search, navigationState]);

  const isOtpVerified = useMemo(
    () =>
      Boolean(navigationState?.verified) ||
      sessionStorage.getItem(RESET_VERIFIED_FLAG_KEY) === "1",
    [navigationState],
  );

  const otpCode = useMemo(
    () => sessionStorage.getItem(RESET_OTP_CODE_KEY)?.trim() ?? "",
    [],
  );

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (!email || isOtpVerified) return;
    toast.error("Yêu cầu xác thực OTP.");
    navigate(`${PATHS.FORGOT_PASSWORD}?email=${encodeURIComponent(email)}`, { replace: true });
  }, [email, isOtpVerified, navigate]);

  useEffect(() => {
    if (!isSuccess) return;
    const loginPath = email ? `${PATHS.LOGIN}?email=${encodeURIComponent(email)}` : PATHS.LOGIN;
    const id = setTimeout(() => {
      sessionStorage.removeItem("pendingResetEmail");
      sessionStorage.removeItem(RESET_VERIFIED_FLAG_KEY);
      sessionStorage.removeItem(RESET_OTP_CODE_KEY);
      navigate(loginPath);
    }, 2000);
    return () => clearTimeout(id);
  }, [email, isSuccess, navigate]);

  const handleSubmit = async (event: FormSubmitEvent) => {
    event.preventDefault();

    if (!email || !isOtpVerified || !otpCode) {
      toast.error("Quá trình bị gián đoạn. Vui lòng bắt đầu lại.");
      return;
    }

    if (newPassword.length < 6) {
      toast.error("Mật khẩu phải có ít nhất 6 ký tự.");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Mật khẩu không khớp.");
      return;
    }

    setIsSubmitting(true);
    const result = await resetPassword({ email, otpCode, newPassword });

    if (result.ok) {
      setIsSuccess(true);
      toast.success("Đã cập nhật Bảo mật", { description: "Mật khẩu của bạn đã được thay đổi. Đang chuyển đến trang đăng nhập..." });
    } else {
      toast.error("Khôi phục thất bại.", { description: result.message });
    }
    setIsSubmitting(false);
  };

  return (
    <div className="min-h-screen w-full relative flex items-center justify-center font-sans bg-white dark:bg-slate-950 transition-colors duration-500 overflow-hidden">
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
        <Link to={PATHS.LOGIN} className="flex items-center gap-2 text-sm font-bold text-gray-600 dark:text-slate-400 hover:text-blue-600 transition-colors group">
          <ChevronLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
          Hủy
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
              {isSuccess ? (
                <CheckCircle2 className="w-8 h-8 text-green-500" />
              ) : (
                <KeyRound className="w-8 h-8 text-blue-600 dark:text-blue-400" />
              )}
            </div>
            <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-3 tracking-tight">
              {isSuccess ? "Bảo mật!" : "Mật khẩu Mới"}
            </h1>
            <p className="text-gray-500 dark:text-slate-400 font-medium max-w-sm mx-auto">
              {isSuccess 
                ? "Hồ sơ bảo mật của bạn đã được cập nhật. Đang chuyển hướng đến đăng nhập..."
                : "Chọn một mật khẩu mạnh mà bạn không sử dụng ở nơi khác."}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="new-password" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Mật khẩu Mới</Label>
              <div className="relative group">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="new-password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="pl-12 pr-12 py-6 bg-white/50 dark:bg-slate-800/50 border-gray-100 dark:border-slate-700 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all dark:text-white"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirm-password" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Xác nhận Mật khẩu Mới</Label>
              <div className="relative group">
                <CheckCircle2 className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="confirm-password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
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

            <Button
              type="submit"
              disabled={isSubmitting || isSuccess}
              className="w-full py-7 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-bold text-lg shadow-xl shadow-blue-500/20 dark:shadow-none transition-all flex items-center justify-center gap-2 group"
            >
              {isSubmitting ? "Đang cập nhật..." : isSuccess ? "Thành công" : (
                <>
                  Cập nhật Mật khẩu <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </Button>
          </form>
        </div>

        <p className="text-center mt-10 text-[10px] text-gray-400 font-bold tracking-[0.3em] uppercase opacity-60">
          Giao thức Bảo mật đã được Khởi tạo lại
        </p>
      </motion.div>
    </div>
  );
}
