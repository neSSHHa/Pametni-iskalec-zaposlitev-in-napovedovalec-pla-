import { useEffect, useMemo, useState } from "react";
import { BarChart3, Brain, Database, FileText, Search, UserRound } from "lucide-react";
import { getAnalyticsDashboard } from "../api/analyticsApi.js";
import { uploadCv } from "../api/cvApi.js";
import { getJobs, searchJobsByPrompt } from "../api/jobApi.js";
import { predictSalary } from "../api/salaryApi.js";
import MotionCityEqualizer from "../components/motion/MotionCityEqualizer.jsx";
import MotionHero from "../components/motion/MotionHero.jsx";
import MotionJobsPanel from "../components/motion/MotionJobsPanel.jsx";
import MotionMapSection from "../components/motion/MotionMapSection.jsx";
import MotionShell from "../components/motion/MotionShell.jsx";
import MotionSignals from "../components/motion/MotionSignals.jsx";
import MotionStats from "../components/motion/MotionStats.jsx";

const fallbackQuery = "";
const JOB_PAGE_SIZE = 50;
const ANALYTICS_DASHBOARD_LIMIT = 50;
const RESULTS_STORAGE_KEY = "jobradar-last-results";
let initialDataCache = null;
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
  poland: "PL",
  czechia: "CZ",
  "czech republic": "CZ",
  slovakia: "SK",
  hungary: "HU",
  romania: "RO",
  denmark: "DK",
  norway: "NO",
  finland: "FI",
  ireland: "IE",
  portugal: "PT",
  estonia: "EE",
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

const countryCenters = {
  SI: { lat: 46.1512, lng: 14.9955 },
  AT: { lat: 47.5162, lng: 14.5501 },
  DE: { lat: 51.1657, lng: 10.4515 },
  HR: { lat: 45.1, lng: 15.2 },
  IT: { lat: 41.8719, lng: 12.5674 },
  NL: { lat: 52.1326, lng: 5.2913 },
  CH: { lat: 46.8182, lng: 8.2275 },
  FR: { lat: 46.2276, lng: 2.2137 },
  ES: { lat: 40.4637, lng: -3.7492 },
  PL: { lat: 51.9194, lng: 19.1451 },
  CZ: { lat: 49.8175, lng: 15.473 },
  SK: { lat: 48.669, lng: 19.699 },
  HU: { lat: 47.1625, lng: 19.5033 },
  RO: { lat: 45.9432, lng: 24.9668 },
  BG: { lat: 42.7339, lng: 25.4858 },
  SE: { lat: 60.1282, lng: 18.6435 },
  DK: { lat: 56.2639, lng: 9.5018 },
  NO: { lat: 60.472, lng: 8.4689 },
  FI: { lat: 61.9241, lng: 25.7482 },
  IE: { lat: 53.4129, lng: -8.2439 },
  GB: { lat: 55.3781, lng: -3.436 },
  PT: { lat: 39.3999, lng: -8.2245 },
  EE: { lat: 58.5953, lng: 25.0136 },
  RS: { lat: 44.0165, lng: 21.0059 },
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

function numberOrNull(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function averageCoordinate(items, key) {
  const values = items.map((item) => numberOrNull(item[key])).filter((value) => value !== null);
  if (!values.length) return null;
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function formatSalary(job) {
  if (!job?.salaryMin && !job?.salaryMax) return "Salary n/a";
  const min = job.salaryMin ? `${Number(job.salaryMin).toLocaleString("sl-SI")} EUR` : "";
  const max = job.salaryMax ? `${Number(job.salaryMax).toLocaleString("sl-SI")} EUR` : "";
  return [min, max].filter(Boolean).join(" - ");
}

function sourceValue(job) {
  return job?.sourceUrl || job?.sourceWebsite || job?.url || "";
}

function isUrl(value) {
  return /^https?:\/\//i.test(String(value || "").trim());
}

function mapJob(job, index) {
  const parts = String(job.location || "").split(",").map((part) => part.trim()).filter(Boolean);
  const city = job.city || parts[0] || "Unknown";
  const region = job.region || (parts.length > 2 ? parts[1] : "");
  const country = job.country || parts.at(-1) || "";
  const latitude = numberOrNull(job.latitude);
  const longitude = numberOrNull(job.longitude);
  const source = sourceValue(job);
  const skills = Array.isArray(job.skills) && job.skills.length ? job.skills : ["AI match", "Skill fit"];

  return {
    id: job.id || `${job.title}-${index}`,
    title: job.title || "Open role",
    company: job.companyName || "Podjetje ni navedeno",
    city,
    region,
    country,
    countryCode: countryCode(country, region || "EU"),
    latitude,
    longitude,
    salary: formatSalary(job),
    salaryMin: job.salaryMin ?? null,
    salaryMax: job.salaryMax ?? null,
    sourceUrl: isUrl(source) ? source : "",
    sourceLabel: source || "Source not available",
    description: job.description || "",
    educationLevel: job.educationLevel || "Not specified",
    postedDate: job.postedDate || "",
    matchLevel: job.matchLevel || "",
    match: Number.isFinite(Number(job.matchScore)) ? Number(job.matchScore) : Math.max(72, 96 - index * 3),
    confidence: Number.isFinite(Number(job.confidenceScore)) ? Number(job.confidenceScore) : null,
    level: job.experienceLevel || "Not specified",
    mode: job.workMode || "Not specified",
    skills,
    tags: skills.slice(0, 4),
  };
}

function statValue(list, fallback = "No data") {
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
      if (!label || label === "Ni navedeno" || label === "Not specified" || label === "Unknown") return;
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
  return "Other roles";
}

function buildFilteredAnalytics(filteredJobs, meta = {}) {
  const totalJobs = meta.totalCount ?? filteredJobs.length;
  const cityStats = countItems(filteredJobs, (job) => job.city).map((item) => {
    const sample = filteredJobs.find((job) => job.city === item.label);
    const coords = locationCoordinates[item.label.toLowerCase()] || {};
    return {
      ...item,
      city: item.label,
      region: sample?.region || null,
      country: sample?.country || null,
      latitude: sample?.latitude ?? coords.lat ?? null,
      longitude: sample?.longitude ?? coords.lng ?? null,
    };
  });
  const countryStats = countItems(filteredJobs, (job) => job.country).map((item) => {
    const countryJobs = filteredJobs.filter((job) => job.country === item.label);
    const sample = countryJobs[0];
    const code = countryCode(item.label, sample?.countryCode);
    const center = countryCenters[code];
    return {
      ...item,
      country: item.label,
      latitude: averageCoordinate(countryJobs, "latitude") ?? center?.lat ?? null,
      longitude: averageCoordinate(countryJobs, "longitude") ?? center?.lng ?? null,
    };
  });
  const remoteJobs = filteredJobs.filter((job) => job.mode.toLowerCase().includes("remote")).length;
  const hybridJobs = filteredJobs.filter((job) => job.mode.toLowerCase().includes("hybrid")).length;
  const averageMatch = meta.averageMatch ?? (filteredJobs.length
    ? Math.round(filteredJobs.reduce((sum, job) => sum + Number(job.match || 0), 0) / filteredJobs.length)
    : 0);

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
    topSkills: countItems(filteredJobs, (job) => job.skills?.length ? job.skills : job.tags),
    topRoles: countItems(filteredJobs, (job) => classifyRole(job.title)),
    cityStats,
    regionStats: countItems(filteredJobs, (job) => job.region),
    countryStats,
    experienceLevelStats: countItems(filteredJobs, (job) => job.level),
    workTypeStats: countItems(filteredJobs, (job) => job.mode.split(",").map((value) => value.trim())),
  };
}

function hasAnalyticsData(analytics) {
  if (!analytics) return false;
  const summary = analytics.summary || {};
  const listKeys = [
    "topSkills",
    "topRoles",
    "cityStats",
    "regionStats",
    "countryStats",
    "experienceLevelStats",
    "workTypeStats",
  ];

  return Number(summary.totalJobs || 0) > 0
    || Number(summary.totalCompanies || 0) > 0
    || listKeys.some((key) => Array.isArray(analytics[key]) && analytics[key].length > 0);
}

function analyticsOrJobFallback(analyticsData, mappedJobs, totalCount) {
  if (hasAnalyticsData(analyticsData)) {
    return { ...analyticsData, isFiltered: false };
  }

  return {
    ...buildFilteredAnalytics(mappedJobs, { totalCount }),
    isFiltered: false,
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
      [String(totalJobs), "active listings", "The complete job database"],
      [statValue(analytics?.topSkills), "top skill", "Most common requirement in listings"],
      [statValue(analytics?.topRoles), "most wanted role", "Current market by category"],
      [statValue(analytics?.cityStats), "strongest location", `${summary?.totalLocations ?? analytics?.cityStats?.length ?? 0} locations in the database`],
    ];
  }

  return [
    [String(totalJobs), "matching roles", analytics?.isFiltered ? "For the current CV/prompt" : "Active listings in the database"],
    [averageMatch ? `${averageMatch}%` : statValue(analytics?.topSkills), averageMatch ? "average match" : "top skill", averageMatch ? "Average match score of results" : "Most common requirement in listings"],
    [statValue(analytics?.cityStats), "strongest location", `${summary?.totalLocations ?? analytics?.cityStats?.length ?? 0} locations in this selection`],
    [String(remoteJobs + hybridJobs), "remote/hybrid", "Flexible roles in this selection"],
  ];
}

function mapAnalyticsToStats(analytics) {
  return [
    {
      title: "Most wanted skills",
      value: statValue(analytics?.topSkills),
      detail: analytics?.topSkills?.slice(0, 4).map((item) => item.label).join("\n") || "Data is loaded from the backend analytics API.",
      tone: "cyan",
    },
    {
      title: "Most wanted roles",
      value: statValue(analytics?.topRoles),
      detail: analytics?.topRoles?.slice(0, 4).map((item) => `${item.label} (${item.count})`).join("\n") || "Developers, designers, chefs, therapists and other roles.",
      tone: "pink",
    },
    {
      title: "Jobs by cities/regions",
      value: statValue(analytics?.cityStats),
      detail: analytics?.regionStats?.slice(0, 4).map((item) => `${item.label} (${item.count})`).join("\n") || "Overview of cities and regions from listing locations.",
      tone: "lime",
    },
    {
      title: "Experience level",
      value: statValue(analytics?.experienceLevelStats),
      detail: analytics?.experienceLevelStats?.slice(0, 4).map((item) => `${item.label} ${item.percentage}%`).join("\n") || "Distribution of listings by required seniority.",
      tone: "amber",
    },
    {
      title: "Work type",
      value: statValue(analytics?.workTypeStats),
      detail: analytics?.workTypeStats?.slice(0, 4).map((item) => `${item.label} (${item.count})`).join("\n") || "Remote, hybrid and on-site signals from the database.",
      tone: "cyan",
    },
    {
      title: "Companies in selection",
      value: String(analytics?.summary?.totalCompanies ?? "0"),
      detail: analytics?.isFiltered ? "Number of companies in filtered results only." : "Number of companies in the currently loaded listings.",
      tone: "pink",
    },
  ];
}

function mapRoleMix(analytics) {
  const colors = ["#2563eb", "#0f766e", "#475569", "#7c3aed", "#0369a1"];
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

  const countries = countryStats.slice(0, 8).map((country, index) => {
    const code = countryCode(country.country || country.label, `C${index}`);
    const center = countryCenters[code] || { lat: 46 + index, lng: 14 + index };

    return {
      code,
      mapId: code,
      name: country.label,
      jobs: country.count,
      lat: numberOrNull(country.latitude) ?? center.lat,
      lng: numberOrNull(country.longitude) ?? center.lng,
      color: colors[index % colors.length],
    };
  });

  const cities = cityStats.map((city, index) => {
    const code = countryCode(city.country, city.region || "EU");
    const cityCoords = locationCoordinates[String(city.city || city.label).toLowerCase()];
    const countryCenter = countryCenters[code] || { lat: 46 + index / 3, lng: 14 + index / 3 };

    return {
      name: city.city || city.label,
      jobs: city.count,
      country: code,
      countryName: city.country || city.region || "Europe",
      lat: numberOrNull(city.latitude) ?? cityCoords?.lat ?? countryCenter.lat,
      lng: numberOrNull(city.longitude) ?? cityCoords?.lng ?? countryCenter.lng,
      color: colors[index % colors.length],
    };
  });

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

function totalFromApiResponse(data, fallback) {
  return Number.isFinite(Number(data?.totalCount)) ? Number(data.totalCount) : fallback;
}

function readStoredResults(expectedMode) {
  if (typeof window === "undefined") return null;

  try {
    const raw = window.sessionStorage.getItem(RESULTS_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (expectedMode && parsed?.mode !== expectedMode) return null;
    return parsed;
  } catch {
    return null;
  }
}

function saveStoredResults(payload) {
  if (typeof window === "undefined") return;

  try {
    window.sessionStorage.setItem(RESULTS_STORAGE_KEY, JSON.stringify(payload));
  } catch {
    // If storage is unavailable, the app still works; only back navigation restore is skipped.
  }
}

function clearStoredResults() {
  if (typeof window === "undefined") return;

  try {
    window.sessionStorage.removeItem(RESULTS_STORAGE_KEY);
  } catch {
    // Ignore storage errors.
  }
}

async function loadSalaryPrediction(filterRequest) {
  if (!filterRequest) return null;

  try {
    const prediction = await predictSalary(filterRequest);
    return prediction?.available ? prediction : null;
  } catch {
    return null;
  }
}

export default function MotionExperience({ initialMode = "idle", resultPage = false }) {
  const storedResult = resultPage ? readStoredResults(initialMode) : null;
  const [mode, setMode] = useState(storedResult?.mode || initialMode);
  const [query, setQuery] = useState(storedResult?.query || fallbackQuery);
  const [cvName, setCvName] = useState(storedResult?.cvName || "");
  const [processingMode, setProcessingMode] = useState("thinking");
  const [jobs, setJobs] = useState(storedResult?.jobs || []);
  const [jobsTotalCount, setJobsTotalCount] = useState(storedResult?.jobsTotalCount || 0);
  const [jobsPage, setJobsPage] = useState(storedResult?.jobsPage || 0);
  const [jobsHasMore, setJobsHasMore] = useState(Boolean(storedResult?.jobsHasMore));
  const [activeFilter, setActiveFilter] = useState(storedResult?.activeFilter || null);
  const [salaryPrediction, setSalaryPrediction] = useState(storedResult?.salaryPrediction || null);
  const [analytics, setAnalytics] = useState(storedResult?.analytics || null);
  const [lastResultsMode, setLastResultsMode] = useState(storedResult?.lastResultsMode || (storedResult?.mode || ""));
  const [initialLoading, setInitialLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadingProgress, setLoadingProgress] = useState(0);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [theme, setTheme] = useState(() => localStorage.getItem("smartjobs-theme") || "light");

  const activeMode = mode;
  const isAnalyticsPage = activeMode === "analytics";
  const showAiLoadingOverlay = loading && processingMode === "thinking";
  const score = analytics?.summary?.averageMatch
    ?? (jobs.length ? Math.round(jobs.reduce((sum, job) => sum + job.match, 0) / jobs.length) : 0);
  const signals = useMemo(() => mapAnalyticsToSignals(analytics, jobsTotalCount || jobs.length), [analytics, jobs.length, jobsTotalCount]);
  const statCards = useMemo(() => mapAnalyticsToStats(analytics), [analytics]);
  const { countries, cities } = useMemo(() => mapLocations(analytics), [analytics]);

  const toggleTheme = () => {
    setTheme((currentTheme) => {
      const nextTheme = currentTheme === "light" ? "dark" : "light";
      localStorage.setItem("smartjobs-theme", nextTheme);
      return nextTheme;
    });
  };

  useEffect(() => {
    if (!loading) {
      setLoadingProgress(0);
      return undefined;
    }

    const startedAt = Date.now();
    setLoadingProgress(3);
    const interval = window.setInterval(() => {
      const elapsedSeconds = (Date.now() - startedAt) / 1000;
      let nextProgress;

      if (elapsedSeconds <= 4) {
        nextProgress = 5 + (elapsedSeconds / 4) * 52;
      } else if (elapsedSeconds <= 9) {
        nextProgress = 57 + ((elapsedSeconds - 4) / 5) * 29;
      } else if (elapsedSeconds <= 18) {
        nextProgress = 86 + ((elapsedSeconds - 9) / 9) * 10;
      } else {
        nextProgress = 96 + Math.min(2, ((elapsedSeconds - 18) / 30) * 2);
      }

      setLoadingProgress(Math.min(98, nextProgress));
    }, 500);

    return () => window.clearInterval(interval);
  }, [loading]);

  useEffect(() => {
    let cancelled = false;

    async function loadInitialData() {
      if (resultPage && storedResult?.jobs?.length) {
        setStatus("");
        return;
      }

      if (initialDataCache) {
        const cachedAnalytics = hasAnalyticsData(initialDataCache.analytics)
          ? initialDataCache.analytics
          : analyticsOrJobFallback(null, initialDataCache.jobs, initialDataCache.totalCount);
        setJobs(initialDataCache.jobs);
        setJobsTotalCount(initialDataCache.totalCount);
        setJobsPage(initialDataCache.page);
        setJobsHasMore(initialDataCache.hasMore);
        setActiveFilter(null);
        setAnalytics(cachedAnalytics);
        initialDataCache = { ...initialDataCache, analytics: cachedAnalytics };
        setStatus("");
        return;
      }

      try {
        setInitialLoading(true);
        setStatus("Loading data from the backend API...");
        const [jobsData, analyticsData] = await Promise.all([getJobs({ page: 0, size: JOB_PAGE_SIZE }), getAnalyticsDashboard(ANALYTICS_DASHBOARD_LIMIT)]);
        if (cancelled) return;
        const mappedJobs = listFromApiResponse(jobsData).map(mapJob);
        const totalCount = totalFromApiResponse(jobsData, mappedJobs.length);
        const cachedData = {
          jobs: mappedJobs,
          totalCount,
          page: jobsData?.page ?? 0,
          hasMore: Boolean(jobsData?.hasMore),
          analytics: analyticsOrJobFallback(analyticsData, mappedJobs, totalCount),
        };
        initialDataCache = cachedData;
        setJobs(mappedJobs);
        setJobsTotalCount(cachedData.totalCount);
        setJobsPage(cachedData.page);
        setJobsHasMore(cachedData.hasMore);
        setActiveFilter(null);
        setAnalytics(cachedData.analytics);
        setStatus("");
      } catch (err) {
        if (cancelled) return;
        setError("The backend is currently unavailable. The UI is loaded and data will appear when the API starts.");
        setStatus("");
      } finally {
        if (!cancelled) {
          setInitialLoading(false);
        }
      }
    }

    loadInitialData();
    return () => {
      cancelled = true;
    };
  }, []);

  const loadMoreJobs = async () => {
    if (activeMode !== "idle" || loading || !jobsHasMore) return;
    setLoading(true);
    setStatus("Loading the next listings...");

    try {
      const nextPage = jobsPage + 1;
      const data = await getJobs({ page: nextPage, size: JOB_PAGE_SIZE });
      const mappedJobs = listFromApiResponse(data).map(mapJob);
      setJobs((currentJobs) => [...currentJobs, ...mappedJobs]);
      setJobsTotalCount(totalFromApiResponse(data, jobsTotalCount));
      setJobsPage(data?.page ?? nextPage);
      setJobsHasMore(Boolean(data?.hasMore));
    } catch (err) {
      setError("The next page of listings is currently unavailable.");
    } finally {
      setLoading(false);
      setStatus("");
    }
  };

  const submitPrompt = async (event) => {
    event.preventDefault();
    if (!query.trim()) {
      setError("Enter a prompt or upload a CV to filter listings.");
      return;
    }
    setLoading(true);
    setError("");
    setSalaryPrediction(null);
    setStatus(processingMode === "thinking"
      ? "AI is reading the prompt and filtering listings..."
      : "Fast mode detects clear criteria and ranks listings...");

    try {
      const data = await searchJobsByPrompt(query, processingMode);
      setLoadingProgress(100);
      const mappedJobs = listFromApiResponse(data).map(mapJob);
      setJobs(mappedJobs);
      const totalCount = totalFromApiResponse(data, mappedJobs.length);
      const nextAnalytics = buildFilteredAnalytics(mappedJobs, { totalCount, averageMatch: data?.averageMatch });
      const nextFilter = data?.filterRequest || null;
      const nextSalaryPrediction = await loadSalaryPrediction(nextFilter);
      setJobsTotalCount(totalCount);
      setJobsPage(data?.page ?? 0);
      setJobsHasMore(false);
      setActiveFilter(nextFilter);
      setSalaryPrediction(nextSalaryPrediction);
      setAnalytics(nextAnalytics);
      setLastResultsMode("search");
      setMode("search");
      saveStoredResults({
        mode: "search",
        query,
        cvName: "",
        jobs: mappedJobs,
        jobsTotalCount: totalCount,
        jobsPage: data?.page ?? 0,
        jobsHasMore: false,
        activeFilter: nextFilter,
        salaryPrediction: nextSalaryPrediction,
        analytics: nextAnalytics,
        lastResultsMode: "search",
      });
      window.history.pushState({}, "", "/motion-prompt");
    } catch (err) {
      setError("The prompt API did not return a result. Check the backend and AI service.");
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
    setSalaryPrediction(null);
    setStatus(processingMode === "thinking"
      ? "The CV is being read, AI is extracting the profile and matching listings..."
      : "The CV is being read, the fast parser is extracting skills and ranking listings...");

    try {
      const data = await uploadCv(file, processingMode);
      setLoadingProgress(100);
      const mappedJobs = (data?.jobs || []).map(mapJob);
      setJobs(mappedJobs);
      const totalCount = totalFromApiResponse(data, mappedJobs.length);
      const nextAnalytics = buildFilteredAnalytics(mappedJobs, { totalCount, averageMatch: data?.averageMatch });
      const nextFilter = data?.filterRequest || null;
      const nextSalaryPrediction = await loadSalaryPrediction(nextFilter);
      setJobsTotalCount(totalCount);
      setJobsPage(data?.page ?? 0);
      setJobsHasMore(false);
      setActiveFilter(nextFilter);
      setSalaryPrediction(nextSalaryPrediction);
      setAnalytics(nextAnalytics);
      setLastResultsMode("cv");
      saveStoredResults({
        mode: "cv",
        query,
        cvName: file.name,
        jobs: mappedJobs,
        jobsTotalCount: totalCount,
        jobsPage: data?.page ?? 0,
        jobsHasMore: false,
        activeFilter: nextFilter,
        salaryPrediction: nextSalaryPrediction,
        analytics: nextAnalytics,
        lastResultsMode: "cv",
      });
      window.history.pushState({}, "", "/motion-cv");
    } catch (err) {
      setError("The CV API did not return a result. Check that the backend and AI service are running.");
    } finally {
      setLoading(false);
      setStatus("");
    }
  };

  const showCurrentStatistics = () => {
    if (activeMode === "idle") return;
    setMode("analytics");
    window.history.pushState({}, "", "/analytics");
  };

  const returnToJobs = () => {
    const nextMode = lastResultsMode || (cvName ? "cv" : "search");
    setMode(nextMode);
    window.history.pushState({}, "", nextMode === "cv" ? "/motion-cv" : "/motion-prompt");
  };

  const resetHome = () => {
    setMode("idle");
    setQuery(fallbackQuery);
    setCvName("");
    setActiveFilter(null);
    setSalaryPrediction(null);
    setLastResultsMode("");
    setInitialLoading(false);
    setLoading(false);
    setError("");
    setStatus("");
    clearStoredResults();
    window.history.pushState({}, "", "/motion");
    window.dispatchEvent(new Event("jobradar:navigate"));
  };

  return (
    <MotionShell mode={activeMode} score={score} theme={theme} onThemeToggle={toggleTheme} onHomeClick={resetHome} onStatisticsClick={showCurrentStatistics}>
      {showAiLoadingOverlay ? <MotionLoadingOverlay mode={activeMode} status={status} progress={loadingProgress} /> : null}
      {!isAnalyticsPage ? (
        <MotionHero
          mode={activeMode}
          score={score}
          query={query}
          cvName={cvName}
          resultPage={activeMode === "cv" || activeMode === "search"}
          loading={loading}
          status={status}
          error={error}
          processingMode={processingMode}
          onProcessingModeChange={setProcessingMode}
          onQueryChange={setQuery}
          onPromptSubmit={submitPrompt}
          onCvUpload={handleCvUpload}
        />
      ) : null}
      {activeMode !== "idle" && !isAnalyticsPage ? (
        <section className="motion-grid" id="results">
          <MotionJobsPanel
            mode={activeMode}
            score={score}
            jobs={jobs}
            totalCount={jobsTotalCount || jobs.length}
            filterRequest={activeFilter}
            salaryPrediction={salaryPrediction}
            query={query}
            cvName={cvName}
            hasMore={false}
            loading={loading}
            error={error}
            onLoadMore={loadMoreJobs}
            onViewStatistics={showCurrentStatistics}
          />
        </section>
      ) : null}
      {isAnalyticsPage ? (
        <section className={`analytics-section ${isAnalyticsPage ? "analytics-page" : ""}`}>
          {lastResultsMode ? (
            <div className="analytics-return-row">
              <button className="back-to-jobs-button" type="button" onClick={returnToJobs}>
                Back to jobs
              </button>
            </div>
          ) : null}
          {initialLoading && !analytics ? (
            <div className="analytics-loading-card">
              <strong>Loading statistics...</strong>
              <p>Reading listings, skills, locations and market signals from the backend API.</p>
            </div>
          ) : (
            <>
              <MotionSignals signals={signals} />
              <MotionMapSection countries={countries} cities={cities} analytics={analytics} />
              <MotionCityEqualizer cities={cities} />
              <MotionStats cards={statCards} />
            </>
          )}
        </section>
      ) : null}
    </MotionShell>
  );
}

function MotionLoadingOverlay({ mode, status, progress = 0 }) {
  const isCvMode = mode === "cv";
  const steps = isCvMode
    ? [
        ["Reading CV", UserRound],
        ["Extracting skills", Brain],
        ["Calculating match", Database],
        ["Building analytics", BarChart3],
      ]
    : [
        ["Reading prompt", Search],
        ["Detecting intent", Brain],
        ["Filtering listings", Database],
        ["Refreshing analytics", BarChart3],
      ];
  const safeProgress = Math.max(0, Math.min(100, progress));

  return (
    <div className="motion-loading-overlay" role="status" aria-live="polite">
      <div className="loading-copy">
        <div className="loading-visual-card" aria-hidden="true">
          <FileText size={46} strokeWidth={1.65} />
          <span>{isCvMode ? "CV" : "AI"}</span>
        </div>
        <strong>
          {isCvMode ? "Reading CV, " : "Reading prompt, "}
          <span>AI is extracting the profile and matching listings...</span>
        </strong>
        <p>AI prepares the filter, the backend calculates compatibility and refreshes analytics.</p>
        <div className="loading-progress">
          <i style={{ width: `${safeProgress}%` }}></i>
        </div>
        <strong className="loading-percent">{Math.round(safeProgress)}%</strong>
        <p className="loading-status">{status || "Analyzing data and preparing results."}</p>
        <div className="loading-steps">
          {steps.map(([step, Icon], index) => (
            <em key={step} style={{ animationDelay: `${index * 180}ms` }}>
              <Icon size={18} strokeWidth={1.9} />
              {step}
            </em>
          ))}
        </div>
      </div>
    </div>
  );
}
