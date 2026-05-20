import { PATHS } from "@/constants/paths";
import ForgotPasswordPage from "@/pages/auth/ForgotPasswordPage";
import LoginPage from "@/pages/auth/LoginPage";
import OnboardingPage from "@/pages/auth/OnboardingPage";
import RegisterPage from "@/pages/auth/RegisterPage";
import ResetPasswordOtpPage from "@/pages/auth/ResetPasswordOtpPage";
import ResetPasswordPage from "@/pages/auth/ResetPasswordPage";
import VerifyOtpPage from "@/pages/auth/VerifyOtpPage";
import BookmarksPage from "@/pages/BookmarksPage";
import DiscoveryPage from "@/pages/DiscoveryPage";
import HomePage from "@/pages/HomePage";
import NotificationsPage from "@/pages/NotificationsPage";
import PostDetailPage from "@/pages/PostDetailPage";
import ProfilePage from "@/pages/ProfilePage";
import SettingsPage from "@/pages/SettingsPage";
import AdminDashboard from "@/pages/AdminDashboard";
import AiModelDashboard from "@/pages/AiModelDashboard";
import RbacManagement from "@/pages/RbacManagement";

type RouteConfig = {
  path: (typeof PATHS)[keyof typeof PATHS];
  element: React.ComponentType;
  isPrivate: boolean;
  requiresAdmin?: boolean;
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
  {
    path: PATHS.HOME,
    element: HomePage,
    isPrivate: true,
  },
  {
    path: PATHS.DISCOVERY,
    element: DiscoveryPage,
    isPrivate: true,
  },
  {
    path: PATHS.NOTIFICATIONS,
    element: NotificationsPage,
    isPrivate: true,
  },
  {
    path: PATHS.BOOKMARKS,
    element: BookmarksPage,
    isPrivate: true,
  },
  {
    path: PATHS.SETTINGS,
    element: SettingsPage,
    isPrivate: true,
  },
  {
    path: PATHS.POST_DETAIL,
    element: PostDetailPage,
    isPrivate: true,
  },
  {
    path: PATHS.PROFILE,
    element: ProfilePage,
    isPrivate: true,
  },
  {
    path: PATHS.USER_PROFILE,
    element: ProfilePage,
    isPrivate: false,
  },
  {
    path: PATHS.ADMIN_REPORTS,
    element: AdminDashboard,
    isPrivate: true,
    requiresAdmin: true,
  },
  {
    path: PATHS.ADMIN_AI,
    element: AiModelDashboard,
    isPrivate: true,
    requiresAdmin: true,
  },
  {
    path: PATHS.ADMIN_RBAC,
    element: RbacManagement,
    isPrivate: true,
    requiresAdmin: true,
  },
];
