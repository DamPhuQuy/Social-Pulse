export const PATHS = {
  ONBOARDING: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  VERIFY_EMAIL: "/verify-email",
  FORGOT_PASSWORD: "/forgot-password",
  RESET_PASSWORD: "/reset-password",
  LEARN_MORE: "/learn-more",
  TERMS: "/terms",
  PRIVACY: "/privacy",
} as const;

export type AppPaths = (typeof PATHS)[keyof typeof PATHS];
