import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { isAdminToken } from "@/lib/jwtUtils";
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
