import { setApiClientToken } from "@/lib/axiosClient";
import { refreshToken as refreshTokenApi } from "@/services/auth/authService";
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";

// ─── Types ───────────────────────────────────────────────────────────────────

type AuthState = {
  /** Access Token ngắn hạn (15 phút). Lưu trong memory — mất sau khi refresh tab. */
  accessToken: string | null;
  /** true khi đang trong quá trình khởi tạo (auto-refresh lần đầu) */
  isLoading: boolean;
};

type AuthContextValue = AuthState & {
  setAccessToken: (token: string | null) => void;
  logout: () => void;
};

// ─── Context ─────────────────────────────────────over────────────────────────────

const AuthContext = createContext<AuthContextValue | null>(null);

// ─── Provider ────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessTokenState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const didInit = useRef(false);

  // Khi app khởi động, thử tự refresh bằng RT cookie
  // Nếu RT còn sống → nhận AT mới (user không cần login lại)
  // Nếu RT hết hạn / không có → isLoading = false, user phải login
  useEffect(() => {
    if (didInit.current) return;
    didInit.current = true;

    const silentRefresh = async () => {
      try {
        const result = await refreshTokenApi();
        if (result.ok && result.accessToken) {
          setAccessTokenState(result.accessToken);
          setApiClientToken(result.accessToken);
        }
      } catch {
        // RT không hợp lệ — user cần login lại, không cần báo lỗi
      } finally {
        setIsLoading(false);
      }
    };

    silentRefresh();
  }, []);

  // Sync setApiClientToken khi token thay đổi
  useEffect(() => {
    setApiClientToken(accessToken);
  }, [accessToken]);

  // Lắng nghe events từ axiosClient interceptor để đồng bộ state
  useEffect(() => {
    const onRefreshed = (e: Event) => {
      const token = (e as CustomEvent<{ accessToken: string }>).detail.accessToken;
      setAccessTokenState(token);
    };
    const onLogoutRequired = () => {
      setAccessTokenState(null);
    };

    window.addEventListener("auth:token-refreshed", onRefreshed);
    window.addEventListener("auth:logout-required", onLogoutRequired);

    return () => {
      window.removeEventListener("auth:token-refreshed", onRefreshed);
      window.removeEventListener("auth:logout-required", onLogoutRequired);
    };
  }, []);

  const setAccessToken = useCallback((token: string | null) => {
    setAccessTokenState(token);
  }, []);

  const logout = useCallback(() => {
    setAccessTokenState(null);
  }, []);

  return (
    <AuthContext.Provider value={{ accessToken, isLoading, setAccessToken, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// ─── Hook ─────────────────────────────────────────────────────────────────────

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within <AuthProvider>");
  }
  return ctx;
}
