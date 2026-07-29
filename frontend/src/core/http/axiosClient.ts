import axios from "axios";

// ─── Axios Instance ───────────────────────────────────────────────────────────

/**
 * Axios client đã configured sẵn:
 *   - baseURL từ env (VITE_API_BASE_URL)
 *   - withCredentials: true → cookie sp_refresh_token tự đính kèm khi gọi /auth/refresh
 *
 * Interceptors:
 *   1. Request: gắn Authorization: Bearer <accessToken> (lấy từ module-level getter)
 *   2. Response: nếu gặp 401 → gọi /auth/refresh → retry request gốc với AT mới
 */

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1").trim();

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // Cần thiết để gửi RT cookie lên /auth/refresh
});

// ─── Token Store ─────────────────────────────────────────────────────────────

// Module-level store để interceptor có thể đọc AT mà không cần React context
let _accessToken: string | null = null;
let _isRefreshing = false;
let _pendingQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

export function setApiClientToken(token: string | null) {
  _accessToken = token;
}

function processQueue(token: string | null, error: unknown = null) {
  _pendingQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token);
    else reject(error);
  });
  _pendingQueue = [];
}

// ─── Request Interceptor ─────────────────────────────────────────────────────

apiClient.interceptors.request.use((config) => {
  if (_accessToken) {
    config.headers["Authorization"] = `Bearer ${_accessToken}`;
  }
  return config;
});

// ─── Response Interceptor (Auto-Refresh on 401) ───────────────────────────────

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Tránh retry vô hạn + bỏ qua lỗi từ chính /auth/refresh
    if (
      error.response?.status !== 401 ||
      originalRequest._retry ||
      originalRequest.url?.includes("/auth/refresh")
    ) {
      return Promise.reject(error);
    }

    if (_isRefreshing) {
      // Đang refresh → xếp hàng chờ
      return new Promise((resolve, reject) => {
        _pendingQueue.push({
          resolve: (token) => {
            originalRequest.headers["Authorization"] = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          },
          reject,
        });
      });
    }

    originalRequest._retry = true;
    _isRefreshing = true;

    try {
      const res = await axios.post(
        `${BASE_URL}/auth/refresh`,
        {},
        { withCredentials: true }
      );

      const newToken: string = res.data?.data?.accessToken;
      _accessToken = newToken;

      // Thông báo cho useAuth hook cập nhật state (nếu đã init)
      window.dispatchEvent(new CustomEvent("auth:token-refreshed", { detail: { accessToken: newToken } }));

      processQueue(newToken);
      originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
      return apiClient(originalRequest);

    } catch (refreshError) {
      processQueue(null, refreshError);
      _accessToken = null;

      // Thông báo logout cho useAuth hook
      window.dispatchEvent(new CustomEvent("auth:logout-required"));

      return Promise.reject(refreshError);
    } finally {
      _isRefreshing = false;
    }
  }
);
