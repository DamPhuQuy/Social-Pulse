import { apiClient } from "@/lib/axiosClient";
import type { PageResponse } from "@/services/user/userService";
import type { UserSummary } from "@/services/social/followService";

export type NotificationType =
  | "FOLLOWED_YOU"
  | "POST_REACTED"
  | "COMMENTED_ON_POST"
  | "REPLIED_TO_COMMENT"
  | "COMMENT_REACTED";

export type NotificationResourceType = "USER" | "POST" | "COMMENT";

export interface NotificationResponse {
  id: number;
  actor: UserSummary | null;
  recipientId: number;
  type: NotificationType;
  resourceType: NotificationResourceType;
  resourceId: number;
  message: string;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationUnreadCountResponse {
  unreadCount: number;
}

export async function getNotifications(page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<NotificationResponse>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<NotificationResponse> }>(
      `/notifications?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch notifications." };
  }
}

export async function getUnreadNotificationCount(): Promise<{ ok: boolean; data?: NotificationUnreadCountResponse; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: NotificationUnreadCountResponse }>("/notifications/unread-count");
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch unread count." };
  }
}

export async function markNotificationRead(notificationId: number): Promise<{ ok: boolean; data?: NotificationResponse; message?: string }> {
  try {
    const res = await apiClient.patch<{ code: number; message: string; data: NotificationResponse }>(`/notifications/${notificationId}/read`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to mark notification as read." };
  }
}

export async function markAllNotificationsRead(): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.patch("/notifications/read-all");
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to mark all notifications as read." };
  }
}
