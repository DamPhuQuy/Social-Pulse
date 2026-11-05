import { useState } from "react";
import { AlertTriangle, Loader2, Key } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/shared/components/AppHeader";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { logoutUser } from "@/features/authentication/infrastructure/api/authService";
import { changePassword, deleteProfile } from "@/features/profiles/infrastructure/api/userService";

type ActiveTab = "password" | "danger";

export default function SettingsPage() {
  const navigate = useNavigate();
  const { logout, setAccessToken } = useAuth();
  const [activeTab, setActiveTab] = useState<ActiveTab>("password");

  // Danger Zone States
  const [deleting, setDeleting] = useState(false);

  // Change Password States
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordLoading, setPasswordLoading] = useState(false);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword || !confirmPassword) {
      toast.error("Vui lòng điền đầy đủ các trường mật khẩu.");
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error("Mật khẩu mới không khớp nhau.");
      return;
    }
    if (newPassword.length < 6) {
      toast.error("Mật khẩu mới phải có ít nhất 6 ký tự.");
      return;
    }

    setPasswordLoading(true);
    const res = await changePassword({
      currentPassword,
      newPassword,
      confirmPassword,
    });
    setPasswordLoading(false);

    if (res.ok) {
      toast.success("Đổi mật khẩu thành công!");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } else {
      toast.error(res.message ?? "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.");
    }
  };

  const handleDeleteAccount = async () => {
    if (!window.confirm("BẠN CÓ CHẮC CHẮN MUỐN XÓA TÀI KHOẢN? Hành động này KHÔNG THỂ hoàn tác!")) {
      return;
    }

    setDeleting(true);
    const res = await deleteProfile();
    setDeleting(false);

    if (res.ok) {
      toast.success("Tài khoản của bạn đã được xóa vĩnh viễn.");
      await logoutUser();
      setAccessToken(null);
      logout();
      navigate(PATHS.LOGIN);
    } else {
      toast.error(res.message ?? "Xóa tài khoản thất bại.");
    }
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="settings" />

        <main className="w-full max-w-4xl mx-auto pb-20">
          <div className="mb-6">
            <h1 className="text-2xl font-black text-slate-900 dark:text-white">Cài đặt tài khoản</h1>
            <p className="text-sm text-slate-500 mt-1">Quản lý bảo mật và tài khoản của bạn</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Sidebar Tabs */}
            <div className="flex flex-col gap-2 bg-white dark:bg-[#1e1e1e] p-3 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm h-fit">
              <button
                onClick={() => setActiveTab("password")}
                className={`flex items-center gap-2.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-left transition ${
                  activeTab === "password"
                    ? "bg-blue-600 text-white shadow-lg shadow-blue-600/15"
                    : "hover:bg-slate-50 dark:hover:bg-neutral-800 text-slate-700 dark:text-neutral-300"
                }`}
              >
                <Key className="w-4 h-4" />
                Mật khẩu & Bảo mật
              </button>
              <button
                onClick={() => setActiveTab("danger")}
                className={`flex items-center gap-2.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-left transition ${
                  activeTab === "danger"
                    ? "bg-red-600 text-white shadow-lg shadow-red-600/25"
                    : "hover:bg-red-50 dark:hover:bg-red-500/10 text-red-600"
                }`}
              >
                <AlertTriangle className="w-4 h-4" />
                Vùng nguy hiểm
              </button>
            </div>

            {/* Tab Contents */}
            <div className="md:col-span-2 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
              {/* Tab 1: Change Password */}
              {activeTab === "password" && (
                <form onSubmit={handleChangePassword} className="space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-slate-100 dark:border-neutral-800 mb-4">
                    <Key className="w-5 h-5 text-blue-500" />
                    <h3 className="font-bold text-base text-slate-800 dark:text-white">Đổi mật khẩu</h3>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                      Mật khẩu hiện tại
                    </label>
                    <input
                      type="password"
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 dark:bg-neutral-850 border border-slate-200 dark:border-neutral-700 rounded-xl text-sm focus:outline-none focus:border-blue-500 transition"
                      placeholder="Nhập mật khẩu hiện tại"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                      Mật khẩu mới
                    </label>
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 dark:bg-neutral-850 border border-slate-200 dark:border-neutral-700 rounded-xl text-sm focus:outline-none focus:border-blue-500 transition"
                      placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                      Xác nhận mật khẩu mới
                    </label>
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 dark:bg-neutral-850 border border-slate-200 dark:border-neutral-700 rounded-xl text-sm focus:outline-none focus:border-blue-500 transition"
                      placeholder="Nhập lại mật khẩu mới"
                    />
                  </div>

                  <div className="pt-2">
                    <button
                      type="submit"
                      disabled={passwordLoading}
                      className="flex items-center justify-center gap-2 px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-xl shadow-lg shadow-blue-600/20 disabled:opacity-50 transition"
                    >
                      {passwordLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Cập nhật mật khẩu"}
                    </button>
                  </div>
                </form>
              )}

              {/* Tab 2: Danger Zone */}
              {activeTab === "danger" && (
                <div className="space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-red-100 dark:border-red-950/40 mb-4">
                    <AlertTriangle className="w-5 h-5 text-red-500" />
                    <h3 className="font-bold text-base text-red-600">Vùng nguy hiểm</h3>
                  </div>

                  <div className="p-4 bg-red-50/50 dark:bg-red-500/5 rounded-xl border border-red-200 dark:border-red-500/20">
                    <h4 className="font-bold text-sm text-red-700 dark:text-red-400 mb-1">Xóa vĩnh viễn tài khoản</h4>
                    <p className="text-xs text-slate-600 dark:text-slate-400 mb-4">
                      Tất cả dữ liệu bài viết, tương tác của bạn sẽ bị xóa vĩnh viễn khỏi hệ thống.
                    </p>
                    <button
                      onClick={handleDeleteAccount}
                      disabled={deleting}
                      className="flex items-center justify-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-lg shadow-red-600/20 disabled:opacity-50 transition"
                    >
                      {deleting ? <Loader2 className="w-4 h-4 animate-spin" /> : "Tôi hiểu và muốn xóa tài khoản"}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </main>
      </div>
      <BottomNavBar active="settings" />
    </div>
  );
}
