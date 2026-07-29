import { useState, useEffect } from "react";
import { AlertTriangle, Loader2, Settings, Shield, UserX, Key } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/shared/components/AppHeader";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { logoutUser } from "@/features/authentication/infrastructure/api/authService";
import { changePassword, deleteProfile, getUserProfileById, type UserProfile } from "@/features/profiles/infrastructure/api/userService";
import { getBlockedUserIds, unblockUser } from "@/features/social-relations/infrastructure/api/blockService";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";

type ActiveTab = "password" | "blocks" | "danger";

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

  // Blocked Users States
  const [blockedUsers, setBlockedUsers] = useState<UserProfile[]>([]);
  const [blocksLoading, setBlocksLoading] = useState(false);

  const loadBlockedUsers = async () => {
    setBlocksLoading(true);
    const res = await getBlockedUserIds();
    if (res.ok && res.data) {
      const ids = res.data;
      const profiles = await Promise.all(
        ids.map(async (id) => {
          const uRes = await getUserProfileById(id);
          if (uRes.ok && uRes.data) {
            return uRes.data;
          }
          return {
            userId: id,
            username: `user_${id}`,
            displayName: `Người dùng ${id}`,
            bio: null,
            avatarUrl: null,
            coverImageUrl: null,
            dob: null,
            gender: null,
            postCount: 0,
            followers: 0,
            following: 0,
            isFollowing: false,
            avatarPublicId: null,
            coverImagePublicId: null,
          } as UserProfile;
        })
      );
      setBlockedUsers(profiles);
    }
    setBlocksLoading(false);
  };

  useEffect(() => {
    if (activeTab === "blocks") {
      loadBlockedUsers();
    }
  }, [activeTab]);

  const handleUnblock = async (userId: number) => {
    const res = await unblockUser(userId);
    if (res.ok) {
      toast.success("Đã bỏ chặn người dùng này!");
      setBlockedUsers(prev => prev.filter(u => u.userId !== userId));
    } else {
      toast.error(res.message ?? "Bỏ chặn thất bại.");
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword || !confirmPassword) {
      toast.error("Vui lòng điền đầy đủ thông tin.");
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error("Mật khẩu mới không trùng khớp.");
      return;
    }
    if (newPassword.length < 6) {
      toast.error("Mật khẩu mới phải từ 6 ký tự trở lên.");
      return;
    }

    setPasswordLoading(true);
    const res = await changePassword({ currentPassword, newPassword, confirmPassword });
    setPasswordLoading(false);
    if (!res.ok) {
      toast.error(res.message ?? "Không thể đổi mật khẩu.");
      return;
    }
    toast.success("Thay đổi mật khẩu thành công!");
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
  };

  const handleDeleteProfile = async () => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa hồ sơ hiện tại? Hành động này sẽ tác động dữ liệu thật.")) {
      return;
    }

    setDeleting(true);
    const res = await deleteProfile();
    setDeleting(false);
    if (!res.ok) {
      toast.error(res.message ?? "Không thể xóa hồ sơ.");
      return;
    }

    await logoutUser();
    logout();
    setAccessToken(null);
    toast.success("Đã xóa hồ sơ thành công.");
    navigate(PATHS.LOGIN);
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="settings" />

        <div className="min-w-0 space-y-6 pb-24 lg:pb-10">
          {/* Header Card */}
          <div className="flex items-center space-x-3 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
            <div className="p-3 bg-blue-500/10 text-blue-500 rounded-xl">
              <Settings className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight">Cài đặt thiết lập</h1>
              <p className="text-slate-500 dark:text-neutral-400 text-xs mt-0.5">
                Quản lý bảo mật tài khoản, danh sách chặn người dùng và tùy chọn nâng cao.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-[240px_1fr] gap-6 items-start">
            {/* Setting Navigation Tabs */}
            <div className="bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] rounded-2xl p-4 flex flex-col gap-1.5 shadow-sm">
              <button
                onClick={() => setActiveTab("password")}
                className={`flex items-center gap-2.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-left transition ${activeTab === "password"
                    ? "bg-blue-600 text-white shadow-lg shadow-blue-600/15"
                    : "hover:bg-slate-50 dark:hover:bg-neutral-800 text-slate-700 dark:text-neutral-300"
                  }`}
              >
                <Key className="w-4 h-4" />
                Mật khẩu & Bảo mật
              </button>
              <button
                onClick={() => setActiveTab("blocks")}
                className={`flex items-center gap-2.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-left transition ${activeTab === "blocks"
                    ? "bg-blue-600 text-white shadow-lg shadow-blue-600/15"
                    : "hover:bg-slate-50 dark:hover:bg-neutral-800 text-slate-700 dark:text-neutral-300"
                  }`}
              >
                <UserX className="w-4 h-4" />
                Danh sách chặn
              </button>
              <button
                onClick={() => setActiveTab("danger")}
                className={`flex items-center gap-2.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-left transition ${activeTab === "danger"
                    ? "bg-red-600 text-white shadow-lg shadow-red-600/25"
                    : "hover:bg-red-50 dark:hover:bg-red-500/10 text-red-600"
                  }`}
              >
                <AlertTriangle className="w-4 h-4" />
                Vùng nguy hiểm
              </button>
            </div>

            {/* Tab Panels */}
            <div className="bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] rounded-2xl p-6 shadow-sm min-h-[300px]">

              {/* Tab 1: Change Password */}
              {activeTab === "password" && (
                <form onSubmit={handleChangePassword} className="space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-slate-100 dark:border-neutral-800 mb-4">
                    <Shield className="w-5 h-5 text-blue-500" />
                    <h3 className="font-bold text-base text-slate-800 dark:text-white">Đổi mật khẩu tài khoản</h3>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-500 dark:text-neutral-450 uppercase tracking-wide">Mật khẩu hiện tại</label>
                    <input
                      type="password"
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-slate-50 dark:bg-neutral-850 border border-slate-200 dark:border-neutral-800 focus:border-blue-500 rounded-xl px-4 py-2.5 text-sm outline-none transition"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-500 dark:text-neutral-450 uppercase tracking-wide">Mật khẩu mới</label>
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Từ 6 ký tự trở lên..."
                      className="w-full bg-slate-50 dark:bg-neutral-850 border border-slate-200 dark:border-neutral-800 focus:border-blue-500 rounded-xl px-4 py-2.5 text-sm outline-none transition"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-500 dark:text-neutral-450 uppercase tracking-wide">Xác nhận mật khẩu mới</label>
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Nhập lại mật khẩu mới..."
                      className="w-full bg-slate-50 dark:bg-neutral-850 border border-slate-200 dark:border-neutral-800 focus:border-blue-500 rounded-xl px-4 py-2.5 text-sm outline-none transition"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={passwordLoading}
                    className="flex items-center gap-2 rounded-xl bg-blue-600 hover:bg-blue-700 px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50 mt-4 transition shadow-lg shadow-blue-600/15"
                  >
                    {passwordLoading && <Loader2 className="h-4 w-4 animate-spin" />}
                    Cập nhật mật khẩu
                  </button>
                </form>
              )}

              {/* Tab 2: Blocked Users */}
              {activeTab === "blocks" && (
                <div className="space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-slate-100 dark:border-neutral-800 mb-4">
                    <UserX className="w-5 h-5 text-blue-500" />
                    <h3 className="font-bold text-base text-slate-800 dark:text-white">Người dùng đã chặn</h3>
                  </div>

                  {blocksLoading ? (
                    <div className="text-center py-10 text-slate-500">
                      <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-blue-500" />
                      Đang tải danh sách chặn...
                    </div>
                  ) : blockedUsers.length === 0 ? (
                    <div className="text-center py-10 text-slate-400">
                      Không có người dùng nào bị chặn.
                    </div>
                  ) : (
                    <div className="divide-y divide-slate-100 dark:divide-neutral-800">
                      {blockedUsers.map((user) => (
                        <div key={user.userId} className="flex items-center justify-between py-3">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full overflow-hidden shrink-0 bg-slate-100 dark:bg-neutral-850">
                              <SafeAvatar src={user.avatarUrl} alt={user.username} />
                            </div>
                            <div>
                              <span className="font-bold text-sm text-slate-800 dark:text-slate-200">
                                {user.displayName || user.username}
                              </span>
                              <p className="text-xs text-slate-450">@{user.username}</p>
                            </div>
                          </div>
                          <button
                            onClick={() => handleUnblock(user.userId)}
                            className="px-3 py-1.5 bg-slate-100 dark:bg-neutral-850 hover:bg-red-50 dark:hover:bg-red-500/10 text-xs font-semibold text-slate-700 dark:text-neutral-300 hover:text-red-600 rounded-lg transition"
                          >
                            Bỏ chặn
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Tab 3: Danger Zone */}
              {activeTab === "danger" && (
                <div className="space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-slate-100 dark:border-neutral-800 mb-4 text-red-600">
                    <AlertTriangle className="w-5 h-5" />
                    <h3 className="font-bold text-base">Vùng nguy hiểm</h3>
                  </div>

                  <p className="text-sm text-slate-500 dark:text-neutral-400">
                    Xóa hồ sơ sẽ xóa toàn bộ bài đăng, tương tác và tài khoản của bạn trên cơ sở dữ liệu thật. Hành động này không thể hoàn tác.
                  </p>

                  <button
                    onClick={handleDeleteProfile}
                    disabled={deleting}
                    className="flex items-center gap-2 rounded-xl bg-red-600 hover:bg-red-700 px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50 transition shadow-lg shadow-red-600/25"
                  >
                    {deleting && <Loader2 className="h-4 w-4 animate-spin" />}
                    Xóa vĩnh viễn tài khoản
                  </button>
                </div>
              )}

            </div>
          </div>
        </div>
      </div>
      <BottomNavBar active="settings" />
    </div>
  );
}
