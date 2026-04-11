/** Wrapper chung cho mọi response từ backend: ApiResponse<T> */
export type ApiResponse<T = null> = {
  code: number;
  message: string;
  data: T | null;
};

/** Payload bên trong data của /auth/login và /auth/refresh */
export type TokenPayload = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

/** Kết quả gọi /auth/me */
export type UserAuthorizedResponse = {
  id: number;
  email: string;
  role: string;
};
