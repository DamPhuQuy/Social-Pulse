import { apiClient } from "@/lib/axiosClient";
import type { PageResponse, UserPost } from "@/services/user/userService";

export interface BookmarkResponse {
  id: number;
  userId: number;
  postId: number;
  createdAt: string;
}

export async function createBookmark(postId: number): Promise<{ ok: boolean; data?: BookmarkResponse; message?: string }> {
  try {
    const res = await apiClient.post<{ code: number; message: string; data: BookmarkResponse }>(`/bookmarks/${postId}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to bookmark post." };
  }
}

export async function deleteBookmark(postId: number): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.delete(`/bookmarks/${postId}`);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to remove bookmark." };
  }
}

export async function getBookmarks(page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserPost>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserPost> }>(
      `/bookmarks?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch bookmarks." };
  }
}
