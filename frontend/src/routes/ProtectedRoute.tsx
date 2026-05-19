import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { Navigate } from "react-router-dom";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { accessToken, isLoading } = useAuth();

  if (isLoading) {
    return null;
  }

  if (!accessToken) {
    return <Navigate to={PATHS.LOGIN} replace />;
  }

  return <>{children}</>;
};
