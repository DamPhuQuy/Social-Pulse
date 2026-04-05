export const PATHS = {
  ONBOARDING: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  VERIFY_EMAIL: "/verify-email",
  LEARN_MORE: "/learn-more",
  TERMS: "/terms",
  PRIVACY: "/privacy",
} as const;

export type AppPaths = (typeof PATHS)[keyof typeof PATHS];
