import type { UserAuthorizedResponse } from "./AuthApiResponse";

export type MeResponse = {
  ok: boolean;
  status?: number;
  message: string;
  data?: UserAuthorizedResponse;
};
