import { Toaster } from "@/components/ui/sonner";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { Route, Routes } from "react-router-dom";
import "./App.css";
import { routesConfig } from "./routes/routesConfig";

function App() {
  return (
    <>
      <Routes>
        {routesConfig.map((route) => {
          const Element: React.ComponentType = route.element;

          return (
            <Route
              key={route.path}
              path={route.path}
              element={
                route.isPrivate ? (
                  <ProtectedRoute>
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
      <Toaster position="top-right" richColors />
    </>
  );
}

export default App;
