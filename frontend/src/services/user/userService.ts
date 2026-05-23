import { apiClient } from "@/lib/axiosClient";
import type { OriginalPostData } from "@/services/post/postService";

export interface UserProfile {
  userId: number;
  username: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  coverImageUrl: string | null;
  dob: string | null;
  gender: string | null;
  postCount: number;
  followers: number;
  following: number;
  isFollowing: boolean;
  avatarPublicId: string | null;
  coverImagePublicId: string | null;
}

export interface UserPost {
  postId: number;
  parentPostId: number | null;
  type: string;
  content: string;
  imageUrl: string | null;
  topicSlugs: string[];
  userId: number;
  username: string;
  userAvatar: string | null;
  upvoteCount: number;
  downvoteCount: number;
  cmtCount: number;
  shareCount: number;
  myReaction: string | null;
  myVote: number | null;
  privacy: "PUBLIC" | "FRIENDS_ONLY" | "PRIVATE";
  createdAt: string;
  updatedAt: string | null;
  originalPost: OriginalPostData | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export async function getMyProfile(): Promise<{ ok: boolean; data?: UserProfile; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: UserProfile }>("/users/profile");
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch profile.",
    };
  }
}

export async function getUserProfile(username: string): Promise<{ ok: boolean; data?: UserProfile; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: UserProfile }>(`/users/profile/${username}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch profile.",
    };
  }
}

export async function getUserProfileById(userId: number): Promise<{ ok: boolean; data?: UserProfile; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: UserProfile }>(`/users/profile/id/${userId}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch profile.",
    };
  }
}

export async function getUserPosts(userId: number, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<UserPost>; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<UserPost> }>(
      `/posts/users/${userId}?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch user posts.",
    };
  }
}

export interface UpdateProfileRequest {
  displayName?: string;
  bio?: string;
  avatarUrl?: string;
  avatarPublicId?: string;
  coverImageUrl?: string;
  coverImagePublicId?: string;
  gender?: string;
  dob?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export async function updateProfile(request: UpdateProfileRequest): Promise<{ ok: boolean; data?: UserProfile; message?: string }> {
  try {
    const res = await apiClient.put<{ code: number; message: string; data: UserProfile }>("/users/profile", request);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to update profile.",
    };
  }
}

export async function deleteProfile(): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.delete("/users/profile");
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to delete profile.",
    };
  }
}

export async function changePassword(request: ChangePasswordRequest): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.put("/users/me/password", request);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to change password.",
    };
  }
}
