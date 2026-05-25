import EuropeJobsMap from "./EuropeJobsMap.jsx";

function insightList(items = [], limit = 4) {
  return items.slice(0, limit).map((item) => `${item.label} (${item.count})`).join(", ") || "No data";
}

export default function MotionMapSection({ countries = [], cities = [], analytics }) {
  const filteredLabel = analytics?.isFiltered ? "current selection" : "the whole database";
  const remoteCount = analytics?.summary?.remoteJobs ?? 0;
  const hybridCount = analytics?.summary?.hybridJobs ?? 0;
  const insights = [
    {
      label: "Top cities",
      value: analytics?.cityStats?.[0]?.label || "No data",
      detail: insightList(analytics?.cityStats),
    },
    {
      label: "Regions",
      value: analytics?.regionStats?.[0]?.label || "No data",
      detail: insightList(analytics?.regionStats),
    },
    {
      label: "Countries",
      value: analytics?.countryStats?.[0]?.label || "No data",
      detail: insightList(analytics?.countryStats),
    },
    {
      label: "Flexible work",
      value: String(remoteCount + hybridCount),
      detail: `${remoteCount} remote, ${hybridCount} hybrid in ${filteredLabel}.`,
    },
    {
      label: "Experience level",
      value: analytics?.experienceLevelStats?.[0]?.label || "No data",
      detail: insightList(analytics?.experienceLevelStats),
    },
  ];

  return (
    <section className="motion-map-panel" id="map">
      <div className="panel-head">
        <div>
          <span>Market radar</span>
          <h2>Jobs by cities and regions.</h2>
        </div>
        <p>{analytics?.isFiltered ? "The map and statistics below show only the current CV/prompt results." : "Locations come from the analytics API and show where active listings are concentrated."}</p>
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
