const navLinks = [
  ["Discover", "/motion"],
  ["Analytics", "/analytics"],
];

function navigate(event, href) {
  event.preventDefault();
  window.history.pushState({}, "", href);
  window.dispatchEvent(new Event("jobradar:navigate"));
}

export default function MotionShell({ mode, score, theme, onThemeToggle, children }) {
  return (
    <main className={`motion-shell mode-${mode} theme-${theme}`}>
      <div className="motion-bg" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <aside className="motion-rail">
        <a className="motion-mark" href="/motion" onClick={(event) => navigate(event, "/motion")}>
          <b className="logo-glyph" aria-hidden="true">
            <i></i>
          </b>
          <span><em>Job</em>Radar</span>
        </a>
        <nav aria-label="Motion pages">
          {navLinks.map(([label, href]) => (
            <a href={href} key={label} onClick={(event) => navigate(event, href)}>{label}</a>
          ))}
        </nav>
        <button className="theme-toggle" type="button" onClick={onThemeToggle}>
          <span>{theme === "light" ? "Light" : "Dark"}</span>
          <i aria-hidden="true"></i>
        </button>
        <div className="rail-meter">
          <span>Avg match</span>
          <strong>{score}%</strong>
        </div>
      </aside>

      <section className="motion-stage">
        {children}
      </section>
    </main>
  );
}
