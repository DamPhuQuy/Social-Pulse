import { Bell, Compass, Home, Plus, User } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { getUnreadNotificationCount } from "@/services/social/notificationService";
import { toast } from "sonner";
import CreatePostModal from "@/components/post/CreatePostModal";
import { getMyProfile, type UserProfile } from "@/services/user/userService";

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
  const { accessToken } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
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
            <NavBtn
              icon={User}
              label="Hồ sơ"
              active={active === "profile"}
              onClick={() => handleNav(PATHS.PROFILE, "Hồ sơ")}
            />
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
