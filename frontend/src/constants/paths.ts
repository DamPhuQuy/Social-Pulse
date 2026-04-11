export const PATHS = {
  ONBOARDING: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  VERIFY_EMAIL: "/verify-email",
  FORGOT_PASSWORD: "/forgot-password",
  RESET_PASSWORD: "/reset-password",
  RESET_PASSWORD_NEW: "/reset-password-new",
  LEARN_MORE: "/learn-more",
  TERMS: "/terms",
  PRIVACY: "/privacy",
} as const;

export type AppPaths = (typeof PATHS)[keyof typeof PATHS];
