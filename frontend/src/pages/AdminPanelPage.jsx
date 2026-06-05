import { ArrowLeft, BriefcaseBusiness, ShieldCheck, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";
import { getAdminOverview } from "../api/authApi.js";
import useAuth from "../hooks/useAuth.js";

export default function AdminPanelPage() {
  const auth = useAuth();
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!auth.authenticated || !auth.admin) return;
    getAdminOverview()
      .then(setOverview)
      .catch(() => setError("Admin overview could not be loaded."));
  }, [auth.authenticated, auth.admin]);

  if (!auth.authenticated) {
    return <AccessPanel title="Admin login required" text="Sign in with a Keycloak admin account." action={auth.login} actionText="Login" />;
  }

  if (!auth.admin) {
    return <AccessPanel title="Admin only" text="Your account does not have the ADMIN role in Keycloak." />;
  }

  return (
    <main className="admin-page">
      <button className="auth-back" type="button" onClick={goHome}>
        <ArrowLeft size={18} />
        Back to Job Radar
      </button>
      <section className="admin-hero">
        <ShieldCheck size={34} />
        <div>
          <span>Admin panel</span>
          <h1>Operational overview</h1>
          <p>Only users with the Keycloak ADMIN realm role can see this page.</p>
        </div>
      </section>

      {error ? <p className="motion-error">{error}</p> : null}
      <section className="admin-grid">
        <article>
          <UsersRound size={22} />
          <span>Users</span>
          <strong>{overview?.users ?? "..."}</strong>
        </article>
        <article>
          <BriefcaseBusiness size={22} />
          <span>Jobs</span>
          <strong>{overview?.jobs ?? "..."}</strong>
        </article>
        <article>
          <ShieldCheck size={22} />
          <span>Status</span>
          <strong>{overview?.status || "..."}</strong>
        </article>
      </section>
    </main>
  );
}

function AccessPanel({ title, text, action, actionText }) {
  return (
    <main className="auth-page">
      <section className="auth-panel">
        <img src="/jobradar.svg" alt="" aria-hidden="true" />
        <span>Admin panel</span>
        <h1>{title}</h1>
        <p>{text}</p>
        {action ? <button type="button" onClick={action}>{actionText}</button> : null}
      </section>
    </main>
  );
}
  const goHome = () => {
    window.history.pushState({}, "", "/motion");
    window.dispatchEvent(new Event("jobradar:navigate"));
  };
