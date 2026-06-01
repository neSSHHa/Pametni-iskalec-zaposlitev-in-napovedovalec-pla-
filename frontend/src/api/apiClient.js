import axios from "axios";

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
  return config;
});

export default apiClient;
