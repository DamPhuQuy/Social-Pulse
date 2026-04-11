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

const DEFAULT_API_BASE_URL = "http://localhost:8080/api/v1";
const DEFAULT_REGISTER_ENDPOINT = "/auth/register";
const DEFAULT_VERIFY_EMAIL_ENDPOINT = "/auth/verify-email";
const DEFAULT_LOGIN_ENDPOINT = "/auth/login";
const DEFAULT_LOGOUT_ENDPOINT = "/auth/logout";
const DEFAULT_REFRESH_ENDPOINT = "/auth/refresh";
const DEFAULT_ME_ENDPOINT = "/auth/me";
const DEFAULT_FORGOT_PASSWORD_ENDPOINT = "/auth/forgot-password";
const DEFAULT_RESEND_OTP_ENDPOINT = "/auth/resend-otp";
const DEFAULT_RESET_PASSWORD_ENDPOINT = "/auth/reset-password";

function buildAuthUrl(endpointValue: string, defaultEndpoint: string): string {
  const baseUrl = (
    import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL
  ).trim();
  const endpoint = (endpointValue || defaultEndpoint).trim();

  if (/^https?:\/\//i.test(endpoint)) {
    return endpoint;
  }

  const normalizedBase = baseUrl.replace(/\/+$/, "");
  const normalizedEndpoint = endpoint.startsWith("/")
    ? endpoint
    : `/${endpoint}`;

  return `${normalizedBase}${normalizedEndpoint}`;
}

function getRegisterUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_REGISTER_ENDPOINT ?? "",
    DEFAULT_REGISTER_ENDPOINT,
  );
}

function getVerifyEmailUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_VERIFY_EMAIL_ENDPOINT ?? "",
    DEFAULT_VERIFY_EMAIL_ENDPOINT,
  );
}

function getLoginUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_LOGIN_ENDPOINT ?? "",
    DEFAULT_LOGIN_ENDPOINT,
  );
}

function getLogoutUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_LOGOUT_ENDPOINT ?? "",
    DEFAULT_LOGOUT_ENDPOINT,
  );
}

function getRefreshUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_REFRESH_ENDPOINT ?? "",
    DEFAULT_REFRESH_ENDPOINT,
  );
}

function getMeUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_ME_ENDPOINT ?? "",
    DEFAULT_ME_ENDPOINT,
  );
}

function getForgotPasswordUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_FORGOT_PASSWORD_ENDPOINT ?? "",
    DEFAULT_FORGOT_PASSWORD_ENDPOINT,
  );
}

function getResendOtpUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_RESEND_OTP_ENDPOINT ?? "",
    DEFAULT_RESEND_OTP_ENDPOINT,
  );
}

function getResetPasswordUrl(): string {
  return buildAuthUrl(
    import.meta.env.VITE_RESET_PASSWORD_ENDPOINT ?? "",
    DEFAULT_RESET_PASSWORD_ENDPOINT,
  );
}

function readMessage(data: unknown): string | null {
  if (!data || typeof data !== "object") {
    return null;
  }

  const payload = data as Record<string, unknown>;

  if (typeof payload.message === "string" && payload.message.trim()) {
    return payload.message;
  }

  if (typeof payload.error === "string" && payload.error.trim()) {
    return payload.error;
  }

  if (payload.data && typeof payload.data === "object") {
    const nestedData = payload.data as Record<string, unknown>;

    if (typeof nestedData.message === "string" && nestedData.message.trim()) {
      return nestedData.message;
    }
  }

  return null;
}

/** JSON headers dùng chung cho các request có body */
const JSON_HEADERS = {
  "Content-Type": "application/json",
  "Accept": "application/json",
  "Accept-Encoding": "gzip, deflate, br",
  "Connection": "keep-alive",
};

// ---------------------------------------------------------------------------
// POST /auth/register
// ---------------------------------------------------------------------------
export async function registerUser(
  payload: RegisterRequest,
): Promise<RegisterResponse> {
  const registerUrl: string = getRegisterUrl();
  const requestBody: RegisterApiRequest = {
    username: payload.username,
    email: payload.email,
    rawPassword: payload.password,
    confirmPassword: payload.confirmPassword,
  };

  try {
    const response = await axios.post(registerUrl, requestBody, {
      headers: JSON_HEADERS,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Registration request succeeded with status ${response.status}.`,
      data: response.data,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Registration request failed. Please try again.",
        data,
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while sending register request.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/verify-email
// ---------------------------------------------------------------------------
export async function verifyEmailOtp(
  payload: VerifyEmailOtpRequest,
): Promise<VerifyEmailOtpResponse> {
  const verifyEmailUrl = getVerifyEmailUrl();

  try {
    const response = await axios.post(verifyEmailUrl, payload, {
      headers: JSON_HEADERS,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Verify email request succeeded with status ${response.status}.`,
      data: response.data,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Verify email request failed. Please try again.",
        data,
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while verifying OTP.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/login
// Backend trả về: ApiResponse<{ accessToken, tokenType, expiresIn }>
// Refresh Token được set tự động vào HttpOnly cookie "sp_refresh_token"
// ---------------------------------------------------------------------------
export async function loginUser(payload: LoginRequest): Promise<LoginResponse> {
  const loginUrl = getLoginUrl();

  try {
    const response = await axios.post(loginUrl, payload, {
      headers: JSON_HEADERS,
      withCredentials: true, // cần để nhận HttpOnly refresh-token cookie
    });

    // Backend: { code, message, data: { accessToken, tokenType, expiresIn } }
    const accessToken: string | undefined =
      response.data?.data?.accessToken ?? undefined;

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Login request succeeded with status ${response.status}.`,
      accessToken,
      data: response.data,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Login request failed. Please try again.",
        data,
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while sending login request.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/refresh
// Gửi HttpOnly cookie "sp_refresh_token" (tự động do trình duyệt)
// Backend trả về: ApiResponse<{ accessToken, tokenType, expiresIn }>
// ---------------------------------------------------------------------------
export async function refreshToken(): Promise<RefreshResponse> {
  const refreshUrl = getRefreshUrl();

  try {
    const response = await axios.post(
      refreshUrl,
      {},
      {
        headers: JSON_HEADERS,
        withCredentials: true, // cần để gửi HttpOnly refresh-token cookie
      },
    );

    // Backend: { code, message, data: { accessToken, tokenType, expiresIn } }
    const accessToken: string | undefined =
      response.data?.data?.accessToken ?? undefined;

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Token refresh succeeded with status ${response.status}.`,
      accessToken,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Token refresh failed. Please log in again.",
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while refreshing token.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/logout
// Backend xóa cookie "sp_refresh_token" bằng cách set maxAge=0
// ---------------------------------------------------------------------------
export async function logoutUser(): Promise<LogoutResponse> {
  const logoutUrl = getLogoutUrl();

  try {
    const response = await axios.post(
      logoutUrl,
      {},
      {
        headers: JSON_HEADERS,
        withCredentials: true, // cần để trình duyệt nhận cookie bị xóa
      },
    );

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Logout request succeeded with status ${response.status}.`,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Logout request failed. Please try again.",
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while sending logout request.",
    };
  }
}

// ---------------------------------------------------------------------------
// GET /auth/me  (cần Access Token trong Authorization header)
// ---------------------------------------------------------------------------
export async function getCurrentUser(accessToken: string): Promise<MeResponse> {
  const meUrl = getMeUrl();

  try {
    const response = await axios.get(meUrl, {
      headers: {
        ...JSON_HEADERS,
        Authorization: `Bearer ${accessToken}`,
      },
      withCredentials: true,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Session check succeeded with status ${response.status}.`,
      data: response.data,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Failed to fetch current user.",
        data: undefined,
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while fetching current user.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/forgot-password
// ---------------------------------------------------------------------------
export async function forgotPassword(
  payload: ForgotPasswordRequest,
): Promise<PasswordResponse> {
  const url = getForgotPasswordUrl();

  try {
    const response = await axios.post(url, payload, {
      headers: JSON_HEADERS,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Forgot password request succeeded with status ${response.status}.`,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Forgot password request failed. Please try again.",
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while sending forgot password request.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/resend-otp
// ---------------------------------------------------------------------------
export async function resendOtp(
  payload: ResendOtpRequest,
): Promise<PasswordResponse> {
  const url = getResendOtpUrl();

  try {
    const response = await axios.post(url, payload, {
      headers: JSON_HEADERS,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Resend OTP request succeeded with status ${response.status}.`,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Resend OTP request failed. Please try again.",
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while resending OTP.",
    };
  }
}

// ---------------------------------------------------------------------------
// POST /auth/reset-password
// ---------------------------------------------------------------------------
export async function resetPassword(
  payload: ResetPasswordRequest,
): Promise<PasswordResponse> {
  const url = getResetPasswordUrl();

  try {
    const response = await axios.post(url, payload, {
      headers: JSON_HEADERS,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Password reset succeeded with status ${response.status}.`,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Password reset failed. Please try again.",
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while resetting password.",
    };
  }
}

export async function getMeAuth(): Promise<LoginResponse> {
  const sessionUrl = getMeUrl();

  try {
    const response = await axios.get(sessionUrl, {
      headers: {
        "Content-Type": "application/json",
        "Content-Length": JSON.stringify({}).length,
        "Accept": "application/json",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive", // persistent connection
      },
      withCredentials: true,
    });

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Session check succeeded with status ${response.status}.`,
      data: response.data,
    };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;

      return {
        ok: false,
        status,
        message:
          readMessage(data) ??
          error.message ??
          "Session check failed.",
        data,
      };
    }

    return {
      ok: false,
      message: "Unexpected error occurred while checking session.",
    };
  }
}
