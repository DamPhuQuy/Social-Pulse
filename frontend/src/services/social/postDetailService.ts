import { apiClient } from "@/lib/axiosClient";
import type { Privacy } from "@/services/post/postService";

export interface ViewPostResponse {
  id: number;
  parentPostId: number | null;
  type: string;
  content: string;
  imageUrl: string | null;
  topicSlugs: string[];
  privacy: Privacy;
  userId: number;
  username: string;
  userAvatar: string | null;
  upvoteCount: number;
  downvoteCount: number;
  cmtCount: number;
  shareCount: number;
  myVote: number | null;
  createdAt: string;
  updatedAt: string | null;
  originalPost: import("@/services/post/postService").OriginalPostData | null;
}

export async function getPostDetail(postId: number): Promise<{ ok: boolean; data?: ViewPostResponse; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: ViewPostResponse }>(`/posts/${postId}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch post detail." };
  }
}
