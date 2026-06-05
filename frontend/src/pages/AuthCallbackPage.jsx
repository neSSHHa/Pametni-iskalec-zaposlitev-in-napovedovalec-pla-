import { useEffect, useState } from "react";
import { completeKeycloakLogin } from "../api/authApi.js";
import useAuth from "../hooks/useAuth.js";

export default function AuthCallbackPage() {
  const auth = useAuth();
  const [error, setError] = useState("");

  useEffect(() => {
    const code = new URLSearchParams(window.location.search).get("code");
    if (!code) {
      setError("Missing Keycloak authorization code.");
      return;
    }

    completeKeycloakLogin(code)
      .then((nextAuth) => {
        auth.setAuth(nextAuth);
        window.history.replaceState({}, "", "/motion");
        window.dispatchEvent(new Event("jobradar:navigate"));
      })
      .catch(() => setError("Keycloak login could not be completed."));
  }, []);

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <img src="/jobradar.svg" alt="" aria-hidden="true" />
        <span>Keycloak</span>
        <h1>{error ? "Login failed" : "Signing you in..."}</h1>
        <p>{error || "Please wait while Job Radar completes your session."}</p>
      </section>
    </main>
  );
}
