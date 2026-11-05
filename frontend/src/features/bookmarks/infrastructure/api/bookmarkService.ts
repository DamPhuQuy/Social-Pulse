import { apiClient } from "@/core/http/axiosClient";
import type { FeedItem } from "@/features/feed/infrastructure/api/postService";

export async function toggleBookmark(
  postId: number
): Promise<{ ok: boolean; bookmarked?: boolean; message?: string }> {
  try {
    const res = await apiClient.post<{
      code: number;
      message: string;
      data: { postId: number; bookmarked: boolean };
    }>(`/posts/${postId}/bookmark`);
    return { ok: true, bookmarked: res.data.data.bookmarked };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Không thể lưu bài viết.",
    };
  }
}

export async function getMyBookmarks(
  page: number = 0,
  size: number = 20
): Promise<{ ok: boolean; data?: FeedItem[]; message?: string }> {
  try {
    const res = await apiClient.get<{
      code: number;
      message: string;
      data: FeedItem[];
    }>(`/bookmarks?page=${page}&size=${size}`);
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Không thể tải danh sách đã lưu.",
    };
  }
}
