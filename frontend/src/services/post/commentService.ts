import { apiClient } from "@/lib/axiosClient";

// ─── TYPES ─────────────────────────────────────────────────────────────────────

export interface UserSummary {
  id: number;
  username: string;
  avatarUrl: string | null;
}

export interface CommentResponse {
  id: number;
  user: UserSummary;
  content: string;
  createdAt: string;
  edited: boolean;
  upvoteCount: number;
  downvoteCount: number;
  replyCount: number;
  myReaction?: "UPVOTE" | "DOWNVOTE" | null; // Tracked locally at the UI level
}

export interface CommentCreationRequest {
  content: string;
  parentCommentId?: number | null;
}

export interface CommentCreationResponse {
  id: number;
  postId: number;
  userId: number;
  content: string;
  parentCommentId: number | null;
  createdAt: string;
}

export interface CommentReactionResponse {
  id: number;
  commentId: number;
  userId: number;
  reactionType: "UPVOTE" | "DOWNVOTE";
}

export interface CommentUpdateResponse {
  id: number;
  postId: number;
  userId: number;
  content: string;
  parentCommentId: number | null;
  createdAt: string;
}

// ─── API FUNCTIONS ──────────────────────────────────────────────────────────────

/**
 * Fetch top-level comments for a post (offset-based using lastId for infinite scroll).
 */
export async function getTopLevelComments(
  postId: number,
  lastId = 0,
  limit = 10
): Promise<{ ok: boolean; data?: CommentResponse[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: CommentResponse[] }>(
      `/posts/${postId}/comments?lastId=${lastId}&limit=${limit}`
    );
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch comments.",
    };
  }
}

/**
 * Fetch replies for a specific parent comment.
 */
export async function getCommentReplies(
  postId: number,
  commentId: number,
  lastId = 0,
  limit = 10
): Promise<{ ok: boolean; data?: CommentResponse[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: CommentResponse[] }>(
      `/posts/${postId}/comments/${commentId}/replies?lastId=${lastId}&limit=${limit}`
    );
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to fetch replies.",
    };
  }
}

/**
 * Create a new comment or nested reply.
 */
export async function createComment(
  postId: number,
  content: string,
  parentCommentId: number | null = null
): Promise<{ ok: boolean; data?: CommentCreationResponse; message?: string }> {
  try {
    const payload: CommentCreationRequest = { content };
    if (parentCommentId !== null) {
      payload.parentCommentId = parentCommentId;
    }

    const res = await apiClient.post<{ code: number; message: string; data: CommentCreationResponse }>(
      `/posts/${postId}/comments`,
      payload
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to post comment.",
    };
  }
}

/**
 * React to a comment (Upvote/Downvote).
 */
export async function reactComment(
  postId: number,
  commentId: number,
  reactionType: "UPVOTE" | "DOWNVOTE"
): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.post<{ code: number; message: string; data: CommentReactionResponse }>(
      `/posts/${postId}/comments/${commentId}/react`,
      { reactionType }
    );
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to react to comment.",
    };
  }
}

/**
 * Delete a comment (soft-delete).
 */
export async function deleteComment(
  postId: number,
  commentId: number
): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.delete(`/posts/${postId}/comments/${commentId}`);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to delete comment.",
    };
  }
}

export async function updateComment(
  postId: number,
  commentId: number,
  content: string
): Promise<{ ok: boolean; data?: CommentUpdateResponse; message?: string }> {
  try {
    const res = await apiClient.put<{ code: number; message: string; data: CommentUpdateResponse }>(
      `/posts/${postId}/comments/${commentId}`,
      { content }
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return {
      ok: false,
      message: axiosErr?.response?.data?.message ?? "Failed to update comment.",
    };
  }
}
