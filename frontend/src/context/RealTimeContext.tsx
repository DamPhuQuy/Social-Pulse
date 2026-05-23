import React, { createContext, useContext, useEffect, useRef } from "react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";

type RealTimeContextType = Record<string, never>;

const RealTimeContext = createContext<RealTimeContextType | undefined>(undefined);

const RECONNECT_DELAY_MS = 5000;

export const useRealTime = () => {
  const context = useContext(RealTimeContext);
  if (!context) {
    throw new Error("useRealTime must be used within a RealTimeProvider");
  }
  return context;
};

async function readSseStream(
  response: Response,
  handlers: Record<string, (data: string) => void>,
  signal: AbortSignal,
) {
  if (!response.body) {
    throw new Error("SSE response has no body");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let eventName = "message";
  let dataLines: string[] = [];

  const flushEvent = () => {
    if (!dataLines.length) {
      eventName = "message";
      return;
    }

    const payload = dataLines.join("\n");
    handlers[eventName]?.(payload);
    eventName = "message";
    dataLines = [];
  };

  while (!signal.aborted) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });

    let newlineIndex = buffer.indexOf("\n");
    while (newlineIndex >= 0) {
      const rawLine = buffer.slice(0, newlineIndex).replace(/\r$/, "");
      buffer = buffer.slice(newlineIndex + 1);

      if (!rawLine) {
        flushEvent();
      } else if (rawLine.startsWith("event:")) {
        eventName = rawLine.slice(6).trim();
      } else if (rawLine.startsWith("data:")) {
        dataLines.push(rawLine.slice(5).trimStart());
      }

      newlineIndex = buffer.indexOf("\n");
    }
  }

  flushEvent();
}

export const RealTimeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { accessToken } = useAuth();
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (!accessToken) {
      abortRef.current?.abort();
      abortRef.current = null;
      return;
    }

    const controller = new AbortController();
    abortRef.current = controller;

    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "/api/v1";
    const baseUrl = apiBaseUrl.startsWith("http")
      ? apiBaseUrl
      : `${window.location.origin}${apiBaseUrl}`;
    const url = `${baseUrl}/realtime/connect`;

    const connect = async () => {
      while (!controller.signal.aborted) {
        try {
          const response = await fetch(url, {
            method: "GET",
            headers: {
              Authorization: `Bearer ${accessToken}`,
              Accept: "text/event-stream",
            },
            signal: controller.signal,
          });

          if (!response.ok) {
            throw new Error(`SSE request failed with status ${response.status}`);
          }

          await readSseStream(
            response,
            {
              notification: (payload) => {
                try {
                  const notification = JSON.parse(payload);
                  toast.info(notification.message || "Bạn có thông báo mới!");
                  window.dispatchEvent(new CustomEvent("realtime:notification", { detail: notification }));
                } catch (error) {
                  console.error("Error parsing notification event data:", error);
                }
              },
              post_stats: (payload) => {
                try {
                  const stats = JSON.parse(payload);
                  window.dispatchEvent(new CustomEvent("realtime:post_stats", { detail: stats }));
                } catch (error) {
                  console.error("Error parsing post stats event data:", error);
                }
              },
              feed_refresh: (payload) => {
                try {
                  const refresh = JSON.parse(payload);
                  window.dispatchEvent(new CustomEvent("realtime:feed_refresh", { detail: refresh }));
                } catch (error) {
                  console.error("Error parsing feed refresh event data:", error);
                }
              },
            },
            controller.signal,
          );
        } catch (error) {
          if (controller.signal.aborted) {
            return;
          }
          console.error("SSE connection error, reconnecting:", error);
          await new Promise((resolve) => window.setTimeout(resolve, RECONNECT_DELAY_MS));
        }
      }
    };

    void connect();

    return () => {
      controller.abort();
    };
  }, [accessToken]);

  return <RealTimeContext.Provider value={{}}>{children}</RealTimeContext.Provider>;
};
