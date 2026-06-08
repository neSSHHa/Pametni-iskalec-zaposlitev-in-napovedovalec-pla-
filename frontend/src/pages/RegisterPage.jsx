import { ArrowLeft, LogIn, UserPlus } from "lucide-react";
import { useEffect } from "react";
import useAuth from "../hooks/useAuth.js";

export default function RegisterPage() {
  const auth = useAuth();

  useEffect(() => {
    auth.register();
  }, []);

  const goHome = () => {
    window.history.pushState({}, "", "/motion");
    window.dispatchEvent(new Event("jobradar:navigate"));
  };

  return (
    <main className="auth-page">
      <button className="auth-back" type="button" onClick={goHome}>
        <ArrowLeft size={18} />
        Back to Job Radar
      </button>
      <section className="auth-panel">
        <img src="/jobradar.svg" alt="" aria-hidden="true" />
        <span>New account</span>
        <h1>Create an account</h1>
        <p>Redirecting you to the secure Job Radar registration page.</p>
        <button type="button" onClick={auth.register}>
          <UserPlus size={19} />
          Register with Keycloak
        </button>
        <button className="auth-secondary" type="button" onClick={auth.login}>
          <LogIn size={18} />
          I already have an account
        </button>
      </section>
    </main>
  );
}
