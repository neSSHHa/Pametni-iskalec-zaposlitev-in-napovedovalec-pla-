import { useEffect, useMemo, useState } from "react";
import { getAnalyticsDashboard } from "../api/analyticsApi.js";
import { uploadCv } from "../api/cvApi.js";
import { getJobs, searchJobsByPrompt } from "../api/jobApi.js";
import MotionCityEqualizer from "../components/motion/MotionCityEqualizer.jsx";
import MotionHero from "../components/motion/MotionHero.jsx";
import MotionJobsPanel from "../components/motion/MotionJobsPanel.jsx";
import MotionMapSection from "../components/motion/MotionMapSection.jsx";
import MotionScorePanel from "../components/motion/MotionScorePanel.jsx";
import MotionShell from "../components/motion/MotionShell.jsx";
import MotionSignals from "../components/motion/MotionSignals.jsx";
import MotionStats from "../components/motion/MotionStats.jsx";

const fallbackQuery = "";
const countryCodes = {
  slovenia: "SI",
  slovenija: "SI",
  germany: "DE",
  nemcija: "DE",
  austria: "AT",
  avstrija: "AT",
  croatia: "HR",
  hrvaska: "HR",
  serbia: "RS",
  srbija: "RS",
  italy: "IT",
  italija: "IT",
  france: "FR",
  francija: "FR",
  netherlands: "NL",
  spain: "ES",
  switzerland: "CH",
  sweden: "SE",
  "united kingdom": "GB",
  bulgaria: "BG",
  bolgarija: "BG",
};

const locationCoordinates = {
  ljubljana: { lat: 46.0569, lng: 14.5058 },
  maribor: { lat: 46.5547, lng: 15.6459 },
  celje: { lat: 46.2397, lng: 15.2677 },
  koper: { lat: 45.5481, lng: 13.7302 },
  "murska sobota": { lat: 46.6625, lng: 16.1664 },
  "novo mesto": { lat: 45.8011, lng: 15.171 },
  sofia: { lat: 42.6977, lng: 23.3219 },
};

function countryCode(value, fallback) {
  const normalized = String(value || "").toLowerCase().trim();
  return countryCodes[normalized] || String(fallback || value || "EU").toUpperCase().slice(0, 2);
}

function formatSalary(job) {
  if (!job?.salaryMin && !job?.salaryMax) return "Salary n/a";
  const min = job.salaryMin ? `${Number(job.salaryMin).toLocaleString("sl-SI")} EUR` : "";
  const max = job.salaryMax ? `${Number(job.salaryMax).toLocaleString("sl-SI")} EUR` : "";
  return [min, max].filter(Boolean).join(" - ");
}

function mapJob(job, index) {
  const parts = String(job.location || "").split(",").map((part) => part.trim()).filter(Boolean);
  const city = parts[0] || "Unknown";
  const region = parts.length > 2 ? parts[1] : "";
  const country = parts.at(-1) || "";

  return {
    id: job.id || `${job.title}-${index}`,
    title: job.title || "Odprta vloga",
    company: job.companyName || "Podjetje ni navedeno",
    city,
    region,
    country,
    countryCode: countryCode(country, region || "EU"),
    salary: formatSalary(job),
    sourceUrl: job.sourceUrl || "",
    description: job.description || "",
    educationLevel: job.educationLevel || "Ni navedeno",
    postedDate: job.postedDate || "",
    matchLevel: job.matchLevel || "",
    match: job.matchScore || Math.max(72, 96 - index * 3),
    level: job.experienceLevel || "Ni navedeno",
    mode: job.workMode || "Ni navedeno",
    tags: Array.isArray(job.skills) && job.skills.length ? job.skills.slice(0, 4) : ["AI match", "Skill fit"],
  };
}

function statValue(list, fallback = "Ni podatkov") {
  return list?.[0]?.label || fallback;
}

function percentage(count, total) {
  if (!total) return 0;
  return Number(((count / total) * 100).toFixed(1));
}

function countItems(items, getter) {
  const total = items.length;
  const counts = items.reduce((acc, item) => {
    const values = getter(item);
    const list = Array.isArray(values) ? values : [values];

    list.filter(Boolean).forEach((value) => {
      const label = String(value).trim();
      if (!label || label === "Ni navedeno" || label === "Unknown") return;
      acc.set(label, (acc.get(label) || 0) + 1);
    });

    return acc;
  }, new Map());

  return [...counts.entries()]
    .map(([label, count]) => ({ label, count, percentage: percentage(count, total) }))
    .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
}

function classifyRole(title = "") {
  const normalized = title.toLowerCase();
  if (/(developer|engineer|razvijalec|java|react|frontend|backend|software|devops|database)/.test(normalized)) return "Razvijalci / inzenirji";
  if (/(designer|grafi|oblikoval)/.test(normalized)) return "Oblikovanje";
  if (/(kuhar|natakar|hrane|food)/.test(normalized)) return "Gostinstvo in kuhinja";
  if (/(terapevt|zdrav|medic|nega|nurs|bolni)/.test(normalized)) return "Zdravstvo in nega";
  if (/(racun|account|tax|dav)/.test(normalized)) return "Racunovodstvo in finance";
  if (/(prodaj|sales|consultant|svetovalec)/.test(normalized)) return "Prodaja in storitve";
  if (/(skladisc|voznik|logistik|warehouse)/.test(normalized)) return "Logistika in transport";
  if (/(ucitelj|teaching|sola)/.test(normalized)) return "Izobrazevanje";
  return "Druge vloge";
}

function buildFilteredAnalytics(filteredJobs) {
  const totalJobs = filteredJobs.length;
  const cityStats = countItems(filteredJobs, (job) => job.city).map((item) => {
    const sample = filteredJobs.find((job) => job.city === item.label);
    const coords = locationCoordinates[item.label.toLowerCase()] || {};
    return {
      ...item,
      city: item.label,
      region: sample?.region || null,
      country: sample?.country || null,
      latitude: coords.lat ?? null,
      longitude: coords.lng ?? null,
    };
  });
  const countryStats = countItems(filteredJobs, (job) => job.country).map((item) => {
    const sample = filteredJobs.find((job) => job.country === item.label);
    const sampleCity = sample ? locationCoordinates[sample.city.toLowerCase()] : null;
    return {
      ...item,
      country: item.label,
      latitude: sampleCity?.lat ?? null,
      longitude: sampleCity?.lng ?? null,
    };
  });
  const remoteJobs = filteredJobs.filter((job) => job.mode.toLowerCase().includes("remote")).length;
  const hybridJobs = filteredJobs.filter((job) => job.mode.toLowerCase().includes("hybrid")).length;
  const averageMatch = totalJobs
    ? Math.round(filteredJobs.reduce((sum, job) => sum + Number(job.match || 0), 0) / totalJobs)
    : 0;

  return {
    isFiltered: true,
    summary: {
      totalJobs,
      totalCompanies: new Set(filteredJobs.map((job) => job.company).filter(Boolean)).size,
      totalLocations: cityStats.length,
      totalCountries: countryStats.length,
      remoteJobs,
      hybridJobs,
      averageMatch,
    },
    topSkills: countItems(filteredJobs, (job) => job.tags),
    topRoles: countItems(filteredJobs, (job) => classifyRole(job.title)),
    cityStats,
    regionStats: countItems(filteredJobs, (job) => job.region),
    countryStats,
    experienceLevelStats: countItems(filteredJobs, (job) => job.level),
    workTypeStats: countItems(filteredJobs, (job) => job.mode.split(",").map((value) => value.trim())),
  };
}

function mapAnalyticsToSignals(analytics, jobsCount) {
  const summary = analytics?.summary;
  const totalJobs = summary?.totalJobs ?? jobsCount ?? 0;
  const averageMatch = summary?.averageMatch;
  const remoteJobs = summary?.remoteJobs ?? 0;
  const hybridJobs = summary?.hybridJobs ?? analytics?.workTypeStats?.find((item) => item.label.toLowerCase().includes("hybrid"))?.count ?? 0;

  if (!analytics?.isFiltered) {
    return [
      [String(totalJobs), "aktivnih oglasov", "Celotna baza delovnih mest"],
      [statValue(analytics?.topSkills), "top vescina", "Najpogostejsa zahteva v oglasih"],
      [statValue(analytics?.topRoles), "najbolj iskana vloga", "Trenutni trg po kategorijah"],
      [statValue(analytics?.cityStats), "najmocnejsa lokacija", `${summary?.totalLocations ?? analytics?.cityStats?.length ?? 0} lokacij v bazi`],
    ];
  }

  return [
    [String(totalJobs), "ujemajocih vlog", analytics?.isFiltered ? "Za trenutni CV/prompt" : "Aktivni oglasi v bazi"],
    [averageMatch ? `${averageMatch}%` : statValue(analytics?.topSkills), averageMatch ? "povprecno ujemanje" : "top vescina", averageMatch ? "Povprecen match score rezultatov" : "Najpogostejsa zahteva v oglasih"],
    [statValue(analytics?.cityStats), "najmocnejsa lokacija", `${summary?.totalLocations ?? analytics?.cityStats?.length ?? 0} lokacij v izboru`],
    [String(remoteJobs + hybridJobs), "remote/hybrid", "Fleksibilne vloge v izboru"],
  ];
}

function mapAnalyticsToStats(analytics) {
  return [
    {
      title: "Najbolj zazelene vescine",
      value: statValue(analytics?.topSkills),
      detail: analytics?.topSkills?.slice(0, 4).map((item) => item.label).join(", ") || "Podatki se napolnijo iz backend analytics API-ja.",
      tone: "cyan",
    },
    {
      title: "Najbolj iskane vloge",
      value: statValue(analytics?.topRoles),
      detail: analytics?.topRoles?.slice(0, 4).map((item) => `${item.label} (${item.count})`).join(", ") || "Razvijalci, dizajnerji, kuharji, terapevti in druge vloge.",
      tone: "pink",
    },
    {
      title: "Delovna mesta po mestih/regijah",
      value: statValue(analytics?.cityStats),
      detail: analytics?.regionStats?.slice(0, 4).map((item) => `${item.label} (${item.count})`).join(", ") || "Pregled mest in regij iz lokacij v oglasih.",
      tone: "lime",
    },
    {
      title: "Raven izkusenj",
      value: statValue(analytics?.experienceLevelStats),
      detail: analytics?.experienceLevelStats?.slice(0, 4).map((item) => `${item.label} ${item.percentage}%`).join(", ") || "Distribucija oglasov po zahtevani senioriteti.",
      tone: "amber",
    },
    {
      title: "Tip dela",
      value: statValue(analytics?.workTypeStats),
      detail: analytics?.workTypeStats?.slice(0, 4).map((item) => `${item.label} (${item.count})`).join(", ") || "Remote, hybrid in on-site signal iz baze.",
      tone: "cyan",
    },
    {
      title: "Podjetja v izboru",
      value: String(analytics?.summary?.totalCompanies ?? "0"),
      detail: analytics?.isFiltered ? "Stevilo podjetij samo med filtriranimi rezultati." : "Stevilo podjetij v trenutno nalozenih oglasih.",
      tone: "pink",
    },
  ];
}

function mapRoleMix(analytics) {
  const colors = ["#69f5ff", "#ff6fb7", "#ffd166", "#8ef0a7", "#a78bfa"];
  const roles = analytics?.topRoles?.length ? analytics.topRoles : [];
  const max = Math.max(...roles.map((role) => role.count), 1);
  return roles.slice(0, 5).map((role, index) => [
    role.label,
    Math.max(6, Math.round((role.count / max) * 100)),
    colors[index % colors.length],
  ]);
}

function mapLocations(analytics) {
  const cityStats = analytics?.cityStats?.length ? analytics.cityStats : [];
  const countryStats = analytics?.countryStats?.length ? analytics.countryStats : [];
  const colors = ["#69f5ff", "#70d6ff", "#ff6fb7", "#ffd166", "#8ef0a7", "#a78bfa"];

  const countries = countryStats.slice(0, 8).map((country, index) => ({
    code: countryCode(country.country || country.label, `C${index}`),
    mapId: countryCode(country.country || country.label, `C${index}`),
    name: country.label,
    jobs: country.count,
    lat: Number(country.latitude ?? 46 + index),
    lng: Number(country.longitude ?? 14 + index),
    color: colors[index % colors.length],
  }));

  const cities = cityStats.slice(0, 12).map((city, index) => ({
    name: city.city || city.label,
    jobs: city.count,
    country: countryCode(city.country, city.region || "EU"),
    countryName: city.country || city.region || "Europe",
    lat: Number(city.latitude ?? 46 + index / 3),
    lng: Number(city.longitude ?? 14 + index / 3),
    color: colors[index % colors.length],
  }));

  return {
    countries,
    cities,
  };
}

function listFromApiResponse(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.jobs)) return data.jobs;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}

export default function MotionExperience({ initialMode = "idle", resultPage = false }) {
  const [mode, setMode] = useState(initialMode);
  const [query, setQuery] = useState(fallbackQuery);
  const [cvName, setCvName] = useState("");
  const [jobs, setJobs] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");

  const activeMode = resultPage && initialMode !== "idle" ? initialMode : mode;
  const score = jobs.length ? Math.round(jobs.reduce((sum, job) => sum + job.match, 0) / jobs.length) : 0;
  const signals = useMemo(() => mapAnalyticsToSignals(analytics, jobs.length), [analytics, jobs.length]);
  const statCards = useMemo(() => mapAnalyticsToStats(analytics), [analytics]);
  const roleMix = useMemo(() => mapRoleMix(analytics), [analytics]);
  const { countries, cities } = useMemo(() => mapLocations(analytics), [analytics]);
  const tickerItems = useMemo(() => cities.slice(0, 5).map((city) => `${city.name} ${city.jobs}`), [cities]);

  useEffect(() => {
    let cancelled = false;

    async function loadInitialData() {
      try {
        setStatus("Nalaganje podatkov iz backend API-ja...");
        const [jobsData, analyticsData] = await Promise.all([getJobs(), getAnalyticsDashboard(10)]);
        if (cancelled) return;
        setJobs(listFromApiResponse(jobsData).map(mapJob));
        setAnalytics({ ...analyticsData, isFiltered: false });
        setStatus("");
      } catch (err) {
        if (cancelled) return;
        setError("Backend trenutno ni dosegljiv. UI je nalozen, podatki se prikazejo ko se API zazene.");
        setStatus("");
      }
    }

    loadInitialData();
    return () => {
      cancelled = true;
    };
  }, []);

  const submitPrompt = async (event) => {
    event.preventDefault();
    if (!query.trim()) {
      setError("Vnesi prompt ali nalozi CV za filtriranje oglasov.");
      return;
    }
    setMode("search");
    setLoading(true);
    setError("");
    setStatus("AI bere prompt in filtrira oglase...");

    try {
      const data = await searchJobsByPrompt(query);
      const mappedJobs = listFromApiResponse(data).map(mapJob);
      setJobs(mappedJobs);
      setAnalytics(buildFilteredAnalytics(mappedJobs));
      window.history.pushState({}, "", "/motion-prompt");
    } catch (err) {
      setError("Prompt API nije vratio rezultat. Proveri backend/AI servis.");
    } finally {
      setLoading(false);
      setStatus("");
    }
  };

  const handleCvUpload = async (file) => {
    if (!file) return;
    setCvName(file.name);
    setMode("cv");
    setLoading(true);
    setError("");
    setStatus("CV se cita i povezuje sa oglasima...");

    try {
      const data = await uploadCv(file);
      const mappedJobs = (data?.jobs || []).map(mapJob);
      setJobs(mappedJobs);
      setAnalytics(buildFilteredAnalytics(mappedJobs));
      window.history.pushState({}, "", "/motion-cv");
    } catch (err) {
      setError("CV API nije vratio rezultat. Proveri da li backend i AI servis rade.");
    } finally {
      setLoading(false);
      setStatus("");
    }
  };

  return (
    <MotionShell mode={activeMode} score={score} tickerItems={tickerItems}>
      {loading ? <MotionLoadingOverlay mode={activeMode} status={status} /> : null}
      <MotionHero
        mode={activeMode}
        score={score}
        query={query}
        cvName={cvName}
        resultPage={false}
        loading={loading}
        status={status}
        error={error}
        onQueryChange={setQuery}
        onPromptSubmit={submitPrompt}
        onCvUpload={handleCvUpload}
      />
      <MotionSignals signals={signals} />
      <section className="motion-grid">
        <MotionJobsPanel mode={activeMode} score={score} jobs={jobs} loading={loading} error={error} />
        <MotionScorePanel mode={activeMode} score={score} roleMix={roleMix} />
      </section>
      <MotionMapSection countries={countries} cities={cities} analytics={analytics} />
      <MotionCityEqualizer cities={cities} />
      <MotionStats cards={statCards} />
    </MotionShell>
  );
}

function MotionLoadingOverlay({ mode, status }) {
  const steps = mode === "cv"
    ? ["Berem CV", "Izvlacim vescine", "Racunam ujemanje", "Sestavljam analitiko"]
    : ["Berem prompt", "Prepoznavam namero", "Filtriram oglase", "Osvezujem analitiko"];

  return (
    <div className="motion-loading-overlay" role="status" aria-live="polite">
      <div className="loading-orbit">
        <span></span>
        <span></span>
        <span></span>
        <b>{mode === "cv" ? "CV" : "AI"}</b>
      </div>
      <div className="loading-copy">
        <strong>{status || "Pripravljam rezultate..."}</strong>
        <p>To lahko traja nekaj trenutkov, ker lokalni AI sestavlja filter in rangira oglase.</p>
        <div>
          {steps.map((step, index) => (
            <em key={step} style={{ animationDelay: `${index * 180}ms` }}>{step}</em>
          ))}
        </div>
      </div>
    </div>
  );
}
