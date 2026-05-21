import { useEffect } from "react";
import { Toaster } from "@/components/ui/sonner";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { Route, Routes } from "react-router-dom";
import "./App.css";
import { routesConfig } from "./routes/routesConfig";
import { AuthProvider } from "@/hooks/useAuth";
import { RealTimeProvider } from "@/context/RealTimeContext";

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
          {routesConfig.map((route) => {
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
