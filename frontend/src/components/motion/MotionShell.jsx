import { Scale, SunMoon } from "lucide-react";

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
          <b className="logo-mark" aria-hidden="true"></b>
          <span>Job Radar</span>
        </a>
        <nav aria-label="Motion pages">
          {navLinks.map(([label, href]) => (
            <a
              className={(href === "/analytics" ? path === "/analytics" : mode === "idle" && path !== "/analytics") ? "active" : ""}
              href={href}
              key={label}
              onClick={(event) => {
                if (href === "/motion" && onHomeClick) {
                  event.preventDefault();
                  onHomeClick();
                  return;
                }

                if (href === "/analytics" && mode !== "idle" && onStatisticsClick) {
                  event.preventDefault();
                  onStatisticsClick();
                  return;
                }

                navigate(event, href);
              }}
            >
              {label}
            </a>
          ))}
          {mode !== "idle" ? <a className="active" href="/motion-prompt" onClick={(event) => event.preventDefault()}>Jobs</a> : null}
        </nav>
        <div className="topbar-actions">
          <button className="theme-toggle" type="button" onClick={onThemeToggle} aria-label="Toggle dark or light mode">
            <SunMoon size={18} strokeWidth={1.9} />
            <span>{theme === "light" ? "Light" : "Dark"}</span>
          </button>
          <button className="compare-button" type="button" aria-label="Compare jobs">
            <Scale size={20} strokeWidth={1.8} />
            <span>Compare jobs (0)</span>
          </button>
          <div className="rail-meter">
            <span>Avg match</span>
            <strong>{score}%</strong>
          </div>
        </div>
      </header>

      <section className="motion-stage">
        {children}
      </section>
    </main>
  );
}
