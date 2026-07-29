import { apiClient } from "@/core/http/axiosClient";
import type { PageResponse } from "@/features/profiles/infrastructure/api/userService";

export interface UserSummary {
  id: number;
  username: string;
  avatarUrl: string | null;
}

export interface FollowResponse {
  id: number;
  followerId: number;
  followingId: number;
  createdAt: string;
}

export interface FollowStatusResponse {
  targetUserId: number;
  following: boolean;
}

export interface FollowCountsResponse {
  userId: number;
  followersCount: number;
  followingCount: number;
}

export async function followUser(userId: number): Promise<{ ok: boolean; data?: FollowResponse; message?: string }> {
  try {
    const res = await apiClient.post<{ code: number; message: string; data: FollowResponse }>(`/follows/${userId}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to follow user." };
  }
}

export async function unfollowUser(userId: number): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.delete(`/follows/${userId}`);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to unfollow user." };
  }
}

export async function getFollowers(userId: number, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserSummary>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserSummary> }>(
      `/follows/${userId}/followers?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch followers." };
  }
}

export async function getFollowing(userId: number, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserSummary>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserSummary> }>(
      `/follows/${userId}/following?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch following users." };
  }
}

export async function getFollowStatus(userId: number): Promise<{ ok: boolean; data?: FollowStatusResponse; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: FollowStatusResponse }>(`/follows/${userId}/status`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch follow status." };
  }
}

export async function getFollowCounts(userId: number): Promise<{ ok: boolean; data?: FollowCountsResponse; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: FollowCountsResponse }>(`/follows/${userId}/counts`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch follow counts." };
  }
}
