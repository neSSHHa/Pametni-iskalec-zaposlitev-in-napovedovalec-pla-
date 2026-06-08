import axios from "axios";

const AUTH_STORAGE_KEY = "jobradar-auth";
const PKCE_STORAGE_KEY = "jobradar-pkce";
const KEYCLOAK_LOGOUT_REDIRECT_KEY = "jobradar-keycloak-logout-redirecting";
const AUTH_EXPIRED_EVENT = "jobradar:auth-expired";

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "smartjobs",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "jobradar-frontend",
};

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 420000,
});

export function createInteractionId() {
  return globalThis.crypto?.randomUUID?.() ||
    `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

apiClient.interceptors.request.use((config) => {
  config.headers["X-Request-ID"] = createInteractionId();
  const token = readAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearExpiredAuth();

      const originalRequest = error.config;
      if (originalRequest && shouldRetryWithoutAuth(originalRequest)) {
        originalRequest._retryWithoutAuth = true;
        delete originalRequest.headers?.Authorization;
        return apiClient(originalRequest);
      }
    }

    return Promise.reject(error);
  },
);

function readAccessToken() {
  try {
    const token = getStoredAuth()?.accessToken || "";
    if (!token) {
      sessionStorage.removeItem(KEYCLOAK_LOGOUT_REDIRECT_KEY);
      return "";
    }

    if (token && isJwtExpired(token)) {
      clearExpiredAuth();
      return "";
    }

    return token;
  } catch {
    return "";
  }
}

function isJwtExpired(token) {
  try {
    const payload = token.split(".")[1] || "";
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const claims = JSON.parse(atob(padded));
    return Boolean(claims.exp && Date.now() >= claims.exp * 1000);
  } catch {
    return true;
  }
}

function clearExpiredAuth() {
  const auth = getStoredAuth();
  localStorage.removeItem(AUTH_STORAGE_KEY);
  sessionStorage.removeItem(PKCE_STORAGE_KEY);
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
  redirectToKeycloakLogout(auth);
}

function shouldRetryWithoutAuth(request) {
  if (request._retryWithoutAuth) return false;
  const url = String(request.url || "");
  return !url.startsWith("/auth") && !url.startsWith("/admin");
}

function getStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || "null");
  } catch {
    return null;
  }
}

function redirectToKeycloakLogout(auth) {
  if (sessionStorage.getItem(KEYCLOAK_LOGOUT_REDIRECT_KEY) === "true") return;

  sessionStorage.setItem(KEYCLOAK_LOGOUT_REDIRECT_KEY, "true");
  const logoutUrl = new URL(`${keycloakConfig.url}/realms/${keycloakConfig.realm}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set("client_id", keycloakConfig.clientId);
  logoutUrl.searchParams.set("post_logout_redirect_uri", `${window.location.origin}/motion`);

  if (auth?.idToken) {
    logoutUrl.searchParams.set("id_token_hint", auth.idToken);
  }

  window.location.replace(logoutUrl.toString());
}

export default apiClient;
