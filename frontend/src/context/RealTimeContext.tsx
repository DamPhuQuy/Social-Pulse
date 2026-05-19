import React, { createContext, useContext, useEffect, useRef } from "react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";

interface RealTimeContextType {
  // We can add any globally exposed functions or state here if needed
}

const RealTimeContext = createContext<RealTimeContextType | undefined>(undefined);

export const useRealTime = () => {
  const context = useContext(RealTimeContext);
  if (!context) {
    throw new Error("useRealTime must be used within a RealTimeProvider");
  }
  return context;
};

export const RealTimeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { accessToken } = useAuth();
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!accessToken) {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      return;
    }

    // Connect to the SSE endpoint
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "/api/v1";
    // Construct the absolute connection URL
    const baseUrl = apiBaseUrl.startsWith("http")
      ? apiBaseUrl
      : `${window.location.origin}${apiBaseUrl}`;

    const url = `${baseUrl}/realtime/connect?token=${encodeURIComponent(accessToken)}`;
    
    console.log("Connecting to SSE:", url);
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      console.log("SSE connected successfully");
    };

    eventSource.onerror = (err) => {
      console.error("SSE error occurred, will reconnect:", err);
    };

    // Listen to default "connected" confirmation
    eventSource.addEventListener("connected", (event) => {
      console.log("SSE Connection Confirmed:", event.data);
    });

    // Listen to "notification" events
    eventSource.addEventListener("notification", (event) => {
      try {
        const notification = JSON.parse(event.data);
        console.log("Realtime notification received:", notification);

        // Show standard toast notification
        toast.info(notification.message || "Bạn có thông báo mới!");

        // Dispatch a custom event to notify components (like AppSidebar to update count)
        const customEvent = new CustomEvent("realtime:notification", { detail: notification });
        window.dispatchEvent(customEvent);
      } catch (err) {
        console.error("Error parsing notification event data:", err);
      }
    });

    // Listen to "post_stats" events (likes, dislikes, comment counts)
    eventSource.addEventListener("post_stats", (event) => {
      try {
        const stats = JSON.parse(event.data);
        console.log("Realtime post stats received:", stats);

        // Dispatch custom event to notify HomePage/feed components to update stats
        const customEvent = new CustomEvent("realtime:post_stats", { detail: stats });
        window.dispatchEvent(customEvent);
      } catch (err) {
        console.error("Error parsing post stats event data:", err);
      }
    });

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [accessToken]);

  return (
    <RealTimeContext.Provider value={{}}>
      {children}
    </RealTimeContext.Provider>
  );
};
