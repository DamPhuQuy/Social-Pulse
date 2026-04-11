export type LoginResponse = {
  ok: boolean;
  status?: number;
  message: string;
  accessToken?: string;
  data?: unknown;
};
