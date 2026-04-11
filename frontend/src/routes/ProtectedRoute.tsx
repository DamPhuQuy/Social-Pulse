import { PATHS } from "@/constants/paths";
import { getMeAuth } from "@/services/auth/authService";
import { useEffect, useState } from "react";
import { Navigate } from "react-router";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const [isChecking, setIsChecking] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const validateSession = async () => {
      const result = await getMeAuth();

      if (isMounted) {
        setIsAuthenticated(result.ok);
        setIsChecking(false);
      }
    };

    validateSession();

    return () => {
      isMounted = false;
    };
  }, []);

  if (isChecking) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to={PATHS.LOGIN} replace />;
  }

  return <>{children}</>;
};
