export default function MotionScorePanel({ mode, score, roleMix = [] }) {
  const bars = roleMix;

  return (
    <aside className="motion-panel score-panel">
      <div className="score-orbit" style={{ "--score": `${Math.max(0, Math.min(100, Number(score) || 0))}%` }}>
        <span></span>
        <strong>{score}</strong>
        <small>{mode === "cv" ? "CV match" : "market fit"}</small>
      </div>
      <div className="role-bars">
        {!bars.length ? <p className="motion-status">Ni podatkov za razrez vlog.</p> : null}
        {bars.map(([label, value, color]) => (
          <div key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
            <i style={{ "--bar": `${Number(value)}%`, "--color": color }}></i>
          </div>
        ))}
      </div>
    </aside>
  );
}
