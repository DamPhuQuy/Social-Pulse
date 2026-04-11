import { PATHS } from "@/constants/paths";
import ForgotPasswordPage from "@/pages/ForgotPasswordPage";
import LoginPage from "@/pages/LoginPage";
import OnboardingPage from "@/pages/OnboardingPage";
import RegisterPage from "@/pages/RegisterPage";
import ResetPasswordPage from "@/pages/ResetPasswordPage";
import VerifyOtpPage from "@/pages/VerifyOtpPage";

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
  {
    path: PATHS.LOGIN,
    element: LoginPage,
    isPrivate: false,
  },
  {
    path: PATHS.VERIFY_EMAIL,
    element: VerifyOtpPage,
    isPrivate: false,
  },
  {
    path: PATHS.FORGOT_PASSWORD,
    element: ForgotPasswordPage,
    isPrivate: false,
  },
  {
    path: PATHS.RESET_PASSWORD,
    element: ResetPasswordPage,
    isPrivate: false,
  },
];
