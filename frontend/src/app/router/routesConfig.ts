import { PATHS } from "@/shared/constants/paths";
import ForgotPasswordPage from "@/features/authentication/presentation/pages/ForgotPasswordPage";
import LoginPage from "@/features/authentication/presentation/pages/LoginPage";
import OnboardingPage from "@/features/authentication/presentation/pages/OnboardingPage";
import RegisterPage from "@/features/authentication/presentation/pages/RegisterPage";
import ResetPasswordOtpPage from "@/features/authentication/presentation/pages/ResetPasswordOtpPage";
import ResetPasswordPage from "@/features/authentication/presentation/pages/ResetPasswordPage";
import VerifyOtpPage from "@/features/authentication/presentation/pages/VerifyOtpPage";
import BookmarksPage from "@/features/bookmarks/presentation/pages/BookmarksPage";
import DiscoveryPage from "@/features/discovery/presentation/pages/DiscoveryPage";
import HomePage from "@/features/feed/presentation/pages/HomePage";
import NotificationsPage from "@/features/notifications/presentation/pages/NotificationsPage";
import PostDetailPage from "@/features/feed/presentation/pages/PostDetailPage";
import ProfilePage from "@/features/profiles/presentation/pages/ProfilePage";
import SettingsPage from "@/features/static-pages/presentation/pages/SettingsPage";
import AdminDashboard from "@/features/admin/presentation/pages/AdminDashboard";
import AiModelDashboard from "@/features/admin/presentation/pages/AiModelDashboard";
import RbacManagement from "@/features/admin/presentation/pages/RbacManagement";
import LearnMorePage from "@/features/static-pages/presentation/pages/LearnMorePage";
import TermsPage from "@/features/static-pages/presentation/pages/TermsPage";
import PrivacyPage from "@/features/static-pages/presentation/pages/PrivacyPage";
import ChatPage from "@/features/chat/presentation/pages/ChatPage";

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
    path: PATHS.LEARN_MORE,
    element: LearnMorePage,
    isPrivate: false,
  },
  {
    path: PATHS.TERMS,
    element: TermsPage,
    isPrivate: false,
  },
  {
    path: PATHS.PRIVACY,
    element: PrivacyPage,
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
    isPrivate: false,
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
    path: PATHS.CHAT,
    element: ChatPage,
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
    isPrivate: true,
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
