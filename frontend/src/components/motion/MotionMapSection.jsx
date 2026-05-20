import EuropeJobsMap from "./EuropeJobsMap.jsx";

function insightList(items = [], limit = 4) {
  return items.slice(0, limit).map((item) => `${item.label} (${item.count})`).join(", ") || "Ni podatkov";
}

export default function MotionMapSection({ countries = [], cities = [], analytics }) {
  const filteredLabel = analytics?.isFiltered ? "trenutni izbor" : "vsa baza";
  const remoteCount = analytics?.summary?.remoteJobs ?? 0;
  const hybridCount = analytics?.summary?.hybridJobs ?? 0;
  const insights = [
    {
      label: "Top mesta",
      value: analytics?.cityStats?.[0]?.label || "Ni podatkov",
      detail: insightList(analytics?.cityStats),
    },
    {
      label: "Regije",
      value: analytics?.regionStats?.[0]?.label || "Ni podatkov",
      detail: insightList(analytics?.regionStats),
    },
    {
      label: "Drzave",
      value: analytics?.countryStats?.[0]?.label || "Ni podatkov",
      detail: insightList(analytics?.countryStats),
    },
    {
      label: "Fleksibilno delo",
      value: String(remoteCount + hybridCount),
      detail: `${remoteCount} remote, ${hybridCount} hybrid v ${filteredLabel}.`,
    },
    {
      label: "Raven izkusenj",
      value: analytics?.experienceLevelStats?.[0]?.label || "Ni podatkov",
      detail: insightList(analytics?.experienceLevelStats),
    },
  ];

  return (
    <section className="motion-map-panel" id="map">
      <div className="panel-head">
        <div>
          <span>Market radar</span>
          <h2>Delovna mesta po mestih/regijah.</h2>
        </div>
        <p>{analytics?.isFiltered ? "Mapa in statistika spodaj prikazujeta samo rezultate trenutnega CV/prompt filtra." : "Lokacije prihajajo iz analytics API-ja in pokazejo, kje je najvec aktivnih oglasov."}</p>
      </div>
      <EuropeJobsMap countries={countries} cities={cities} />
      <div className="map-insights">
        {insights.map((insight, index) => (
          <article key={insight.label} style={{ animationDelay: `${index * 90}ms` }}>
            <span>{insight.label}</span>
            <strong>{insight.value}</strong>
            <p>{insight.detail}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
