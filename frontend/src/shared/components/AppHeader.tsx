import { useEffect, useState } from "react";
import { Activity, Moon, Plus, Sun } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { PATHS } from "@/shared/constants/paths";
import { getMyProfile, type UserProfile } from "@/features/profiles/infrastructure/api/userService";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { useAuth } from "@/shared/hooks/useAuth";
import { toast } from "sonner";
import CreatePostModal from "@/features/feed/presentation/components/CreatePostModal";

export default function AppHeader() {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [isDark, setIsDark] = useState(() => {
    if (typeof window !== "undefined") {
      return localStorage.getItem("theme") === "dark" || 
        (!("theme" in localStorage) && window.matchMedia("(prefers-color-scheme: dark)").matches);
    }
    return false;
  });

  useEffect(() => {
    if (!accessToken) {
      setCurrentUser(null);
      return;
    }

    getMyProfile().then((res) => {
      if (res.ok && res.data) {
        setCurrentUser(res.data);
      }
    });
  }, [accessToken]);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  }, [isDark]);

  return (
    <>
      <header className="fixed top-0 left-0 w-full h-16 bg-white/95 dark:bg-[#1e1e1e]/95 backdrop-blur-xl border-b border-slate-200/80 dark:border-[#2a2a2a] z-40 flex items-center justify-between px-4 sm:px-6 shadow-sm dark:shadow-none">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3 cursor-pointer group" onClick={() => navigate(PATHS.HOME)}>
            <div className="w-9 h-9 sm:w-10 sm:h-10 bg-slate-900 dark:bg-white rounded-xl flex items-center justify-center shadow-md group-hover:scale-110 group-active:scale-95 transition-all duration-300">
              <Activity className="text-white dark:text-slate-900 w-4 h-4 sm:w-5 sm:h-5 stroke-[2.5px]" />
            </div>
            <span className="text-lg sm:text-xl font-extrabold tracking-tight text-slate-900 dark:text-white hidden sm:block">SocialPulse</span>
          </div>
        </div>
        <div className="flex items-center gap-2 sm:gap-4">
          <button onClick={() => setIsDark((value) => !value)} className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-850 text-slate-500 dark:text-slate-400 transition-all">
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
          <button
            onClick={() => {
              if (!accessToken) {
                toast.error("Vui lòng đăng nhập để đăng bài.");
                navigate(PATHS.LOGIN);
                return;
              }
              setShowModal(true);
            }}
            className="hidden lg:flex items-center gap-2 px-4 py-2 rounded-full bg-slate-900 text-white dark:bg-white dark:text-slate-900 text-sm font-bold hover:opacity-90 transition-opacity"
          >
            <Plus className="w-4 h-4" /> Đăng bài
          </button>
          <div
            onClick={() => {
              if (!accessToken) {
                toast.error("Vui lòng đăng nhập để xem hồ sơ.");
                navigate(PATHS.LOGIN);
                return;
              }
              navigate(PATHS.PROFILE);
            }}
            className="w-9 h-9 rounded-full overflow-hidden cursor-pointer border border-slate-200 dark:border-neutral-700 hover:opacity-80 transition-opacity"
          >
            <SafeAvatar src={currentUser?.avatarUrl} alt="me" />
          </div>
        </div>
      </header>

      <CreatePostModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        currentUsername={currentUser?.displayName || currentUser?.username}
      />
    </>
  );
}
