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

// ─── Types ────────────────────────────────────────────────────────────────────

type AuthState = {
  /**
   * Short-lived Access Token (15 min).
   * Stored in memory only — lost when the tab is refreshed or closed.
   */
  accessToken: string | null;
  /**
   * True while the initial silent-refresh attempt is in progress.
   * Use this to show a loading skeleton instead of redirecting the user.
   */
  isLoading: boolean;
};

type AuthContextValue = AuthState & {
  /** Store a new Access Token after login or token refresh */
  setAccessToken: (token: string | null) => void;
  /** Clear the Access Token (local only — does not call the server) */
  logout: () => void;
};

// ─── Context ──────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextValue | null>(null);

// ─── Provider ─────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessTokenState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const didInit = useRef(false);

  /**
   * On app startup, attempt a silent token refresh using the HttpOnly
   * refresh-token cookie. If it succeeds, the user stays logged in without
   * needing to re-enter credentials. If it fails, they must log in manually.
   */
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
        // RT is invalid or expired — user needs to log in again
      } finally {
        setIsLoading(false);
      }
    };

    silentRefresh();
  }, []);

  // Keep the Axios client in sync with the latest token
  useEffect(() => {
    setApiClientToken(accessToken);
  }, [accessToken]);

  /**
   * Listen to custom events dispatched by the Axios interceptor so that the
   * auth context stays in sync with background token refreshes.
   */
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

/**
 * Returns the current auth context.
 * Must be used inside an `<AuthProvider>` tree.
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within <AuthProvider>");
  }
  return ctx;
}
