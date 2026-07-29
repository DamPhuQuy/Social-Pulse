import AppHeader from "@/shared/components/AppHeader";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { getCurrentUser } from "@/features/authentication/infrastructure/api/authService";
import {
  createConversation,
  getConversationMessages,
  getConversations,
  type ChatMessageResponse,
  type ConversationListResponse,
  type MessageStatus,
} from "@/features/chat/infrastructure/api/chatService";
import {
  searchUsers,
  type SearchUserResponse,
} from "@/features/discovery/infrastructure/api/discoveryService";
import { getUserProfile, type UserProfile } from "@/features/profiles/infrastructure/api/userService";
import { Client, type StompSubscription } from "@stomp/stompjs";
import {
  ArrowLeft,
  CheckCheck,
  Clock3,
  Loader2,
  MessageSquare,
  Search,
  Send,
  Users,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import SockJS from "sockjs-client";
import { toast } from "sonner";

type ChatMessageItem = Omit<ChatMessageResponse, "id"> & {
  id: number | string;
  optimistic?: boolean;
};

type SocketStatus = "connecting" | "connected" | "disconnected";

const MESSAGE_PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;
const SEND_SYNC_DELAY_MS = 350;

function formatRelativeTime(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  const diffSeconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (diffSeconds < 60) return "Vừa xong";
  if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)} phút trước`;
  if (diffSeconds < 86400) return `${Math.floor(diffSeconds / 3600)} giờ trước`;
  return `${Math.floor(diffSeconds / 86400)} ngày trước`;
}

function formatClockTime(value: string): string {
  return new Date(value).toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getInitials(name: string): string {
  const cleaned = name.replace(/^@/, "").trim();
  if (!cleaned) return "?";
  const parts = cleaned.split(/\s+/).filter(Boolean);
  if (parts.length === 0) return cleaned.slice(0, 2).toUpperCase();
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

function buildChatSocketUrl(): string {
  const apiBaseUrl = (
    import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1"
  ).trim();
  const url = new URL(apiBaseUrl, window.location.origin);
  url.pathname = "/ws";
  url.search = "";
  url.hash = "";
  return url.toString().replace(/\/$/, "");
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isChatMessagePayload(value: unknown): value is ChatMessageResponse {
  return (
    isObject(value) &&
    typeof value.id === "number" &&
    typeof value.conversationId === "number" &&
    typeof value.senderId === "number" &&
    typeof value.content === "string" &&
    typeof value.timestamp === "string" &&
    typeof value.status === "string"
  );
}

function isMessageStatusUpdatePayload(value: unknown): value is {
  messageId: number;
  conversationId: number;
  previousStatus: MessageStatus;
  newStatus: MessageStatus;
  updatedAt: string;
} {
  return (
    isObject(value) &&
    typeof value.messageId === "number" &&
    typeof value.conversationId === "number" &&
    typeof value.previousStatus === "string" &&
    typeof value.newStatus === "string" &&
    typeof value.updatedAt === "string"
  );
}

function isReadAckPayload(value: unknown): value is {
  type: "READ_ACK";
  conversationId: number;
  readBy: number;
  timestamp: string;
} {
  return (
    isObject(value) &&
    value.type === "READ_ACK" &&
    typeof value.conversationId === "number" &&
    typeof value.readBy === "number" &&
    typeof value.timestamp === "string"
  );
}

function isUnreadCountsPayload(
  value: unknown,
): value is Array<{ conversationId: number; unreadCount: number }> {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        isObject(item) &&
        typeof item.conversationId === "number" &&
        typeof item.unreadCount === "number",
    )
  );
}

function normalizeServerMessage(message: ChatMessageResponse): ChatMessageItem {
  return { ...message, id: message.id };
}

function sortConversations(
  items: ConversationListResponse[],
): ConversationListResponse[] {
  return [...items].sort((a, b) => {
    const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
    const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
    return bTime - aTime || b.id - a.id;
  });
}

function mergeMessages(
  existing: ChatMessageItem[],
  incoming: ChatMessageItem[],
): ChatMessageItem[] {
  const next = [...existing];

  for (const message of incoming) {
    const exactIndex = next.findIndex(
      (item) => String(item.id) === String(message.id),
    );
    if (exactIndex >= 0) {
      next[exactIndex] = { ...next[exactIndex], ...message, optimistic: false };
      continue;
    }

    const optimisticIndex = next.findIndex((item) => {
      if (!item.optimistic) return false;
      if (item.senderId !== message.senderId) return false;
      if (item.content !== message.content) return false;
      const itemTime = new Date(item.timestamp).getTime();
      const messageTime = new Date(message.timestamp).getTime();
      return Math.abs(itemTime - messageTime) < 15000;
    });

    if (optimisticIndex >= 0) {
      next[optimisticIndex] = { ...message };
      continue;
    }

    next.push({ ...message });
  }

  const deduped = new Map<string, ChatMessageItem>();
  for (const message of next) {
    deduped.set(String(message.id), message);
  }

  return [...deduped.values()].sort((a, b) => {
    const aTime = new Date(a.timestamp).getTime();
    const bTime = new Date(b.timestamp).getTime();
    return aTime - bTime || String(a.id).localeCompare(String(b.id));
  });
}

function truncatePreview(text: string, max = 72): string {
  const compact = text.replace(/\s+/g, " ").trim();
  if (compact.length <= max) return compact;
  return `${compact.slice(0, max - 1)}…`;
}

export default function ChatPage() {
  const { accessToken } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [currentUserEmail, setCurrentUserEmail] = useState<string | null>(null);
  const [bootstrapping, setBootstrapping] = useState(true);
  const [socketStatus, setSocketStatus] = useState<SocketStatus>("connecting");
  const [mobileView, setMobileView] = useState<"list" | "chat">("list");
  const [conversations, setConversations] = useState<
    ConversationListResponse[]
  >([]);
  const [conversationsLoading, setConversationsLoading] = useState(true);
  const [selectedConversationId, setSelectedConversationId] = useState<
    number | null
  >(null);
  const [selectedContact, setSelectedContact] = useState<UserProfile | null>(
    null,
  );
  const [messages, setMessages] = useState<ChatMessageItem[]>([]);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [messageCursor, setMessageCursor] = useState<string | null>(null);
  const [hasMoreMessages, setHasMoreMessages] = useState(false);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [userSearchQuery, setUserSearchQuery] = useState("");
  const [userSearchResults, setUserSearchResults] = useState<
    SearchUserResponse[]
  >([]);
  const [userSearchLoading, setUserSearchLoading] = useState(false);

  const stompClientRef = useRef<Client | null>(null);
  const conversationSubscriptionRef = useRef<StompSubscription | null>(null);
  const unreadSubscriptionRef = useRef<StompSubscription | null>(null);
  const statusSubscriptionRef = useRef<StompSubscription | null>(null);
  const errorSubscriptionRef = useRef<StompSubscription | null>(null);
  const messageListRef = useRef<HTMLDivElement | null>(null);
  const autoOpenedParticipantRef = useRef<number | null>(null);
  const pendingReadAckRef = useRef<number | null>(null);
  const syncTimerRef = useRef<number | null>(null);
  const currentUserIdRef = useRef<number | null>(null);

  const selectedConversation = useMemo(
    () =>
      conversations.find(
        (conversation) => conversation.id === selectedConversationId,
      ) ?? null,
    [conversations, selectedConversationId],
  );
  const hasSelectedConversation = !!selectedConversation;

  useEffect(() => {
    currentUserIdRef.current = currentUserId;
  }, [currentUserId]);

  const loadConversations = async (preferredConversationId?: number) => {
    setConversationsLoading(true);
    const res = await getConversations(0, 100);
    setConversationsLoading(false);

    if (!res.ok || !res.data) {
      toast.error(res.message ?? "Không thể tải danh sách cuộc trò chuyện.");
      return;
    }

    const ordered = sortConversations(res.data);
    setConversations(ordered);

    if (preferredConversationId) {
      setSelectedConversationId(preferredConversationId);
      return;
    }

    if (selectedConversationId === null && ordered.length > 0) {
      setSelectedConversationId(ordered[0].id);
    }
  };

  const loadSelectedContactProfile = async (username: string) => {
    const res = await getUserProfile(username);
    if (res.ok && res.data) {
      setSelectedContact(res.data);
      return;
    }
    setSelectedContact(null);
  };

  const syncSelectedConversation = async (conversationId: number) => {
    const res = await getConversationMessages(
      conversationId,
      undefined,
      MESSAGE_PAGE_SIZE,
    );
    if (!res.ok || !res.data) {
      toast.error(res.message ?? "Không thể đồng bộ hội thoại.");
      return;
    }

    const normalized = [...res.data.messages]
      .reverse()
      .map(normalizeServerMessage);
    setMessages((prev) => mergeMessages(prev, normalized));
    setMessageCursor(res.data.nextCursor);
    setHasMoreMessages(res.data.hasMore);
    requestAnimationFrame(() => {
      if (messageListRef.current) {
        messageListRef.current.scrollTop = messageListRef.current.scrollHeight;
      }
    });
  };

  const loadConversationMessages = async (
    conversationId: number,
    cursor?: string,
    mode: "replace" | "older" = "replace",
  ) => {
    if (mode === "replace") {
      setMessagesLoading(true);
      setMessages([]);
      setMessageCursor(null);
      setHasMoreMessages(false);
    } else {
      setLoadingOlder(true);
    }

    const previousScrollHeight = messageListRef.current?.scrollHeight ?? 0;
    const previousScrollTop = messageListRef.current?.scrollTop ?? 0;

    const res = await getConversationMessages(
      conversationId,
      cursor,
      MESSAGE_PAGE_SIZE,
    );

    if (mode === "replace") {
      setMessagesLoading(false);
    } else {
      setLoadingOlder(false);
    }

    if (!res.ok || !res.data) {
      toast.error(res.message ?? "Không thể tải tin nhắn.");
      return;
    }

    const normalized = [...res.data.messages]
      .reverse()
      .map(normalizeServerMessage);
    setMessages((prev) =>
      mode === "replace" ? normalized : mergeMessages(prev, normalized),
    );
    setMessageCursor(res.data.nextCursor);
    setHasMoreMessages(res.data.hasMore);

    if (mode === "replace") {
      requestAnimationFrame(() => {
        if (messageListRef.current) {
          messageListRef.current.scrollTop =
            messageListRef.current.scrollHeight;
        }
      });
    } else {
      requestAnimationFrame(() => {
        if (messageListRef.current) {
          const nextScrollHeight = messageListRef.current.scrollHeight;
          messageListRef.current.scrollTop =
            nextScrollHeight - previousScrollHeight + previousScrollTop;
        }
      });
    }
  };

  const openConversationWithParticipant = async (participantId: number) => {
    if (!participantId || participantId === currentUserId) {
      return;
    }

    const res = await createConversation(participantId);
    if (!res.ok || !res.data) {
      toast.error(res.message ?? "Không thể mở cuộc trò chuyện.");
      return;
    }

    setSelectedConversationId(res.data.id);
    await loadConversations(res.data.id);
  };

  const sendReadAck = (conversationId: number) => {
    const client = stompClientRef.current;
    if (client?.connected) {
      client.publish({
        destination: "/app/chat.read",
        body: JSON.stringify({ conversationId }),
      });
      pendingReadAckRef.current = null;
      return;
    }

    pendingReadAckRef.current = conversationId;
  };

  const applyIncomingMessage = (payload: ChatMessageResponse) => {
    const nextMessage = normalizeServerMessage(payload);
    const myUserId = currentUserIdRef.current;

    setMessages((prev) => mergeMessages(prev, [nextMessage]));
    setConversations((prev) => {
      const updated = prev.map((conversation) => {
        if (conversation.id !== payload.conversationId) return conversation;
        const isSelected = conversation.id === selectedConversationId;
        const preview = truncatePreview(payload.content);
        const shouldIncrementUnread =
          payload.senderId !== myUserId && !isSelected;

        return {
          ...conversation,
          lastMessagePreview: preview,
          lastMessageAt: payload.timestamp,
          unreadCount:
            payload.senderId === myUserId
              ? 0
              : shouldIncrementUnread
                ? conversation.unreadCount + 1
                : 0,
        };
      });

      return sortConversations(updated);
    });

    if (
      payload.conversationId === selectedConversationId &&
      payload.senderId !== myUserId
    ) {
      sendReadAck(payload.conversationId);
      requestAnimationFrame(() => {
        if (messageListRef.current) {
          messageListRef.current.scrollTop =
            messageListRef.current.scrollHeight;
        }
      });
    }
  };

  const applyStatusUpdate = (payload: {
    messageId: number;
    conversationId: number;
    previousStatus: MessageStatus;
    newStatus: MessageStatus;
    updatedAt: string;
  }) => {
    setMessages((prev) =>
      prev.map((message) =>
        String(message.id) === String(payload.messageId)
          ? { ...message, status: payload.newStatus, optimistic: false }
          : message,
      ),
    );
  };

  const applyReadAck = (payload: {
    type: "READ_ACK";
    conversationId: number;
    readBy: number;
    timestamp: string;
  }) => {
    const myUserId = currentUserIdRef.current;
    if (payload.readBy === myUserId) {
      return;
    }

    setMessages((prev) =>
      prev.map((message) =>
        message.conversationId === payload.conversationId &&
        message.senderId === myUserId &&
        message.status !== "READ"
          ? { ...message, status: "READ" }
          : message,
      ),
    );
  };

  const scheduleSync = (conversationId: number) => {
    if (syncTimerRef.current) {
      window.clearTimeout(syncTimerRef.current);
    }

    syncTimerRef.current = window.setTimeout(() => {
      void syncSelectedConversation(conversationId);
    }, SEND_SYNC_DELAY_MS);
  };

  const handleSendMessage = async () => {
    if (!selectedConversation || !currentUserId) return;
    const content = draft.trim();
    if (!content || sending) return;

    setSending(true);
    setDraft("");
    const myUserId = currentUserIdRef.current;

    const optimisticMessage: ChatMessageItem = {
      id: `temp-${Date.now()}`,
      conversationId: selectedConversation.id,
      senderId: myUserId ?? currentUserId,
      content,
      timestamp: new Date().toISOString(),
      status: "SENT",
      optimistic: true,
    };

    setMessages((prev) => [...prev, optimisticMessage]);
    setConversations((prev) =>
      sortConversations(
        prev.map((conversation) =>
          conversation.id === selectedConversation.id
            ? {
                ...conversation,
                lastMessagePreview: truncatePreview(content),
                lastMessageAt: optimisticMessage.timestamp,
                unreadCount: 0,
              }
            : conversation,
        ),
      ),
    );

    const client = stompClientRef.current;
    if (!client?.connected) {
      toast.error("Kết nối chat chưa sẵn sàng.");
      setSending(false);
      setDraft(content);
      return;
    }

    try {
      client.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({
          conversationId: selectedConversation.id,
          content,
        }),
      });
      scheduleSync(selectedConversation.id);
      requestAnimationFrame(() => {
        if (messageListRef.current) {
          messageListRef.current.scrollTop =
            messageListRef.current.scrollHeight;
        }
      });
    } catch {
      toast.error("Không thể gửi tin nhắn.");
      setDraft(content);
    } finally {
      setSending(false);
    }
  };

  useEffect(() => {
    if (!accessToken) return;

    let cancelled = false;
    const client = new Client({
      webSocketFactory: () => new SockJS(buildChatSocketUrl()),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      onConnect: () => {
        if (cancelled) return;
        setSocketStatus("connected");

        unreadSubscriptionRef.current?.unsubscribe();
        statusSubscriptionRef.current?.unsubscribe();
        errorSubscriptionRef.current?.unsubscribe();

        unreadSubscriptionRef.current = client.subscribe(
          "/user/queue/unread-counts",
          (message) => {
            try {
              const payload = JSON.parse(message.body);
              if (!isUnreadCountsPayload(payload)) return;

              setConversations((prev) =>
                sortConversations(
                  prev.map((conversation) => {
                    const nextUnread = payload.find(
                      (entry) => entry.conversationId === conversation.id,
                    );
                    return nextUnread
                      ? { ...conversation, unreadCount: nextUnread.unreadCount }
                      : conversation;
                  }),
                ),
              );
            } catch {
              // ignore malformed payloads
            }
          },
        );

        statusSubscriptionRef.current = client.subscribe(
          "/user/queue/status-updates",
          (message) => {
            try {
              const payload = JSON.parse(message.body);
              if (!isMessageStatusUpdatePayload(payload)) return;
              applyStatusUpdate(payload);
            } catch {
              // ignore malformed payloads
            }
          },
        );

        errorSubscriptionRef.current = client.subscribe(
          "/user/queue/errors",
          (message) => {
            try {
              const payload = JSON.parse(message.body);
              if (isObject(payload) && typeof payload.message === "string") {
                toast.error(payload.message);
              }
            } catch {
              toast.error("Đã xảy ra lỗi trong cuộc trò chuyện.");
            }
          },
        );

        if (pendingReadAckRef.current !== null) {
          sendReadAck(pendingReadAckRef.current);
        }
      },
      onWebSocketClose: () => {
        if (!cancelled) setSocketStatus("disconnected");
      },
      onStompError: () => {
        if (!cancelled) setSocketStatus("disconnected");
      },
      onWebSocketError: () => {
        if (!cancelled) setSocketStatus("disconnected");
      },
    });

    stompClientRef.current = client;
    setSocketStatus("connecting");
    client.activate();

    return () => {
      cancelled = true;
      conversationSubscriptionRef.current?.unsubscribe();
      unreadSubscriptionRef.current?.unsubscribe();
      statusSubscriptionRef.current?.unsubscribe();
      errorSubscriptionRef.current?.unsubscribe();
      conversationSubscriptionRef.current = null;
      unreadSubscriptionRef.current = null;
      statusSubscriptionRef.current = null;
      errorSubscriptionRef.current = null;
      client.deactivate();
      stompClientRef.current = null;
      setSocketStatus("disconnected");
    };
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;

    let cancelled = false;
    const bootstrap = async () => {
      try {
        const [meRes, conversationsRes] = await Promise.all([
          getCurrentUser(accessToken),
          getConversations(0, 100),
        ]);

        if (cancelled) return;

        if (meRes.ok && meRes.data) {
          setCurrentUserId(meRes.data.id);
          setCurrentUserEmail(meRes.data.email);
        } else {
          toast.error(meRes.message ?? "Không thể tải thông tin tài khoản.");
        }

        if (conversationsRes.ok && conversationsRes.data) {
          const ordered = sortConversations(conversationsRes.data);
          setConversations(ordered);
          if (ordered.length > 0) {
            setSelectedConversationId((current) => current ?? ordered[0].id);
          }
        } else {
          toast.error(
            conversationsRes.message ??
              "Không thể tải danh sách cuộc trò chuyện.",
          );
        }
      } catch (err) {
        console.error("Error during bootstrap:", err);
      } finally {
        setConversationsLoading(false);
        setBootstrapping(false);
      }
    };

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  useEffect(() => {
    if (!currentUserId) return;

    const rawParticipantId = params.get("userId");
    if (!rawParticipantId) return;

    const participantId = Number(rawParticipantId);
    if (!Number.isFinite(participantId) || participantId <= 0) return;
    if (participantId === currentUserId) return;
    if (autoOpenedParticipantRef.current === participantId) return;

    autoOpenedParticipantRef.current = participantId;
    void openConversationWithParticipant(participantId);
    navigate(PATHS.CHAT, { replace: true });
  }, [currentUserId, params, navigate]);

  useEffect(() => {
    if (!selectedConversation) {
      setSelectedContact(null);
      return;
    }

    void loadConversationMessages(
      selectedConversation.id,
      undefined,
      "replace",
    );
    void loadSelectedContactProfile(
      selectedConversation.otherParticipantUsername,
    );
    setConversations((prev) =>
      prev.map((conversation) =>
        conversation.id === selectedConversation.id
          ? { ...conversation, unreadCount: 0 }
          : conversation,
      ),
    );
    sendReadAck(selectedConversation.id);
  }, [selectedConversationId, hasSelectedConversation]);

  useEffect(() => {
    if (!selectedConversationId || socketStatus !== "connected") {
      return;
    }

    conversationSubscriptionRef.current?.unsubscribe();
    conversationSubscriptionRef.current =
      stompClientRef.current?.subscribe(
        `/topic/chat.${selectedConversationId}`,
        (message) => {
          try {
            const payload = JSON.parse(message.body);
            if (isReadAckPayload(payload)) {
              applyReadAck(payload);
              return;
            }
            if (isMessageStatusUpdatePayload(payload)) {
              applyStatusUpdate(payload);
              return;
            }
            if (isChatMessagePayload(payload)) {
              applyIncomingMessage(payload);
            }
          } catch {
            // ignore malformed payloads
          }
        },
      ) ?? null;
  }, [selectedConversationId, socketStatus, currentUserId]);

  useEffect(() => {
    if (!selectedConversationId || socketStatus !== "connected") return;
    if (pendingReadAckRef.current === selectedConversationId) {
      sendReadAck(selectedConversationId);
    }
  }, [selectedConversationId, socketStatus]);

  useEffect(() => {
    if (!userSearchQuery.trim()) {
      setUserSearchResults([]);
      setUserSearchLoading(false);
      return;
    }

    let cancelled = false;
    setUserSearchLoading(true);
    const timer = window.setTimeout(async () => {
      const res = await searchUsers(userSearchQuery.trim(), 0, 8);
      if (cancelled) return;
      setUserSearchLoading(false);
      if (res.ok && res.data) {
        setUserSearchResults(
          (res.data.items ?? []).filter(
            (user) => user.id !== currentUserIdRef.current,
          ),
        );
      } else {
        setUserSearchResults([]);
      }
    }, SEARCH_DEBOUNCE_MS);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [userSearchQuery, currentUserId]);

  const handleConversationPick = (conversation: ConversationListResponse) => {
    setSelectedConversationId(conversation.id);
    setMobileView("chat");
  };

  const handleUserPick = async (user: SearchUserResponse) => {
    setUserSearchQuery("");
    setUserSearchResults([]);
    await openConversationWithParticipant(user.id);
    setMobileView("chat");
  };

  const loadOlderMessages = async () => {
    if (
      !selectedConversationId ||
      !hasMoreMessages ||
      !messageCursor ||
      loadingOlder
    ) {
      return;
    }
    await loadConversationMessages(
      selectedConversationId,
      messageCursor,
      "older",
    );
  };

  const selectedConversationUnread = selectedConversation?.unreadCount ?? 0;

  if (bootstrapping) {
    return (
      <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
        <AppHeader />
        <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
          <AppSidebar active="chat" />
          <div className="flex items-center justify-center py-24 text-slate-500 dark:text-neutral-400">
            <Loader2 className="w-6 h-6 animate-spin mr-2" />
            Đang khởi tạo hộp thư...
          </div>
        </div>
        <BottomNavBar active="chat" />
      </div>
    );
  }

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="chat" />

        <div className="min-w-0 pb-24 lg:pb-10">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
            <div className="flex items-center gap-3">
              <div className="rounded-xl bg-blue-500/10 p-3 text-blue-500">
                <MessageSquare className="w-6 h-6" />
              </div>
              <div>
                <h1 className="text-2xl font-bold tracking-tight">Tin nhắn</h1>
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  {currentUserEmail
                    ? `Đang đăng nhập bằng ${currentUserEmail}`
                    : "Hộp thư realtime cho cuộc trò chuyện 1-1."}
                </p>
              </div>
            </div>

            <div
              className={`inline-flex items-center gap-2 rounded-full px-4 py-2 text-xs font-bold uppercase tracking-[0.2em] ${
                socketStatus === "connected"
                  ? "bg-green-500/10 text-green-600"
                  : socketStatus === "connecting"
                    ? "bg-amber-500/10 text-amber-600"
                    : "bg-slate-500/10 text-slate-500"
              }`}
            >
              <span
                className={`h-2 w-2 rounded-full ${
                  socketStatus === "connected"
                    ? "bg-green-500"
                    : socketStatus === "connecting"
                      ? "bg-amber-500"
                      : "bg-slate-400"
                }`}
              />
              {socketStatus === "connected"
                ? "Realtime active"
                : socketStatus === "connecting"
                  ? "Đang kết nối"
                  : "Đã ngắt kết nối"}
            </div>
          </div>

          <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
            {/* Conversation List — hidden on mobile when chat is open */}
            <aside className={`space-y-4 ${
              mobileView === "chat" ? "hidden xl:block" : "block"
            }`}>
              <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
                <div className="mb-3 flex items-center gap-2">
                  <Users className="h-4 w-4 text-blue-500" />
                  <h2 className="font-bold">Tạo cuộc trò chuyện</h2>
                </div>
                <div className="relative">
                  <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    value={userSearchQuery}
                    onChange={(event) => setUserSearchQuery(event.target.value)}
                    placeholder="Tìm theo tên, username hoặc email..."
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-10 text-sm outline-none transition focus:border-slate-400 dark:border-neutral-800 dark:bg-neutral-950 dark:text-white"
                  />
                  {userSearchLoading && (
                    <Loader2 className="absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-slate-400" />
                  )}
                  {userSearchQuery && !userSearchLoading && (
                    <button
                      onClick={() => {
                        setUserSearchQuery("");
                        setUserSearchResults([]);
                      }}
                      className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-slate-400 hover:bg-slate-200/70 dark:hover:bg-neutral-800"
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>

                {userSearchQuery.trim().length >= 2 && (
                  <div className="mt-3 max-h-72 overflow-y-auto rounded-2xl border border-slate-200/80 bg-white dark:border-neutral-800 dark:bg-neutral-950">
                    {userSearchLoading ? (
                      <div className="p-4 text-center text-sm text-slate-500 dark:text-slate-400">
                        <Loader2 className="mx-auto mb-2 h-4 w-4 animate-spin" />
                        Đang tìm người dùng...
                      </div>
                    ) : userSearchResults.length === 0 ? (
                      <div className="p-4 text-center text-sm text-slate-500 dark:text-slate-400">
                        Không tìm thấy người dùng phù hợp.
                      </div>
                    ) : (
                      <div className="flex flex-col divide-y divide-slate-200/80 dark:divide-neutral-800">
                        {userSearchResults.map((user) => (
                          <button
                            key={user.id}
                            onClick={() => void handleUserPick(user)}
                            className="flex items-center gap-3 px-4 py-3 text-left hover:bg-slate-50 dark:hover:bg-neutral-900"
                          >
                            <div className="h-10 w-10 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
                              <SafeAvatar
                                src={user.avatarUrl}
                                alt={user.username}
                              />
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-semibold text-slate-900 dark:text-white">
                                {user.displayName || user.username}
                              </p>
                              <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                                @{user.username}
                              </p>
                            </div>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </section>

              <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <Clock3 className="h-4 w-4 text-blue-500" />
                    <h2 className="font-bold">Hộp thư</h2>
                  </div>
                  <span className="text-xs text-slate-500 dark:text-slate-400">
                    {conversations.length} cuộc trò chuyện
                  </span>
                </div>

                {conversationsLoading ? (
                  <div className="py-12 text-center text-sm text-slate-500 dark:text-slate-400">
                    <Loader2 className="mx-auto mb-2 h-6 w-6 animate-spin text-blue-500" />
                    Đang tải hội thoại...
                  </div>
                ) : conversations.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-slate-200/80 px-4 py-10 text-center text-sm text-slate-500 dark:border-neutral-800 dark:text-slate-400">
                    Chưa có cuộc trò chuyện nào.
                    <div className="mt-1">
                      Tìm người dùng ở phía trên để bắt đầu.
                    </div>
                  </div>
                ) : (
                  <div className="max-h-[calc(100vh-320px)] overflow-y-auto pr-1">
                    <div className="flex flex-col gap-2">
                      {conversations.map((conversation) => {
                        const active =
                          conversation.id === selectedConversationId;
                        return (
                          <button
                            key={conversation.id}
                            onClick={() => handleConversationPick(conversation)}
                            className={`rounded-2xl border p-4 text-left transition ${
                              active
                                ? "border-blue-200 bg-blue-50/70 dark:border-blue-500/20 dark:bg-blue-500/10"
                                : "border-slate-200/80 hover:bg-slate-50 dark:border-neutral-800 dark:hover:bg-neutral-900"
                            }`}
                          >
                            <div className="flex items-start gap-3">
                              <div
                                className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full border text-sm font-bold ${
                                  active
                                    ? "border-blue-200 bg-blue-100 text-blue-700 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300"
                                    : "border-slate-200 bg-slate-100 text-slate-600 dark:border-neutral-800 dark:bg-neutral-900 dark:text-slate-400"
                                }`}
                              >
                                {getInitials(
                                  conversation.otherParticipantUsername,
                                )}
                              </div>
                              <div className="min-w-0 flex-1">
                                <div className="flex items-center justify-between gap-3">
                                  <p className="truncate font-semibold text-slate-900 dark:text-white">
                                    @{conversation.otherParticipantUsername}
                                  </p>
                                  <span className="text-[11px] text-slate-400 dark:text-slate-500">
                                    {formatRelativeTime(
                                      conversation.lastMessageAt,
                                    )}
                                  </span>
                                </div>
                                <p className="mt-1 line-clamp-2 text-sm text-slate-500 dark:text-slate-400">
                                  {conversation.lastMessagePreview ||
                                    "Chưa có tin nhắn."}
                                </p>
                                <div className="mt-2 flex items-center justify-between">
                                  <span
                                    className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold ${
                                      conversation.unreadCount > 0
                                        ? "bg-blue-500/10 text-blue-600"
                                        : "bg-slate-100 text-slate-500 dark:bg-neutral-900 dark:text-slate-400"
                                    }`}
                                  >
                                    {conversation.unreadCount > 0 ? (
                                      <>
                                        <span className="h-1.5 w-1.5 rounded-full bg-blue-500" />
                                        {conversation.unreadCount} chưa đọc
                                      </>
                                    ) : (
                                      "Đã đọc"
                                    )}
                                  </span>
                                  {active && (
                                    <span className="text-[11px] font-bold uppercase tracking-[0.2em] text-blue-600 dark:text-blue-300">
                                      Đang mở
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}
              </section>
            </aside>

            {/* Chat Panel — hidden on mobile when list is shown */}
            <section className={`min-w-0 rounded-2xl border border-slate-200/80 bg-white shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e] ${
              mobileView === "list" ? "hidden xl:flex xl:flex-col" : "flex flex-col"
            }`}>
              {!selectedConversation ? (
                <div className="flex min-h-[70vh] flex-col items-center justify-center px-6 py-20 text-center">
                  <div className="rounded-full bg-blue-500/10 p-5 text-blue-500">
                    <MessageSquare className="h-10 w-10" />
                  </div>
                  <h2 className="mt-5 text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
                    Chọn một cuộc trò chuyện
                  </h2>
                  <p className="mt-3 max-w-xl text-sm leading-6 text-slate-500 dark:text-slate-400">
                    Mở hội thoại từ danh sách bên trái hoặc tìm người dùng mới
                    để bắt đầu nhắn tin realtime.
                  </p>
                  <button
                    onClick={() => {
                      const input = document.querySelector<HTMLInputElement>(
                        'input[placeholder="Tìm theo tên, username hoặc email..."]',
                      );
                      input?.focus();
                    }}
                    className="mt-6 inline-flex items-center gap-2 rounded-full bg-slate-900 px-5 py-3 text-sm font-bold text-white transition hover:bg-slate-800 dark:bg-white dark:text-slate-900 dark:hover:bg-slate-100"
                  >
                    <Search className="h-4 w-4" />
                    Tìm người dùng
                  </button>
                </div>
              ) : (
                <div className="flex min-h-[70vh] flex-col">
                  <header className="flex items-center justify-between gap-4 border-b border-slate-200/80 px-5 py-4 dark:border-neutral-800">
                    <div className="flex min-w-0 items-center gap-3">
                      {/* Back button — mobile only */}
                      <button
                        onClick={() => setMobileView("list")}
                        className="xl:hidden flex items-center justify-center w-9 h-9 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-800 text-slate-600 dark:text-neutral-300 transition-colors shrink-0"
                        aria-label="Quay lại"
                      >
                        <ArrowLeft className="w-5 h-5" />
                      </button>
                      <div className="h-12 w-12 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800 shrink-0">
                        <SafeAvatar
                          src={selectedContact?.avatarUrl}
                          alt={selectedConversation.otherParticipantUsername}
                        />
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <h2 className="truncate text-lg font-bold text-slate-900 dark:text-white">
                            {selectedContact?.displayName ||
                              selectedConversation.otherParticipantUsername}
                          </h2>
                          <span className="rounded-full bg-blue-500/10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.2em] text-blue-600 dark:text-blue-300">
                            Chat
                          </span>
                        </div>
                        <p className="truncate text-sm text-slate-500 dark:text-slate-400">
                          @{selectedConversation.otherParticipantUsername}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() =>
                          navigate(
                            `/profile/${selectedConversation.otherParticipantUsername}`,
                          )
                        }
                        className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 dark:border-neutral-800 dark:text-slate-200 dark:hover:bg-neutral-900"
                      >
                        Xem hồ sơ
                      </button>
                    </div>
                  </header>

                  {selectedConversationUnread > 0 && (
                    <div className="border-b border-blue-100 bg-blue-50/60 px-5 py-3 text-sm text-blue-700 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-200">
                      {selectedConversationUnread} tin nhắn chưa đọc.
                    </div>
                  )}

                  <div
                    ref={messageListRef}
                    className="flex-1 overflow-y-auto px-5 py-5"
                  >
                    {messagesLoading ? (
                      <div className="flex h-full items-center justify-center py-20 text-slate-500 dark:text-slate-400">
                        <Loader2 className="mr-2 h-5 w-5 animate-spin text-blue-500" />
                        Đang tải lịch sử...
                      </div>
                    ) : (
                      <div className="flex flex-col gap-3">
                        {hasMoreMessages && (
                          <button
                            onClick={() => void loadOlderMessages()}
                            disabled={loadingOlder}
                            className="mx-auto inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 disabled:opacity-50 dark:border-neutral-800 dark:text-slate-300 dark:hover:bg-neutral-900"
                          >
                            {loadingOlder ? (
                              <Loader2 className="h-3.5 w-3.5 animate-spin" />
                            ) : (
                              <ArrowLeft className="h-3.5 w-3.5" />
                            )}
                            Tải tin cũ hơn
                          </button>
                        )}

                        {messages.length === 0 ? (
                          <div className="flex flex-col items-center justify-center py-20 text-center text-slate-500 dark:text-slate-400">
                            <MessageSquare className="mb-3 h-10 w-10 text-slate-300 dark:text-neutral-700" />
                            Chưa có tin nhắn nào trong cuộc trò chuyện này.
                          </div>
                        ) : (
                          messages.map((message) => {
                            const isMine = message.senderId === currentUserId;
                            return (
                              <div
                                key={String(message.id)}
                                className={`flex ${isMine ? "justify-end" : "justify-start"}`}
                              >
                                <div
                                  className={`max-w-[80%] rounded-[1.5rem] px-4 py-3 shadow-sm ${
                                    isMine
                                      ? "bg-slate-900 text-white dark:bg-white dark:text-slate-900"
                                      : "border border-slate-200 bg-white text-slate-800 dark:border-neutral-800 dark:bg-neutral-950 dark:text-slate-200"
                                  } ${message.optimistic ? "opacity-80" : ""}`}
                                >
                                  <p className="whitespace-pre-wrap break-words text-sm leading-6">
                                    {message.content}
                                  </p>
                                  <div
                                    className={`mt-2 flex items-center gap-2 text-[11px] ${isMine ? "text-white/70 dark:text-slate-500" : "text-slate-400 dark:text-slate-500"}`}
                                  >
                                    <span>
                                      {formatClockTime(message.timestamp)}
                                    </span>
                                    {isMine && (
                                      <>
                                        <span>·</span>
                                        <span className="inline-flex items-center gap-1">
                                          {message.status === "READ" ? (
                                            <CheckCheck className="h-3.5 w-3.5" />
                                          ) : (
                                            <CheckCheck className="h-3.5 w-3.5 opacity-70" />
                                          )}
                                          {message.status === "SENT" &&
                                            "Đã gửi"}
                                          {message.status === "DELIVERED" &&
                                            "Đã giao"}
                                          {message.status === "READ" &&
                                            "Đã đọc"}
                                        </span>
                                      </>
                                    )}
                                  </div>
                                </div>
                              </div>
                            );
                          })
                        )}
                      </div>
                    )}
                  </div>

                  <footer className="border-t border-slate-200/80 px-5 py-4 dark:border-neutral-800">
                    <div className="rounded-3xl border border-slate-200/80 bg-slate-50 p-3 dark:border-neutral-800 dark:bg-neutral-950">
                      <textarea
                        id="chat-message-input"
                        name="message"
                        value={draft}
                        onChange={(event) => setDraft(event.target.value)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter" && !event.shiftKey) {
                            event.preventDefault();
                            void handleSendMessage();
                          }
                        }}
                        placeholder="Soạn tin nhắn..."
                        rows={3}
                        maxLength={2000}
                        className="w-full resize-none bg-transparent px-1 py-1 text-sm leading-6 outline-none placeholder:text-slate-400 dark:text-white dark:placeholder:text-slate-500"
                      />
                      <div className="mt-2 flex items-center justify-between gap-3">
                        <span className="text-[11px] text-slate-400 dark:text-slate-500">
                          Enter để gửi, Shift+Enter để xuống dòng
                        </span>
                        <button
                          onClick={() => void handleSendMessage()}
                          disabled={sending || !draft.trim()}
                          className="inline-flex items-center gap-2 rounded-full bg-blue-600 px-4 py-2 text-sm font-bold text-white transition hover:bg-blue-700 disabled:opacity-50"
                        >
                          {sending ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Send className="h-4 w-4" />
                          )}
                          Gửi
                        </button>
                      </div>
                    </div>
                  </footer>
                </div>
              )}
            </section>
          </div>
        </div>
      </div>
      <BottomNavBar active="chat" />
    </div>
  );
}
