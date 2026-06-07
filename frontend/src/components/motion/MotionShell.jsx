import { Menu, Scale, SunMoon, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useComparison } from "../../context/ComparisonContext.jsx";
import useAuth from "../../hooks/useAuth.js";

const navLinks = [
  ["Home", "/motion"],
  ["Statistics", "/analytics"],
];

function navigate(event, href) {
  event.preventDefault();
  window.history.pushState({}, "", href);
  window.dispatchEvent(new Event("jobradar:navigate"));
}

export default function MotionShell({ mode, score, theme, onThemeToggle, onHomeClick, onStatisticsClick, children }) {
  const path = window.location.pathname;
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const comparison = useComparison();
  const auth = useAuth();
  const adminActive = path === "/admin" || mode === "admin";
  const jobsActive = !adminActive && (
    ["/motion-prompt", "/motion-cv"].includes(path) ||
    (mode !== "idle" && mode !== "analytics" && mode !== "compare" && path !== "/compare")
  );

  useEffect(() => {
    setMobileNavOpen(false);
  }, [path, mode]);

  const openComparison = () => {
    window.history.pushState({}, "", "/compare");
    window.dispatchEvent(new Event("jobradar:navigate"));
    setMobileNavOpen(false);
  };

  const goTo = (href) => {
    window.history.pushState({}, "", href);
    window.dispatchEvent(new Event("jobradar:navigate"));
    setMobileNavOpen(false);
  };

  return (
    <main className={`motion-shell mode-${mode} theme-${theme}`}>
      <div className="motion-bg" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <header className="jr-topbar">
        <a className="motion-mark" href="/motion" onClick={(event) => {
          event.preventDefault();
          onHomeClick?.();
        }}>
          <img className="logo-mark" src="/jobradar.svg" alt="" aria-hidden="true" />
          <span>Job Radar</span>
        </a>
        <button
          className="mobile-nav-toggle"
          type="button"
          aria-label={mobileNavOpen ? "Close navigation" : "Open navigation"}
          aria-expanded={mobileNavOpen}
          aria-controls="jobradar-navigation"
          onClick={() => setMobileNavOpen((open) => !open)}
        >
          {mobileNavOpen ? <X size={22} strokeWidth={1.9} /> : <Menu size={22} strokeWidth={1.9} />}
        </button>
        <nav id="jobradar-navigation" className={mobileNavOpen ? "open" : ""} aria-label="Motion pages">
          {navLinks.map(([label, href]) => (
            <a
              className={(href === "/analytics" ? path === "/analytics" : path === "/motion" && mode === "idle") ? "active" : ""}
              href={href}
              key={label}
              onClick={(event) => {
                if (href === "/motion" && onHomeClick) {
                  event.preventDefault();
                  onHomeClick();
                  setMobileNavOpen(false);
                  return;
                }

                if (href === "/analytics" && mode !== "idle" && onStatisticsClick) {
                  event.preventDefault();
                  onStatisticsClick();
                  setMobileNavOpen(false);
                  return;
                }

                navigate(event, href);
                setMobileNavOpen(false);
              }}
            >
              {label}
            </a>
          ))}
          {jobsActive ? <a className="active" href="/motion-prompt" onClick={(event) => event.preventDefault()}>Jobs</a> : null}
          {auth.admin ? <button className={adminActive ? "mobile-nav-compare-link active" : "mobile-nav-compare-link"} type="button" onClick={() => goTo("/admin")}>Admin panel</button> : null}
          {!auth.authenticated ? (
            <>
              <button className="mobile-nav-compare-link" type="button" onClick={auth.login}>Login</button>
              <button className="mobile-nav-compare-link" type="button" onClick={auth.register}>Register</button>
            </>
          ) : (
            <button className="mobile-nav-compare-link" type="button" onClick={auth.logout}>Logout</button>
          )}
          <button className="mobile-nav-compare-link" type="button" onClick={openComparison}>
            Compare jobs ({comparison.count}/{comparison.maxJobs})
          </button>
        </nav>
        <div className="topbar-actions">
          <button className="theme-toggle" type="button" onClick={onThemeToggle} aria-label="Toggle dark or light mode">
            <SunMoon size={18} strokeWidth={1.9} />
            <span>{theme === "light" ? "Light" : "Dark"}</span>
          </button>
          <button
            className={`compare-button ${path === "/compare" ? "active" : ""}`}
            type="button"
            aria-label={`Compare jobs ${comparison.count} of ${comparison.maxJobs}`}
            onClick={openComparison}
          >
            <Scale size={20} strokeWidth={1.8} />
            <span>Compare jobs ({comparison.count}/{comparison.maxJobs})</span>
            <b className="compare-count-badge" aria-hidden="true">{comparison.count}/{comparison.maxJobs}</b>
          </button>
          <div className="rail-meter">
            <span>Avg match</span>
            <strong>{score}%</strong>
          </div>
          {auth.admin ? <button className={adminActive ? "topbar-text-action active" : "topbar-text-action"} type="button" onClick={() => goTo("/admin")}>Admin</button> : null}
          {!auth.authenticated ? (
            <button className="topbar-text-action" type="button" onClick={auth.login}>Login</button>
          ) : (
            <button className="topbar-text-action" type="button" onClick={auth.logout}>Logout</button>
          )}
        </div>
      </header>

      <section className="motion-stage">
        {children}
      </section>
    </main>
  );
}
