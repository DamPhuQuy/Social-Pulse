import type { ForgotPasswordRequest } from "@/types/request/ForgotPasswordRequest";
import type { LoginRequest } from "@/types/request/LoginRequest";
import type { RegisterApiRequest } from "@/types/request/RegisterApiRequest";
import type { RegisterRequest } from "@/types/request/RegisterRequest";
import type { ResendOtpRequest } from "@/types/request/ResendOtpRequest";
import type { ResetPasswordRequest } from "@/types/request/ResetPasswordRequest";
import type { VerifyEmailOtpRequest } from "@/types/request/VerifyEmailOtpRequest";

import type { LoginResponse } from "@/types/response/LoginResponse";
import type { LogoutResponse } from "@/types/response/LogoutResponse";
import type { MeResponse } from "@/types/response/MeResponse";
import type { PasswordResponse } from "@/types/response/PasswordResponse";
import type { RefreshResponse } from "@/types/response/RefreshResponse";
import type { RegisterResponse } from "@/types/response/RegisterReponse";
import type { VerifyEmailOtpResponse } from "@/types/response/VerifyEmailOtpResponse";

import axios from "axios";

// ─── URL Helpers ─────────────────────────────────────────────────────────────

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1"
).trim().replace(/\/+$/, "");

/**
 * Builds a full API endpoint URL.
 *
 * @param envVar - The value of the VITE_ env variable (may be empty string).
 * @param defaultPath - The default path to use when the env var is not set.
 */
function buildEndpointUrl(envVar: string | undefined, defaultPath: string): string {
  const path = (envVar || defaultPath).trim();
  // Already a full URL (e.g. from env override)
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

const ENDPOINTS = {
  register:       () => buildEndpointUrl(import.meta.env.VITE_REGISTER_ENDPOINT,        "/auth/register"),
  verifyEmail:    () => buildEndpointUrl(import.meta.env.VITE_VERIFY_EMAIL_ENDPOINT,    "/auth/verify-email"),
  login:          () => buildEndpointUrl(import.meta.env.VITE_LOGIN_ENDPOINT,           "/auth/login"),
  logout:         () => buildEndpointUrl(import.meta.env.VITE_LOGOUT_ENDPOINT,          "/auth/logout"),
  refresh:        () => buildEndpointUrl(import.meta.env.VITE_REFRESH_ENDPOINT,         "/auth/refresh"),
  me:             () => buildEndpointUrl(import.meta.env.VITE_ME_ENDPOINT,              "/auth/me"),
  forgotPassword: () => buildEndpointUrl(import.meta.env.VITE_FORGOT_PASSWORD_ENDPOINT, "/auth/forgot-password"),
  resendOtp:      () => buildEndpointUrl(import.meta.env.VITE_RESEND_OTP_ENDPOINT,      "/auth/resend-otp"),
  resetPassword:  () => buildEndpointUrl(import.meta.env.VITE_RESET_PASSWORD_ENDPOINT,  "/auth/reset-password"),
  verifyOtp:      () => buildEndpointUrl(import.meta.env.VITE_VERIFY_OTP_ENDPOINT,      "/auth/verify-otp"),
} as const;

// ─── Shared Utilities ─────────────────────────────────────────────────────────

/** JSON headers used for all requests that send a body */
const JSON_HEADERS = {
  "Content-Type": "application/json",
  Accept: "application/json",
  "Accept-Encoding": "gzip, deflate, br",
  Connection: "keep-alive",
};

/**
 * Extracts the human-readable error / success message from an API response body.
 * Checks `message`, `error`, and `data.message` fields.
 */
function readMessage(data: unknown): string | null {
  if (!data || typeof data !== "object") return null;

  const payload = data as Record<string, unknown>;

  if (typeof payload.message === "string" && payload.message.trim()) {
    return payload.message;
  }

  if (typeof payload.error === "string" && payload.error.trim()) {
    return payload.error;
  }

  const nested = payload.data;
  if (nested && typeof nested === "object") {
    const nestedMsg = (nested as Record<string, unknown>).message;
    if (typeof nestedMsg === "string" && nestedMsg.trim()) return nestedMsg;
  }

  return null;
}

/**
 * Wraps an Axios call and maps success / HTTP error / network error into a
 * consistent result object with `{ ok, status?, message, data? }`.
 */
async function safeRequest<T extends { ok: boolean }>(
  call: () => Promise<T>,
  fallbackMessage: string,
): Promise<T> {
  try {
    return await call();
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;
      return {
        ok: false,
        status,
        message: readMessage(data) ?? error.message ?? fallbackMessage,
        data,
      } as unknown as T;
    }
    return { ok: false, message: fallbackMessage } as unknown as T;
  }
}

// ─── Auth API Functions ───────────────────────────────────────────────────────

export async function registerUser(payload: RegisterRequest): Promise<RegisterResponse> {
  const body: RegisterApiRequest = {
    username: payload.username,
    email: payload.email,
    rawPassword: payload.password,
    confirmPassword: payload.confirmPassword,
  };

  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.register(), body, { headers: JSON_HEADERS });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Registration succeeded (${response.status}).`,
      data: response.data,
    };
  }, "Registration failed. Please try again.");
}

export async function verifyEmailOtp(payload: VerifyEmailOtpRequest): Promise<VerifyEmailOtpResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.verifyEmail(), payload, { headers: JSON_HEADERS });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Email verified (${response.status}).`,
      data: response.data,
    };
  }, "OTP verification failed. Please try again.");
}

export async function loginUser(payload: LoginRequest): Promise<LoginResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.login(), payload, {
      headers: JSON_HEADERS,
      withCredentials: true, // needed to receive the HttpOnly refresh-token cookie
    });
    const accessToken: string | undefined = response.data?.data?.accessToken ?? undefined;
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Login succeeded (${response.status}).`,
      accessToken,
      data: response.data,
    };
  }, "Login failed. Please try again.");
}

export async function refreshToken(): Promise<RefreshResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.refresh(), {}, {
      headers: JSON_HEADERS,
      withCredentials: true, // needed to send the HttpOnly refresh-token cookie
    });
    const accessToken: string | undefined = response.data?.data?.accessToken ?? undefined;
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Token refreshed (${response.status}).`,
      accessToken,
    };
  }, "Token refresh failed. Please log in again.");
}

export async function logoutUser(): Promise<LogoutResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.logout(), {}, {
      headers: JSON_HEADERS,
      withCredentials: true, // needed so the browser clears the cookie
    });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Logout succeeded (${response.status}).`,
    };
  }, "Logout failed. Please try again.");
}

export async function getCurrentUser(accessToken: string): Promise<MeResponse> {
  return safeRequest(async () => {
    const response = await axios.get(ENDPOINTS.me(), {
      headers: { ...JSON_HEADERS, Authorization: `Bearer ${accessToken}` },
      withCredentials: true,
    });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Session check succeeded (${response.status}).`,
      data: response.data,
    };
  }, "Failed to fetch current user.");
}

export async function forgotPassword(payload: ForgotPasswordRequest): Promise<PasswordResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.forgotPassword(), payload, { headers: JSON_HEADERS });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Reset OTP sent (${response.status}).`,
    };
  }, "Failed to send reset code. Please try again.");
}

export async function resendOtp(payload: ResendOtpRequest): Promise<PasswordResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.resendOtp(), payload, { headers: JSON_HEADERS });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `OTP resent (${response.status}).`,
    };
  }, "Resend OTP failed. Please try again.");
}

export async function resetPassword(payload: ResetPasswordRequest): Promise<PasswordResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.resetPassword(), payload, { headers: JSON_HEADERS });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Password reset succeeded (${response.status}).`,
    };
  }, "Password reset failed. Please try again.");
}

export async function verifyResetOtp(payload: {
  email: string;
  otpCode: string;
}): Promise<PasswordResponse> {
  return safeRequest(async () => {
    const response = await axios.post(ENDPOINTS.verifyOtp(), payload, { headers: JSON_HEADERS });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `OTP verified (${response.status}).`,
    };
  }, "OTP verification failed. Please try again.");
}

export async function getMeAuth(): Promise<LoginResponse> {
  return safeRequest(async () => {
    const response = await axios.get(ENDPOINTS.me(), {
      headers: JSON_HEADERS,
      withCredentials: true,
    });
    return {
      ok: true,
      status: response.status,
      message: readMessage(response.data) ?? `Session check succeeded (${response.status}).`,
      data: response.data,
    };
  }, "Session check failed.");
}
