import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { isAdminToken } from "@/core/utils/jwtUtils";
import { Navigate } from "react-router-dom";

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiresAdmin?: boolean;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requiresAdmin = false }) => {
  const { accessToken, isLoading } = useAuth();

  if (isLoading) {
    return null;
  }

  if (!accessToken) {
    return <Navigate to={PATHS.LOGIN} replace />;
  }

  if (requiresAdmin && !isAdminToken(accessToken)) {
    return <Navigate to={PATHS.HOME} replace />;
  }

  return <>{children}</>;
};
