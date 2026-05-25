export default function MotionCityEqualizer({ cities }) {
  const topCities = [...cities].sort((a, b) => b.jobs - a.jobs).slice(0, 7);
  const cityMax = Math.max(...topCities.map((city) => city.jobs));

  return (
    <section className="city-equalizer">
      {!topCities.length ? <p className="motion-status">No locations for the current selection.</p> : null}
      {topCities.map((city, index) => (
        <article key={`${city.country}-${city.name}`} style={{ animationDelay: `${index * 80}ms` }}>
          <span>{city.name}</span>
          <div>
            <i style={{ width: `${Math.max(18, (city.jobs / cityMax) * 100)}%` }}></i>
          </div>
          <strong>{city.jobs}</strong>
        </article>
      ))}
    </section>
  );
}
