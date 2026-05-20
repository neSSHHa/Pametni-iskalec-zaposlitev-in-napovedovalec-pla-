const navLinks = [
  ["Home", "/motion"],
  ["Prompt", "/motion-prompt"],
  ["CV", "/motion-cv"],
];

export default function MotionShell({ mode, score, tickerItems = [], children }) {
  const modeLabel = mode === "cv" ? "CV pulse active" : mode === "search" ? "Prompt pulse active" : "Prototype idle";

  return (
    <main className={`motion-shell mode-${mode}`}>
      <div className="motion-bg" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <aside className="motion-rail">
        <a className="motion-mark" href="/motion">
          <b>J</b>
          <span>JobPilot</span>
        </a>
        <nav aria-label="Motion pages">
          {navLinks.map(([label, href]) => (
            <a href={href} key={label}>{label}</a>
          ))}
        </nav>
        <div className="rail-meter">
          <span>Signal</span>
          <strong>{score}%</strong>
        </div>
      </aside>

      <section className="motion-stage">
        <header className="motion-topline">
          <div>
            <span>Motion Lab</span>
            <strong>{modeLabel}</strong>
          </div>
          <div className="ticker">
            {tickerItems.length ? tickerItems.map((item) => (
              <span key={item}>{item}</span>
            )) : <span>Backend data</span>}
          </div>
        </header>
        {children}
      </section>
    </main>
  );
}
