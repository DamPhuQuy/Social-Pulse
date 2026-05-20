import { apiClient } from "@/lib/axiosClient";

export type MessageStatus = "SENT" | "DELIVERED" | "READ";

export interface ConversationListResponse {
  id: number;
  otherParticipantId: number;
  otherParticipantUsername: string;
  lastMessagePreview: string | null;
  unreadCount: number;
  lastMessageAt: string | null;
}

export interface ConversationResponse {
  id: number;
  participant1Id: number;
  participant2Id: number;
  createdAt: string;
  lastMessageAt: string | null;
}

export interface ChatMessageResponse {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  timestamp: string;
  status: MessageStatus;
}

export interface MessageHistoryResponse {
  messages: ChatMessageResponse[];
  nextCursor: string | null;
  hasMore: boolean;
}

export async function getConversations(page = 0, size = 20): Promise<{ ok: boolean; data?: ConversationListResponse[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: ConversationListResponse[] }>(`/chat/conversations?page=${page}&size=${size}`);
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể tải danh sách cuộc trò chuyện." };
  }
}

export async function createConversation(participantId: number): Promise<{ ok: boolean; data?: ConversationResponse; message?: string }> {
  try {
    const res = await apiClient.post<{ code: number; message: string; data: ConversationResponse }>(`/chat/conversations`, {
      participantId,
    });
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể tạo cuộc trò chuyện." };
  }
}

export async function getConversationMessages(
  conversationId: number,
  cursor?: string,
  size = 20,
): Promise<{ ok: boolean; data?: MessageHistoryResponse; message?: string }> {
  try {
    const params = new URLSearchParams({ size: String(size) });
    if (cursor) {
      params.set("cursor", cursor);
    }
    const res = await apiClient.get<{ code: number; message: string; data: MessageHistoryResponse }>(
      `/chat/conversations/${conversationId}/messages?${params.toString()}`,
    );
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể tải lịch sử tin nhắn." };
  }
}
