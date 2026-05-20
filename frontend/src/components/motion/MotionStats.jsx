export default function MotionStats({ cards = [] }) {
  return (
    <section className="motion-stats">
      {cards.map((card, index) => (
        <article className={`tone-${card.tone}`} key={card.title} style={{ animationDelay: `${index * 110}ms` }}>
          <span>{card.title}</span>
          <strong>{card.value}</strong>
          <p>{card.detail}</p>
        </article>
      ))}
    </section>
  );
}
