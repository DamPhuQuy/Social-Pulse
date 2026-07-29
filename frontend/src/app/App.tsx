import { useEffect } from "react";
import { Toaster } from "@/shared/components/ui/sonner";
import { ProtectedRoute } from "@/app/router/ProtectedRoute";
import { Route, Routes } from "react-router-dom";
import "./styles/App.css";
import { routesConfig } from "./router/routesConfig";
import { AuthProvider } from "@/shared/hooks/useAuth";
import { RealTimeProvider } from "@/app/providers/RealTimeContext";

function App() {
  useEffect(() => {
    const isDark = localStorage.getItem('theme') === 'dark' || 
      (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches);
    
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, []);

  return (
    <AuthProvider>
      <RealTimeProvider>
        <Routes>
          {routesConfig.map((route: any) => {
            const Element: React.ComponentType = route.element;

            return (
              <Route
                key={route.path}
                path={route.path}
                element={
                  route.isPrivate ? (
                    <ProtectedRoute requiresAdmin={route.requiresAdmin}>
                      <Element />
                    </ProtectedRoute>
                  ) : (
                    <Element />
                  )
                }
              />
            );
          })}
        </Routes>
        <Toaster position="top-right" duration={3000} />
      </RealTimeProvider>
    </AuthProvider>
  );
}

export default App;
