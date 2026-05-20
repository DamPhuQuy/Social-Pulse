import { Bookmark, Compass, Home, LogOut, Settings, User, Bell, Flag, Brain, Shield } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { logoutUser } from "@/services/auth/authService";
import { getUnreadNotificationCount } from "@/services/social/notificationService";
import { isAdminToken } from "@/lib/jwtUtils";

type SidebarKey = "home" | "discovery" | "notifications" | "bookmarks" | "profile" | "settings";

interface AppSidebarProps {
  active: SidebarKey;
}

export default function AppSidebar({ active }: AppSidebarProps) {
  const navigate = useNavigate();
  const { logout, accessToken, setAccessToken } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);

  const isAdmin = isAdminToken(accessToken);

  useEffect(() => {
    getUnreadNotificationCount().then(res => {
      if (res.ok && res.data) {
        setUnreadCount(res.data.unreadCount);
      }
    });

    const handleRealtimeNotification = () => {
      setUnreadCount(prev => prev + 1);
    };

    window.addEventListener("realtime:notification", handleRealtimeNotification);
    return () => {
      window.removeEventListener("realtime:notification", handleRealtimeNotification);
    };
  }, []);

  const handleLogout = async () => {
    await logoutUser();
    setAccessToken(null);
    logout();
    navigate(PATHS.LOGIN);
  };

  return (
    <aside className="hidden lg:flex flex-col gap-6 sticky top-24 h-[calc(100vh-120px)] overflow-y-auto pr-2">
      <nav className="flex flex-col gap-1">
        <NavItem icon={Home} label="Trang chủ" active={active === "home"} onClick={() => navigate(PATHS.HOME)} />
        <NavItem icon={Compass} label="Khám phá" active={active === "discovery"} onClick={() => navigate(PATHS.DISCOVERY)} />
        <NavItem icon={Bell} label={unreadCount > 0 ? `Thông báo (${unreadCount})` : "Thông báo"} active={active === "notifications"} onClick={() => navigate(PATHS.NOTIFICATIONS)} />
        <NavItem icon={Bookmark} label="Đã lưu" active={active === "bookmarks"} onClick={() => navigate(PATHS.BOOKMARKS)} />
        <NavItem icon={User} label="Hồ sơ" active={active === "profile"} onClick={() => navigate(PATHS.PROFILE)} />
        <NavItem icon={Settings} label="Cài đặt" active={active === "settings"} onClick={() => navigate(PATHS.SETTINGS)} />
      </nav>

      {/* ADMIN CONTROL PANEL SECTION — only rendered for ADMIN role */}
      {isAdmin && (
        <div className="flex flex-col gap-2 pt-4 border-t border-slate-200/80 dark:border-neutral-800">
          <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 dark:text-neutral-500 px-3">Quản trị viên</span>
          <nav className="flex flex-col gap-1 mt-1">
            <NavItem icon={Flag} label="Báo cáo" active={active === "settings" && window.location.pathname === PATHS.ADMIN_REPORTS} onClick={() => navigate(PATHS.ADMIN_REPORTS)} />
            <NavItem icon={Brain} label="Giám sát AI" active={active === "settings" && window.location.pathname === PATHS.ADMIN_AI} onClick={() => navigate(PATHS.ADMIN_AI)} />
            <NavItem icon={Shield} label="Phân quyền RBAC" active={active === "settings" && window.location.pathname === PATHS.ADMIN_RBAC} onClick={() => navigate(PATHS.ADMIN_RBAC)} />
          </nav>
        </div>
      )}

      <div className="mt-auto pt-4 pb-2 border-t border-slate-200/80 dark:border-neutral-800">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 px-3 py-2.5 text-sm font-medium text-slate-500 dark:text-neutral-400 hover:text-slate-900 dark:hover:text-white transition-colors w-full rounded-xl hover:bg-slate-100 dark:hover:bg-neutral-900"
        >
          <LogOut className="w-5 h-5" /> Đăng xuất
        </button>
      </div>
    </aside>
  );
}

function NavItem({
  icon: Icon,
  label,
  active = false,
  onClick,
}: {
  icon: React.FC<{ className?: string }>;
  label: string;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-4 px-3 py-3 rounded-xl transition-all text-left w-full ${
        active
          ? "bg-slate-100 dark:bg-neutral-800/80 text-slate-900 dark:text-white font-bold border-l-4 border-slate-900 dark:border-white pl-2"
          : "text-slate-600 dark:text-neutral-400 hover:bg-slate-100/50 dark:hover:bg-neutral-800/40 hover:text-slate-900 dark:hover:text-white font-medium pl-3"
      }`}
    >
      <Icon
        className={`w-5 h-5 ${
          active
            ? "stroke-[2.5px] text-slate-900 dark:text-white fill-slate-900 dark:fill-white"
            : "stroke-2 text-slate-500 dark:text-neutral-400 fill-none"
        }`}
      />
      <span className="text-[15px]">{label}</span>
    </button>
  );
}
