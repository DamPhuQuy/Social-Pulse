import { PATHS } from "@/constants/paths";
import ForgotPasswordPage from "@/pages/auth/ForgotPasswordPage";
import LoginPage from "@/pages/auth/LoginPage";
import OnboardingPage from "@/pages/auth/OnboardingPage";
import RegisterPage from "@/pages/auth/RegisterPage";
import ResetPasswordOtpPage from "@/pages/auth/ResetPasswordOtpPage";
import ResetPasswordPage from "@/pages/auth/ResetPasswordPage";
import VerifyOtpPage from "@/pages/auth/VerifyOtpPage";

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
    element: ResetPasswordOtpPage,
    isPrivate: false,
  },
  {
    path: PATHS.RESET_PASSWORD_NEW,
    element: ResetPasswordPage,
    isPrivate: false,
  },
];
