import { apiClient } from "@/lib/axiosClient";

// ─── Types ─────────────────────────────────────────────────────────────────────

export type Privacy = "PUBLIC" | "FRIENDS_ONLY" | "PRIVATE";

export interface PostTopic {
  slug: string;
  label: string;
  category: string;
}

export interface PostCreationRequest {
  content: string;
  imageUrl?: string | null;
  imagePublicId?: string | null;
  topicSlugs: string[];
  privacy: Privacy;
  parentPostId?: number | null;
}

export interface PostUpdateRequest {
  content: string;
  imageUrl?: string | null;
  imagePublicId?: string | null;
  topicSlugs: string[];
  privacy: Privacy;
}

export interface PostCreationResponse {
  id: number;
  content: string;
  imageUrl: string | null;
  imagePublicId: string | null;
  topicSlugs: string[];
  parentPostId: number | null;
  type: string;
  userId: number;
  privacy: Privacy;
  createdAt: string;
}

export interface PostUpdateResponse {
  id: number;
  content: string;
  imageUrl: string | null;
  imagePublicId: string | null;
  topicSlugs: string[];
  type: string;
  userId: number;
  privacy: Privacy;
  createdAt: string;
  updatedAt: string;
}

export interface OriginalPostData {
  postId: number;
  content: string | null;
  imageUrl: string | null;
  topicSlugs: string[];
  userId: number;
  username: string | null;
  userAvatar: string | null;
  createdAt: string;
}

export interface FeedItem {
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
  aiScore: number | null;
  source: string | null;
  rankedAt: string | null;
  privacy: Privacy;
  createdAt: string;
  updatedAt: string | null;
  originalPost: OriginalPostData | null;
}

export type PulseReaction = "UPVOTE";

export interface PostReactionRequest {
  postId: number;
  reactionType: PulseReaction;
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

export async function updatePost(
  postId: number,
  payload: PostUpdateRequest
): Promise<{ ok: boolean; data?: PostUpdateResponse; message?: string }> {
  try {
    const res = await apiClient.put<{ code: number; message: string; data: PostUpdateResponse }>(
      `/posts/${postId}`,
      payload
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to update post.",
    };
  }
}

export async function deletePost(postId: number): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.delete(`/posts/${postId}`);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to delete post.",
    };
  }
}

export async function getPostTopics(): Promise<{ ok: boolean; data?: PostTopic[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: PostTopic[] }>("/posts/topics");
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch post topics.",
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
 * Pass topicSlug to filter by a specific topic (e.g. "gaming", "am-thuc").
 */
export async function getFeed(
  page = 0,
  size = 20,
  topicSlug?: string
): Promise<{ ok: boolean; data?: FeedItem[]; message?: string }> {
  try {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (topicSlug) params.set("topicSlug", topicSlug);
    const res = await apiClient.get<{ code: number; message: string; data: FeedItem[] }>(
      `/feed?${params.toString()}`
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
