import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { clearAuth, getStoredAuth, isAdmin, logoutFromKeycloak, startKeycloakLogin } from "../api/authApi.js";

const AuthContext = createContext(null);
const AUTH_EXPIRED_EVENT = "jobradar:auth-expired";

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => getStoredAuth());
  const user = auth?.user || null;

  useEffect(() => {
    const handleExpiredAuth = () => {
      clearAuth();
      setAuth(null);
    };

    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpiredAuth);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpiredAuth);
  }, []);

  const value = useMemo(() => ({
    auth,
    user,
    authenticated: Boolean(user),
    admin: isAdmin(user),
    login: () => startKeycloakLogin("login"),
    register: () => startKeycloakLogin("register"),
    logout: () => {
      setAuth(null);
      logoutFromKeycloak();
    },
    setAuth,
  }), [auth, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthContext() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuthContext must be used within AuthProvider");
  return value;
}
