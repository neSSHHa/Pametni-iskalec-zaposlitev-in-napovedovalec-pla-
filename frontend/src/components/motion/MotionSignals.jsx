export default function MotionSignals({ signals = [] }) {
  return (
    <section className="signal-strip" aria-label="Live signals">
      {signals.map(([value, label, detail], index) => (
        <article key={label} style={{ animationDelay: `${index * 90}ms` }}>
          <strong>{value}</strong>
          <span>{label}</span>
          <p>{detail}</p>
        </article>
      ))}
    </section>
  );
}
