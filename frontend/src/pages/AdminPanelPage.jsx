import {
  CircleAlert,
  CalendarClock,
  DatabaseZap,
  RefreshCw,
  Search,
  TerminalSquare,
  UsersRound,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { getAdminCacheStatus, getAdminLogs, getAdminOverview, refreshAdminCaches } from "../api/authApi.js";
import MotionShell from "../components/motion/MotionShell.jsx";
import useAuth from "../hooks/useAuth.js";

const oneHourAgo = () => toLocalInputValue(new Date(Date.now() - 60 * 60 * 1000));
const nowLocal = () => toLocalInputValue(new Date());

export default function AdminPanelPage() {
  const auth = useAuth();
  const [theme, setTheme] = useState(() => localStorage.getItem("smartjobs-theme") || "light");
  const [overview, setOverview] = useState(null);
  const [cacheStatus, setCacheStatus] = useState(null);
  const [systemLogs, setSystemLogs] = useState([]);
  const [weeklyLogs, setWeeklyLogs] = useState([]);
  const [systemLogQuery, setSystemLogQuery] = useState("");
  const [weeklyDisclaimerOpen, setWeeklyDisclaimerOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [service, setService] = useState("backend");
  const [from, setFrom] = useState(oneHourAgo);
  const [to, setTo] = useState(nowLocal);
  const [direction, setDirection] = useState("backward");
  const [loading, setLoading] = useState(false);
  const [cacheRefreshing, setCacheRefreshing] = useState(false);
  const [error, setError] = useState("");

  const adminReady = auth.authenticated && auth.admin;

  const toggleTheme = () => {
    setTheme((currentTheme) => {
      const nextTheme = currentTheme === "light" ? "dark" : "light";
      localStorage.setItem("smartjobs-theme", nextTheme);
      return nextTheme;
    });
  };

  useEffect(() => {
    if (!adminReady) return;
    loadOverview();
    loadCacheStatus();
    loadLogs();
    loadWeeklyLogs();
  }, [adminReady]);

  const stats = useMemo(() => [
    { label: "Keycloak users", value: overview?.users ?? "...", icon: UsersRound },
  ], [overview]);

  if (!auth.authenticated) {
    return <AccessPanel title="Admin login required" text="Sign in with a Keycloak admin account." action={auth.login} actionText="Login" />;
  }

  if (!auth.admin) {
    return <AccessPanel title="Admin only" text="Your account does not have the ADMIN role in Keycloak." />;
  }

  async function loadOverview() {
    getAdminOverview()
      .then(setOverview)
      .catch(() => setError("Admin overview could not be loaded."));
  }

  async function loadCacheStatus() {
    getAdminCacheStatus()
      .then(setCacheStatus)
      .catch(() => setError("Cache status could not be loaded."));
  }

  async function refreshCaches() {
    setCacheRefreshing(true);
    setError("");
    try {
      await refreshAdminCaches();
      await loadCacheStatus();
      await loadLogs();
    } catch {
      setError("Cache refresh failed.");
    } finally {
      setCacheRefreshing(false);
    }
  }

  async function loadLogs(event) {
    event?.preventDefault();
    setLoading(true);
    setError("");
    try {
      const response = await getAdminLogs({
        service,
        search: query,
        from: toIso(from),
        to: toIso(to),
        direction,
        limit: 160,
      });
      setSystemLogs(response.logs || []);
      setSystemLogQuery(response.query || "");
    } catch {
      setError("Logs could not be loaded. Check that Loki and Alloy are running.");
    } finally {
      setLoading(false);
    }
  }

  async function loadWeeklyLogs() {
    try {
      const response = await getAdminLogs({
        service: "weekly",
        from: toIso(from),
        to: toIso(to),
        direction,
        limit: 80,
      });
      setWeeklyLogs(response.logs || []);
    } catch {
      setWeeklyLogs([]);
    }
  }

  return (
    <MotionShell mode="admin" score={0} theme={theme} onThemeToggle={toggleTheme} onHomeClick={goHome}>
      <section className="admin-page">
        {error ? <p className="motion-error admin-error">{error}</p> : null}

        <section className="admin-console">
        <div className="admin-left">
          <div className="admin-grid">
            {stats.map((item) => {
              const Icon = item.icon;
              return (
                <article key={item.label}>
                  <Icon size={22} />
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </article>
              );
            })}
          </div>

          <article className="admin-card">
            <div className="admin-card-head">
              <div>
                <span>Cache health</span>
                <h2>Backend caches</h2>
              </div>
              <button type="button" onClick={refreshCaches} disabled={cacheRefreshing} title="Refresh backend caches">
                <RefreshCw size={17} />
                {cacheRefreshing ? "Refreshing" : "Refresh cache"}
              </button>
            </div>
            <CacheStatus status={cacheStatus} />
          </article>

          <article className="admin-card admin-weekly-card">
            <div className="admin-card-head">
              <div>
                <span>Data updater</span>
                <h2>Weekly updater logs</h2>
              </div>
              <button className="admin-danger-button" type="button" onClick={loadWeeklyLogs}>
                <RefreshCw size={18} />
                Refresh updater logs
              </button>
            </div>
            <button
              className="admin-disclaimer-toggle"
              type="button"
              onClick={() => setWeeklyDisclaimerOpen((open) => !open)}
              aria-expanded={weeklyDisclaimerOpen}
            >
              <CircleAlert size={18} />
              Weekly updater warning
            </button>
            {weeklyDisclaimerOpen ? (
              <p className="admin-disclaimer">
                This operation can take a very long time. With approximately 20 API keys, the expected duration is around
                two days. Do not start this process unless you are sure that enough API keys are available. If you are
                unsure, contact support before running it.
              </p>
            ) : null}
            <LogList logs={weeklyLogs} empty="No weekly updater logs for this range. This usually means the weekly updater container has not produced logs in the selected time window." compact />
          </article>
        </div>

        <div className="admin-right">
          <article className="admin-card admin-log-card">
            <div className="admin-card-head">
              <div>
                <span>Grafana Loki</span>
                <h2>Request and system logs</h2>
              </div>
              <TerminalSquare size={22} />
            </div>

            <form className="admin-log-form" onSubmit={loadLogs}>
              <label>
                Search
                <div className="admin-input-icon">
                  <Search size={16} />
                  <input
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="email, requestId, userId, path..."
                  />
                </div>
              </label>
              <label>
                Service
                <select value={service} onChange={(event) => setService(event.target.value)}>
                  <option value="backend">Backend</option>
                  <option value="weekly">Weekly updater</option>
                  <option value="all">All services</option>
                </select>
              </label>
              <label>
                From
                <div className="admin-input-icon">
                  <CalendarClock size={16} />
                  <input type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
                </div>
              </label>
              <label>
                To
                <div className="admin-input-icon">
                  <CalendarClock size={16} />
                  <input type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
                </div>
              </label>
              <label>
                Order
                <select value={direction} onChange={(event) => setDirection(event.target.value)}>
                  <option value="backward">Newest first</option>
                  <option value="forward">Oldest first</option>
                </select>
              </label>
              <button type="submit" disabled={loading}>
                <RefreshCw size={17} />
                {loading ? "Loading" : "Search logs"}
              </button>
            </form>

            <div className="admin-log-meta">
              <span>{systemLogs.length} logs loaded</span>
              {systemLogQuery ? <code>{systemLogQuery}</code> : null}
            </div>
            <LogList logs={systemLogs} empty="No logs match this search." />
          </article>
        </div>
        </section>
      </section>
    </MotionShell>
  );
}

function CacheStatus({ status }) {
  if (!status) {
    return <p className="admin-empty">Loading cache status...</p>;
  }

  return (
    <div className="admin-cache-list">
      <div className={status.allLoaded ? "admin-cache-summary ok" : "admin-cache-summary warn"}>
        <DatabaseZap size={18} />
        <span>{status.allLoaded ? "All cache components are loaded." : "Some cache components are not loaded."}</span>
      </div>
      {status.components?.map((component) => (
        <article className="admin-cache-row" key={component.name}>
          <div>
            <strong>{component.name}</strong>
            <span className={component.loaded ? "ok" : "warn"}>{component.loaded ? "Loaded" : "Not loaded"}</span>
          </div>
          <p>{formatDetails(component.details)}</p>
        </article>
      ))}
    </div>
  );
}

function LogList({ logs, empty, compact = false }) {
  if (!logs?.length) {
    return <p className="admin-empty">{empty}</p>;
  }

  return (
    <div className={compact ? "admin-log-list compact" : "admin-log-list"}>
      {logs.map((log, index) => (
        <article className="admin-log-row" key={`${log.timestamp}-${index}`}>
          <div>
            <time>{formatTime(log.timestamp)}</time>
            <span>{log.service}</span>
            <b>{log.level}</b>
          </div>
          <p>{log.message}</p>
        </article>
      ))}
    </div>
  );
}

function AccessPanel({ title, text, action, actionText }) {
  return (
    <main className="auth-page">
      <section className="auth-panel">
        <img src="/jobradar.svg" alt="" aria-hidden="true" />
        <span>Admin panel</span>
        <h1>{title}</h1>
        <p>{text}</p>
        {action ? <button type="button" onClick={action}>{actionText}</button> : null}
      </section>
    </main>
  );
}

function toIso(value) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toISOString();
}

function toLocalInputValue(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function formatDetails(details = {}) {
  const entries = Object.entries(details);
  if (!entries.length) return "No counters exposed.";
  return entries.map(([key, value]) => `${key}: ${value}`).join(" | ");
}

function formatTime(value) {
  try {
    return new Intl.DateTimeFormat("en", {
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function goHome() {
  window.history.pushState({}, "", "/motion");
  window.dispatchEvent(new Event("jobradar:navigate"));
}
