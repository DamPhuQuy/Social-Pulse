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

const DEFAULT_REGISTER_ENDPOINT = "/auth/register";

function getRegisterUrl(): string {
  const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").trim();
  const endpoint = (
    import.meta.env.VITE_REGISTER_ENDPOINT ?? DEFAULT_REGISTER_ENDPOINT
  ).trim();

  if (/^https?:\/\//i.test(endpoint)) {
    return endpoint;
  }

  const normalizedBase = baseUrl.replace(/\/+$/, "");
  const normalizedEndpoint = endpoint.startsWith("/")
    ? endpoint
    : `/${endpoint}`;

  return `${normalizedBase}${normalizedEndpoint}`;
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
