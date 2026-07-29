import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { User, Mail, Lock, Eye, EyeOff, Activity, ArrowRight, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { Checkbox } from "@/shared/components/ui/checkbox";
import { PATHS } from "@/shared/constants/paths";
import { registerUser } from "@/features/authentication/infrastructure/api/authService";
import { InteractiveBackground } from "@/features/authentication/presentation/components/InteractiveBackground";
import type { FormSubmitEvent } from "@/core/types/form";

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
      toast.error("Vui lòng điền vào tất cả các trường bắt buộc.");
      return;
    }

    if (username.length < 3 || username.length > 27) {
      toast.error("Tên người dùng phải từ 3 đến 27 ký tự.");
      return;
    }

    const usernameRegex = /^[a-zA-Z0-9_]*$/;
    if (!usernameRegex.test(username)) {
      toast.error("Tên người dùng chỉ được chứa chữ cái không dấu, số và dấu gạch dưới.");
      return;
    }

    if (form.rawPassword !== form.confirmPassword) {
      toast.error("Mật khẩu không khớp.");
      return;
    }

    if (!form.acceptedTerms) {
      toast.error("Vui lòng đồng ý với Điều khoản và Chính sách Bảo mật.");
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
      toast.success("Đăng ký thành công.", { description: "Vui lòng xác thực email của bạn." });
      sessionStorage.setItem("pendingVerificationEmail", email);
      navigate(`${PATHS.VERIFY_EMAIL}?email=${encodeURIComponent(email)}`);
    } else {
      toast.error("Đăng ký thất bại.", { description: result.message });
    }
    setIsSubmitting(false);
  };

  return (
    <div className="min-h-screen w-full relative flex items-center justify-center font-sans bg-white dark:bg-slate-950 transition-colors duration-500 overflow-hidden py-20">
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
          Đăng nhập
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
            <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-3 tracking-tight">Tạo tài khoản</h1>
            <p className="text-gray-500 dark:text-slate-400 font-medium">Tham gia mạng xã hội thế hệ mới</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <div className="flex justify-between items-center ml-1">
                <Label htmlFor="username" className="text-sm font-bold text-gray-700 dark:text-slate-300">Tên người dùng</Label>
                <span className="text-[10px] font-bold text-gray-400 dark:text-neutral-500">
                  {form.username.length}/27
                </span>
              </div>
              <div className="relative group">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
                <Input
                  id="username"
                  placeholder="johndoe"
                  value={form.username}
                  maxLength={27}
                  onChange={(e) => setForm(prev => ({ ...prev, username: e.target.value }))}
                  className="pl-12 pr-4 py-6 bg-white/50 dark:bg-slate-800/50 border-gray-100 dark:border-slate-700 rounded-2xl focus:ring-blue-500 focus:border-blue-500 transition-all dark:text-white"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="email" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Địa chỉ Email</Label>
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
              <Label htmlFor="password" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Mật khẩu</Label>
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
              <Label htmlFor="confirmPassword" className="text-sm font-bold text-gray-700 dark:text-slate-300 ml-1">Xác nhận Mật khẩu</Label>
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
                className="w-6 h-6 border-2 border-slate-300 dark:border-slate-700 rounded-[4px] data-[state=checked]:bg-blue-600 data-[state=checked]:border-blue-600 transition-all cursor-pointer shrink-0"
              />
              <Label htmlFor="terms" className="text-sm text-gray-500 dark:text-slate-400 font-medium cursor-pointer select-none whitespace-nowrap">
                Đồng ý{" "}
                <Link to={PATHS.TERMS} className="text-blue-600 dark:text-blue-400 font-bold hover:underline">Điều khoản Dịch vụ</Link>{" "}
                và{" "}
                <Link to={PATHS.PRIVACY} className="text-blue-600 dark:text-blue-400 font-bold hover:underline">Chính sách Bảo mật</Link>.
              </Label>
            </div>

            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-7 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-bold text-lg shadow-xl shadow-blue-500/20 dark:shadow-none transition-all flex items-center justify-center gap-2 group"
            >
              {isSubmitting ? "Đang tạo tài khoản..." : (
                <>
                  Đăng ký <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </Button>
          </form>

        </div>

        {/* Footer info */}
        <p className="text-center mt-10 text-[10px] text-gray-400 font-bold tracking-[0.3em] uppercase opacity-60">
          Quá trình đăng ký liên kết bảo mật
        </p>
      </motion.div>
    </div>
  );
}
