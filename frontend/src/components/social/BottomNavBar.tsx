import { Bell, Compass, Home, LogOut, Plus, Settings, User } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { getUnreadNotificationCount } from "@/services/social/notificationService";
import { logoutUser } from "@/services/auth/authService";
import { toast } from "sonner";
import CreatePostModal from "@/components/post/CreatePostModal";
import { getMyProfile, type UserProfile } from "@/services/user/userService";
import { SafeAvatar } from "@/components/ui/SafeAvatar";

type SidebarKey =
  | "home"
  | "discovery"
  | "notifications"
  | "chat"
  | "bookmarks"
  | "profile"
  | "settings"
  | "admin-reports"
  | "admin-ai"
  | "admin-rbac";

interface BottomNavBarProps {
  active: SidebarKey;
}

export default function BottomNavBar({ active }: BottomNavBarProps) {
  const navigate = useNavigate();
  const { accessToken, logout, setAccessToken } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showProfileSheet, setShowProfileSheet] = useState(false);
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);

  useEffect(() => {
    if (!accessToken) {
      setCurrentUser(null);
      return;
    }

    getMyProfile().then((res) => {
      if (res.ok && res.data) setCurrentUser(res.data);
    });

    getUnreadNotificationCount().then((res) => {
      if (res.ok && res.data) setUnreadCount(res.data.unreadCount);
    });

    const handleRealtimeNotification = () => setUnreadCount((prev) => prev + 1);
    window.addEventListener("realtime:notification", handleRealtimeNotification);
    return () => window.removeEventListener("realtime:notification", handleRealtimeNotification);
  }, [accessToken]);

  const handleNav = (path: string, label: string) => {
    if (path === PATHS.HOME) { navigate(path); return; }
    if (!accessToken) {
      toast.error(`Vui lòng đăng nhập để truy cập ${label.toLowerCase()}.`);
      navigate(PATHS.LOGIN);
      return;
    }
    navigate(path);
  };

  const handleCreatePost = () => {
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập để đăng bài.");
      navigate(PATHS.LOGIN);
      return;
    }
    setShowCreateModal(true);
  };

  const handleProfileTap = () => {
    if (!accessToken) {
      navigate(PATHS.LOGIN);
      return;
    }
    // If already on profile, show quick sheet; otherwise navigate
    if (active === "profile") {
      setShowProfileSheet(true);
    } else {
      navigate(PATHS.PROFILE);
    }
  };

  const handleLogout = async () => {
    setShowProfileSheet(false);
    await logoutUser();
    setAccessToken(null);
    logout();
    navigate(PATHS.LOGIN);
  };

  return (
    <>
      <nav
        className="lg:hidden fixed bottom-0 left-0 right-0 z-50 bg-white/95 dark:bg-[#1e1e1e]/95 backdrop-blur-xl border-t border-slate-200/80 dark:border-[#2a2a2a] shadow-[0_-4px_20px_rgba(0,0,0,0.06)] dark:shadow-[0_-4px_30px_rgba(0,0,0,0.4)]"
        style={{ paddingBottom: "env(safe-area-inset-bottom, 0px)" }}
      >
        <div className="flex items-center justify-around px-2 h-16">
          {/* Home */}
          <NavBtn
            icon={Home}
            label="Trang chủ"
            active={active === "home"}
            onClick={() => handleNav(PATHS.HOME, "Trang chủ")}
          />

          {/* Discovery */}
          <NavBtn
            icon={Compass}
            label="Khám phá"
            active={active === "discovery"}
            onClick={() => handleNav(PATHS.DISCOVERY, "Khám phá")}
          />

          {/* Create Post — center elevated FAB */}
          <button
            onClick={handleCreatePost}
            aria-label="Tạo bài viết"
            className="relative -top-4 flex items-center justify-center w-14 h-14 rounded-full bg-slate-900 dark:bg-white shadow-[0_4px_20px_rgba(0,0,0,0.3)] dark:shadow-[0_4px_20px_rgba(255,255,255,0.15)] hover:scale-105 active:scale-95 transition-transform duration-150"
          >
            <Plus className="w-6 h-6 text-white dark:text-slate-900 stroke-[2.5px]" />
          </button>

          {/* Notifications */}
          <div className="relative">
            <NavBtn
              icon={Bell}
              label="Thông báo"
              active={active === "notifications"}
              onClick={() => handleNav(PATHS.NOTIFICATIONS, "Thông báo")}
            />
            {unreadCount > 0 && (
              <span className="absolute top-0.5 right-0.5 min-w-[16px] h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center px-1 leading-none pointer-events-none">
                {unreadCount > 99 ? "99+" : unreadCount}
              </span>
            )}
          </div>

          {/* Profile or Login */}
          {accessToken ? (
            <button
              onClick={handleProfileTap}
              aria-label="Hồ sơ"
              className={`flex flex-col items-center justify-center gap-0.5 px-3 py-1 rounded-xl transition-all duration-200 min-w-[44px] ${
                active === "profile"
                  ? "text-slate-900 dark:text-white"
                  : "text-slate-400 dark:text-neutral-500"
              }`}
            >
              {currentUser?.avatarUrl ? (
                <div className={`w-6 h-6 rounded-full overflow-hidden border-2 transition-all ${active === "profile" ? "border-slate-900 dark:border-white" : "border-transparent"}`}>
                  <SafeAvatar src={currentUser.avatarUrl} alt="me" />
                </div>
              ) : (
                <User className={`w-5 h-5 transition-all ${active === "profile" ? "stroke-[2.5px]" : "stroke-2"}`} />
              )}
              <span className="text-[9px] font-semibold tracking-tight leading-none">Hồ sơ</span>
            </button>
          ) : (
            <NavBtn
              icon={User}
              label="Đăng nhập"
              active={false}
              onClick={() => navigate(PATHS.LOGIN)}
            />
          )}
        </div>
      </nav>

      {/* Profile Quick Actions Sheet */}
      {showProfileSheet && (
        <>
          {/* Backdrop */}
          <div
            className="lg:hidden fixed inset-0 z-[60] bg-black/40 backdrop-blur-[2px]"
            onClick={() => setShowProfileSheet(false)}
          />
          {/* Sheet */}
          <div className="lg:hidden fixed bottom-0 left-0 right-0 z-[70] bg-white dark:bg-[#1e1e1e] rounded-t-2xl border-t border-slate-200/80 dark:border-[#2a2a2a] shadow-[0_-8px_40px_rgba(0,0,0,0.15)] dark:shadow-[0_-8px_40px_rgba(0,0,0,0.5)] animate-in slide-in-from-bottom-4 duration-200"
            style={{ paddingBottom: "calc(env(safe-area-inset-bottom, 0px) + 1rem)" }}
          >
            {/* Handle */}
            <div className="flex justify-center pt-3 pb-2">
              <div className="w-10 h-1 bg-slate-200 dark:bg-neutral-700 rounded-full" />
            </div>

            {/* User info */}
            {currentUser && (
              <div className="flex items-center gap-3 px-5 py-3 border-b border-slate-100 dark:border-neutral-800">
                <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 dark:border-neutral-700">
                  <SafeAvatar src={currentUser.avatarUrl} alt={currentUser.username} />
                </div>
                <div className="min-w-0">
                  <p className="font-bold text-slate-900 dark:text-white text-sm truncate">
                    {currentUser.displayName || currentUser.username}
                  </p>
                  <p className="text-xs text-slate-500 dark:text-neutral-400 truncate">@{currentUser.username}</p>
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-col py-2">
              <button
                onClick={() => { setShowProfileSheet(false); navigate(PATHS.PROFILE); }}
                className="flex items-center gap-3 px-5 py-3.5 text-slate-700 dark:text-neutral-200 hover:bg-slate-50 dark:hover:bg-neutral-800/60 transition-colors"
              >
                <User className="w-5 h-5 text-slate-500 dark:text-neutral-400" />
                <span className="font-medium text-sm">Xem hồ sơ</span>
              </button>
              <button
                onClick={() => { setShowProfileSheet(false); navigate(PATHS.SETTINGS); }}
                className="flex items-center gap-3 px-5 py-3.5 text-slate-700 dark:text-neutral-200 hover:bg-slate-50 dark:hover:bg-neutral-800/60 transition-colors"
              >
                <Settings className="w-5 h-5 text-slate-500 dark:text-neutral-400" />
                <span className="font-medium text-sm">Cài đặt</span>
              </button>
              <button
                onClick={handleLogout}
                className="flex items-center gap-3 px-5 py-3.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
              >
                <LogOut className="w-5 h-5" />
                <span className="font-medium text-sm">Đăng xuất</span>
              </button>
            </div>
          </div>
        </>
      )}

      {/* Create Post Modal */}
      <CreatePostModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        currentUsername={currentUser?.displayName || currentUser?.username}
        onPostCreated={() => setShowCreateModal(false)}
      />
    </>
  );
}

function NavBtn({
  icon: Icon,
  label,
  active,
  onClick,
}: {
  icon: React.FC<{ className?: string }>;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      aria-label={label}
      className={`flex flex-col items-center justify-center gap-0.5 px-3 py-1 rounded-xl transition-all duration-200 min-w-[44px] ${
        active
          ? "text-slate-900 dark:text-white"
          : "text-slate-400 dark:text-neutral-500 hover:text-slate-700 dark:hover:text-neutral-300"
      }`}
    >
      <Icon
        className={`w-5 h-5 transition-all duration-200 ${
          active ? "stroke-[2.5px]" : "stroke-2"
        }`}
      />
      <span className="text-[9px] font-semibold tracking-tight leading-none">{label}</span>
    </button>
  );
}
