import { ArrowLeft, LogIn, UserPlus } from "lucide-react";
import { useEffect } from "react";
import useAuth from "../hooks/useAuth.js";

export default function LoginPage() {
  const auth = useAuth();

  useEffect(() => {
    auth.login();
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
        <span>Secure account</span>
        <h1>Sign in to Job Radar</h1>
        <p>Redirecting you to the secure Job Radar sign in page.</p>
        <button type="button" onClick={auth.login}>
          <LogIn size={19} />
          Continue with Keycloak
        </button>
        <button className="auth-secondary" type="button" onClick={auth.register}>
          <UserPlus size={18} />
          Create account
        </button>
      </section>
    </main>
  );
}
