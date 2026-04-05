export const PATHS: {
  readonly ONBOARDING: "/";
  readonly HOME: "/home";
  readonly LOGIN: "/login";
  readonly REGISTER: "/register";
  readonly VERIFY_EMAIL: "/verify-email";
} = {
  ONBOARDING: "/",
  HOME: "/home",
  LOGIN: "/login",
  REGISTER: "/register",
  VERIFY_EMAIL: "/verify-email",
} as const;

export type AppPaths = (typeof PATHS)[keyof typeof PATHS];
