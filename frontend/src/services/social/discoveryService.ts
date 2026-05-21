import { apiClient } from "@/lib/axiosClient";
import type { PageResponse, UserPost } from "@/services/user/userService";

export interface SearchUserResponse {
  id: number;
  username: string;
  displayName: string;
  avatarUrl: string | null;
}

export interface TrendingHashtagResponse {
  hashtag: string;
  count: number;
}

export async function searchUsers(query: string, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<SearchUserResponse>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<SearchUserResponse> }>(
      `/discovery/users?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to search users." };
  }
}

export async function searchPosts(query: string, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserPost>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserPost> }>(
      `/discovery/posts?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to search posts." };
  }
}

export async function getTrendingHashtags(days = 7, limit = 10): Promise<{ ok: boolean; data?: TrendingHashtagResponse[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: TrendingHashtagResponse[] }>(
      `/discovery/hashtags/trending?days=${days}&limit=${limit}`
    );
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch trending hashtags." };
  }
}

export async function getPostsByHashtag(hashtag: string, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserPost>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserPost> }>(
      `/discovery/hashtags/${encodeURIComponent(hashtag)}/posts?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch hashtag posts." };
  }
}

export async function getPostsByMention(username: string, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserPost>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserPost> }>(
      `/discovery/mentions/${encodeURIComponent(username)}/posts?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch mention posts." };
  }
}
