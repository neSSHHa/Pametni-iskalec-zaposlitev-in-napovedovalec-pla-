import apiClient from "./apiClient.js";

const AUTH_STORAGE_KEY = "jobradar-auth";
const PKCE_STORAGE_KEY = "jobradar-pkce";

const config = {
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "smartjobs",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "jobradar-frontend",
};

export function getStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || "null");
  } catch {
    return null;
  }
}

export function storeAuth(auth) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
  sessionStorage.removeItem(PKCE_STORAGE_KEY);
}

export function getAccessToken() {
  return getStoredAuth()?.accessToken || "";
}

export function isAdmin(user) {
  return Boolean(user?.roles?.some((role) => role.toUpperCase() === "ADMIN"));
}

export async function startKeycloakLogin(mode = "login") {
  const verifier = randomString(64);
  const challenge = await pkceChallenge(verifier);
  const redirectUri = `${window.location.origin}/auth/callback`;
  sessionStorage.setItem(PKCE_STORAGE_KEY, JSON.stringify({ verifier, redirectUri }));

  const path = mode === "register" ? "registrations" : "auth";
  const url = new URL(`${config.url}/realms/${config.realm}/protocol/openid-connect/${path}`);
  url.searchParams.set("client_id", config.clientId);
  url.searchParams.set("redirect_uri", redirectUri);
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", "openid profile email");
  url.searchParams.set("code_challenge", challenge);
  url.searchParams.set("code_challenge_method", "S256");
  window.location.href = url.toString();
}

export async function completeKeycloakLogin(code) {
  const stored = JSON.parse(sessionStorage.getItem(PKCE_STORAGE_KEY) || "{}");
  if (!stored.verifier || !stored.redirectUri) throw new Error("Missing login session");

  const body = new URLSearchParams();
  body.set("grant_type", "authorization_code");
  body.set("client_id", config.clientId);
  body.set("code", code);
  body.set("redirect_uri", stored.redirectUri);
  body.set("code_verifier", stored.verifier);

  const response = await fetch(`${config.url}/realms/${config.realm}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });

  if (!response.ok) throw new Error("Keycloak login failed");

  const token = await response.json();
  const claims = decodeJwt(token.access_token);
  const auth = {
    accessToken: token.access_token,
    idToken: token.id_token,
    refreshToken: token.refresh_token,
    user: {
      id: claims.sub,
      name: claims.name || claims.preferred_username || "User",
      email: claims.email || "",
      roles: claims.realm_access?.roles || [],
    },
  };

  storeAuth(auth);
  sessionStorage.removeItem(PKCE_STORAGE_KEY);
  return auth;
}

export async function logoutFromKeycloak() {
  const auth = getStoredAuth();
  clearAuth();
  if (auth?.refreshToken) {
    postKeycloakLogoutInBackground(auth.refreshToken);
    window.location.replace(`${window.location.origin}/motion`);
    return;
  }

  window.location.replace(`${window.location.origin}/motion`);
}

export async function getCurrentUser() {
  const response = await apiClient.get("/auth/me");
  return response.data;
}

export async function getAdminOverview() {
  const response = await apiClient.get("/admin/overview");
  return response.data;
}

export async function getAdminLogs(params = {}) {
  const response = await apiClient.get("/admin/logs", { params });
  return response.data;
}

export async function getAdminCacheStatus() {
  const response = await apiClient.get("/admin/cache/status");
  return response.data;
}

export async function refreshAdminCaches() {
  const response = await apiClient.post("/admin/cache/refresh");
  return response.data;
}

function randomString(length) {
  const values = new Uint8Array(length);
  crypto.getRandomValues(values);
  return Array.from(values, (value) => ("0" + value.toString(16)).slice(-2)).join("");
}

async function pkceChallenge(verifier) {
  const data = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return base64Url(new Uint8Array(digest));
}

function decodeJwt(token) {
  const payload = token.split(".")[1] || "";
  const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  return JSON.parse(atob(padded));
}

function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function postKeycloakLogoutInBackground(refreshToken) {
  const iframeName = "jobradar-keycloak-logout";
  const iframe = document.createElement("iframe");
  iframe.name = iframeName;
  iframe.style.display = "none";
  document.body.appendChild(iframe);

  const form = document.createElement("form");
  form.method = "POST";
  form.action = `${config.url}/realms/${config.realm}/protocol/openid-connect/logout`;
  form.target = iframeName;
  form.style.display = "none";

  const clientInput = document.createElement("input");
  clientInput.type = "hidden";
  clientInput.name = "client_id";
  clientInput.value = config.clientId;

  const refreshInput = document.createElement("input");
  refreshInput.type = "hidden";
  refreshInput.name = "refresh_token";
  refreshInput.value = refreshToken;

  form.append(clientInput, refreshInput);
  document.body.appendChild(form);
  form.submit();

  window.setTimeout(() => {
    form.remove();
    iframe.remove();
  }, 2000);
}
