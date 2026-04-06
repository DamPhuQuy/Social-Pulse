import axios from "axios";

export type RegisterRequest = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
};

type RegisterApiRequest = {
  username: string;
  email: string;
  rawPassword: string;
  confirmPassword: string;
};

export type RegisterResult = {
  ok: boolean;
  status?: number;
  message: string;
  data?: unknown;
};

export type VerifyEmailOtpRequest = {
  email: string;
  otpCode: string;
};

export type VerifyEmailOtpResult = {
  ok: boolean;
  status?: number;
  message: string;
  data?: unknown;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResult = {
  ok: boolean;
  status?: number;
  message: string;
  data?: unknown;
};

const DEFAULT_API_BASE_URL = "http://localhost:8080/api/v1";
const DEFAULT_REGISTER_ENDPOINT = "/auth/register";
const DEFAULT_VERIFY_EMAIL_ENDPOINT = "/auth/verify-email";
const DEFAULT_LOGIN_ENDPOINT = "/auth/login";

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

export async function registerUser(
  payload: RegisterRequest,
): Promise<RegisterResult> {
  const registerUrl = getRegisterUrl();
  const requestBody: RegisterApiRequest = {
    username: payload.username,
    email: payload.email,
    rawPassword: payload.password,
    confirmPassword: payload.confirmPassword,
  };

  try {
    const response = await axios.post(registerUrl, requestBody);

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

export async function verifyEmailOtp(
  payload: VerifyEmailOtpRequest,
): Promise<VerifyEmailOtpResult> {
  const verifyEmailUrl = getVerifyEmailUrl();

  try {
    const response = await axios.post(verifyEmailUrl, payload);

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

export async function loginUser(payload: LoginRequest): Promise<LoginResult> {
  const loginUrl = getLoginUrl();

  try {
    const response = await axios.post(loginUrl, payload);

    return {
      ok: true,
      status: response.status,
      message:
        readMessage(response.data) ??
        `Login request succeeded with status ${response.status}.`,
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
