import { apiClient } from "@/core/http/axiosClient";
import type { ApiResponse } from "@/core/types/response/AuthApiResponse";

export interface TopicItem {
  id?: number;
  slug: string;
  label: string;
  category: string;
}

export async function getFollowedTopics(): Promise<ApiResponse<string[]>> {
  const response = await apiClient.get<ApiResponse<string[]>>("/topics/followed");
  return response.data;
}

export async function followTopic(slug: string): Promise<ApiResponse<void>> {
  const response = await apiClient.post<ApiResponse<void>>(`/topics/${slug}/follow`);
  return response.data;
}

export async function unfollowTopic(slug: string): Promise<ApiResponse<void>> {
  const response = await apiClient.delete<ApiResponse<void>>(`/topics/${slug}/follow`);
  return response.data;
}
