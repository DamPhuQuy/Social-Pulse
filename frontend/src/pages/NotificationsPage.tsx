import { useEffect, useState } from "react";
import { Bell, CheckCheck, Loader2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import BottomNavBar from "@/components/social/BottomNavBar";
import { PATHS } from "@/constants/paths";
import { SafeAvatar } from "@/components/ui/SafeAvatar";
import {
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationResponse,
} from "@/services/social/notificationService";

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    loadNotifications();

    const handleRealtimeNotification = (e: Event) => {
      const customEvent = e as CustomEvent;
      const notification = customEvent.detail;
      if (!notification) return;
      
      // Transform incoming notification to match NotificationResponse format
      const formattedNotification: NotificationResponse = {
        id: notification.id,
        recipientId: notification.recipientId,
        type: notification.type,
        resourceType: notification.resourceType,
        resourceId: notification.resourceId,
        message: notification.message,
        read: !!notification.readAt,
        createdAt: notification.createdAt || new Date().toISOString(),
        readAt: notification.readAt || null,
        actor: notification.actorId ? {
          id: notification.actorId,
          username: "Someone",
          avatarUrl: null
        } : null
      };

      setNotifications((prev) => [formattedNotification, ...prev]);
    };

    window.addEventListener("realtime:notification", handleRealtimeNotification);
    return () => {
      window.removeEventListener("realtime:notification", handleRealtimeNotification);
    };
  }, []);

  const loadNotifications = async () => {
    setLoading(true);
    const res = await getNotifications(0, 50);
    setLoading(false);
    if (res.ok && res.data) {
      setNotifications(res.data.items ?? []);
    } else {
      toast.error(res.message ?? "Không thể tải thông báo.");
    }
  };

  const handleOpen = async (item: NotificationResponse) => {
    if (!item.read) {
      const res = await markNotificationRead(item.id);
      if (res.ok) {
        setNotifications((prev) => prev.map((notification) => notification.id === item.id ? { ...notification, read: true, readAt: new Date().toISOString() } : notification));
      }
    }

    if (item.resourceType === "POST") {
      navigate(`/posts/${item.resourceId}`);
      return;
    }
    if (item.resourceType === "USER" && item.actor?.username) {
      navigate(`/profile/${item.actor.username}`);
      return;
    }
    if (item.resourceType === "COMMENT") {
      navigate(PATHS.HOME);
    }
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    const res = await markAllNotificationsRead();
    setMarkingAll(false);
    if (!res.ok) {
      toast.error(res.message ?? "Không thể đánh dấu tất cả đã đọc.");
      return;
    }
    setNotifications((prev) => prev.map((item) => ({ ...item, read: true, readAt: item.readAt ?? new Date().toISOString() })));
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="notifications" />

        <div className="min-w-0 pb-24 lg:pb-10">
          <div className="mb-5 flex items-center justify-end">
          <button
            onClick={handleMarkAll}
            disabled={markingAll || notifications.length === 0}
            className="flex items-center gap-2 rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50 dark:bg-white dark:text-slate-900"
          >
            {markingAll ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCheck className="h-4 w-4" />}
            Đánh dấu đã đọc
          </button>
          </div>

          <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
          <div className="mb-4 flex items-center gap-2">
            <Bell className="h-5 w-5 text-blue-500" />
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Thông báo</h1>
          </div>

          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
            </div>
          ) : notifications.length === 0 ? (
            <div className="py-20 text-center text-slate-500 dark:text-neutral-500">Bạn chưa có thông báo nào.</div>
          ) : (
            <div className="flex flex-col gap-3">
              {notifications.map((item) => (
                <button
                  key={item.id}
                  onClick={() => handleOpen(item)}
                  className={`flex items-start gap-3 rounded-2xl border p-4 text-left transition ${item.read ? "border-slate-200/80 bg-white dark:border-neutral-800 dark:bg-[#1e1e1e]" : "border-blue-200 bg-blue-50/70 dark:border-blue-500/20 dark:bg-blue-500/10"}`}
                >
                  <div className="h-11 w-11 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
                    <SafeAvatar src={item.actor?.avatarUrl} alt={item.actor?.username} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-slate-900 dark:text-white">{item.message}</p>
                    <p className="mt-1 text-xs text-slate-500 dark:text-neutral-400">{new Date(item.createdAt).toLocaleString("vi-VN")}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
          </section>
        </div>
      </div>
      <BottomNavBar active="notifications" />
    </div>
  );
}
