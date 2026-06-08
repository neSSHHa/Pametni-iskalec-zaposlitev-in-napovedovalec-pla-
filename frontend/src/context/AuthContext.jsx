import { createContext, useContext, useMemo, useState } from "react";
import { clearAuth, getStoredAuth, isAdmin, logoutFromKeycloak, startKeycloakLogin } from "../api/authApi.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => getStoredAuth());
  const user = auth?.user || null;

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
