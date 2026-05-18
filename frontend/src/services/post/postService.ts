import { apiClient } from "@/lib/axiosClient";

// ─── Types ─────────────────────────────────────────────────────────────────────

export type Privacy = "PUBLIC" | "FRIENDS_ONLY" | "PRIVATE";

export interface PostCreationRequest {
  content: string;
  imageUrl?: string | null;
  imagePublicId?: string | null;
  privacy: Privacy;
  parentPostId?: number | null;
}

export interface PostCreationResponse {
  id: number;
  content: string;
  imageUrl: string | null;
  imagePublicId: string | null;
  parentPostId: number | null;
  type: string;
  userId: number;
  privacy: Privacy;
  createdAt: string;
}

export interface FeedItem {
  postId: number;
  parentPostId: number | null;
  type: string;
  content: string;
  imageUrl: string | null;
  userId: number;
  username: string;
  userAvatar: string | null;
  upvoteCount: number;
  downvoteCount: number;
  cmtCount: number;
  shareCount: number;
  myReaction: string | null;
  myVote: number | null;
  aiScore: number | null;
  source: string | null;
  rankedAt: string | null;
  createdAt: string;
}

export interface PostReactionRequest {
  postId: number;
  reactionType: "UPVOTE" | "DOWNVOTE";
}

// ─── API Functions ──────────────────────────────────────────────────────────────

/**
 * Create a new post. Requires Bearer token (injected by Axios interceptor).
 */
export async function createPost(
  payload: PostCreationRequest
): Promise<{ ok: boolean; data?: PostCreationResponse; message?: string }> {
  try {
    const res = await apiClient.post<{ code: number; message: string; data: PostCreationResponse }>(
      "/posts",
      payload
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to create post.",
    };
  }
}

export async function uploadMedia(file: File): Promise<{ ok: boolean; data?: string; message?: string }> {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const res = await apiClient.post<{ code: number; message: string; data: string }>(
      "/media/upload",
      formData,
      { headers: { "Content-Type": "multipart/form-data" } }
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to upload media.",
    };
  }
}

/**
 * Fetch personalized feed for the current authenticated user.
 */
export async function getFeed(
  page = 0,
  size = 20
): Promise<{ ok: boolean; data?: FeedItem[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: FeedItem[] }>(
      `/feed?page=${page}&size=${size}`
    );
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch feed.",
    };
  }
}

/**
 * React (upvote/downvote) to a post.
 */
export async function reactPost(
  payload: PostReactionRequest
): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.post("/posts/react", payload);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to react to post.",
    };
  }
}
