import { PATHS } from "@/constants/paths";
import OnboardingPage from "@/pages/OnboardingPage";
import RegisterPage from "@/pages/RegisterPage";

type RouteConfig = {
  path: (typeof PATHS)[keyof typeof PATHS];
  element: React.ComponentType;
  isPrivate: boolean;
};

export const routesConfig: RouteConfig[] = [
  {
    path: PATHS.ONBOARDING,
    element: OnboardingPage,
    isPrivate: false,
  },
  {
    path: PATHS.REGISTER,
    element: RegisterPage,
    isPrivate: false,
  },
];
