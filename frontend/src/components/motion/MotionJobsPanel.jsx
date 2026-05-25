import { BarChart3, Bookmark, BriefcaseBusiness, Check, MapPin, Scale, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

const accentColors = ["#8b5cf6", "#10b981", "#64748b", "#a855f7", "#3b82f6"];
const PAGE_SIZE = 7;

export default function MotionJobsPanel({
  mode,
  score,
  jobs = [],
  totalCount,
  filterRequest,
  query = "",
  cvName = "",
  hasMore = false,
  loading = false,
  error = "",
  onLoadMore,
  onViewStatistics,
}) {
  const displayJobs = jobs;
  const resultCount = totalCount ?? displayJobs.length;
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [selectedJob, setSelectedJob] = useState(null);
  const visibleJobs = displayJobs.slice(0, visibleCount);
  const hiddenLoadedJobs = Math.max(displayJobs.length - visibleCount, 0);
  const remainingJobs = Math.max(resultCount - visibleJobs.length, 0);
  const chips = useMemo(() => filterChips(filterRequest), [filterRequest]);

  const handleLoadMore = () => {
    if (hiddenLoadedJobs > 0) {
      setVisibleCount((count) => count + PAGE_SIZE);
      return;
    }
    onLoadMore?.();
    setVisibleCount((count) => count + PAGE_SIZE);
  };

  useEffect(() => {
    setVisibleCount(PAGE_SIZE);
    setSelectedJob(null);
  }, [displayJobs]);

  return (
    <section className="motion-panel job-panel">
      <div className="results-overview">
        <div>
          <span>Search results</span>
          <h2>Found <b>{Number(resultCount || 0).toLocaleString("sl-SI")}</b> jobs</h2>
          <p>Results are ranked by compatibility, with additional market statistics.</p>
        </div>
        <CompatibilitySummary score={score} />
      </div>

      <QuerySummaryCard mode={mode} query={query} cvName={cvName} chips={chips} />

      {loading ? <p className="motion-status">Waiting for the API response...</p> : null}
      {error ? <p className="motion-error">{error}</p> : null}
      {!loading && !displayJobs.length ? <p className="motion-status">No listings for the current selection.</p> : null}
      {!loading && displayJobs.length ? (
        <div className="result-toolbar">
          <p className="result-range">Showing {visibleJobs.length} of {Number(resultCount || 0).toLocaleString("sl-SI")} results.</p>
          <button className="view-statistics-button" type="button" onClick={onViewStatistics}>
            <BarChart3 size={18} strokeWidth={1.9} />
            View statistics
          </button>
        </div>
      ) : null}

      <div className="job-stream">
        {visibleJobs.map((job, index) => (
          <JobCard
            accent={accentColors[index % accentColors.length]}
            index={index}
            job={job}
            key={job.id}
            mode={mode}
            onOpen={() => setSelectedJob(job)}
          />
        ))}
      </div>

      {hiddenLoadedJobs || hasMore ? (
        <LoadMoreButton remainingJobs={remainingJobs} hasMore={hasMore && !hiddenLoadedJobs} onClick={handleLoadMore} />
      ) : null}
      {selectedJob ? <JobDetailsModal job={selectedJob} filterRequest={filterRequest} onClose={() => setSelectedJob(null)} /> : null}
    </section>
  );
}

function CompatibilitySummary({ score }) {
  const safeScore = Math.max(0, Math.min(100, Number(score) || 0));

  return (
    <aside className="compatibility-summary">
      <div className="score-ring" style={{ "--score": `${safeScore}%` }}>
        <strong>{safeScore}%</strong>
        <span>Total compatibility</span>
      </div>
      <p>Calculated from current results and match score values.</p>
    </aside>
  );
}

function QuerySummaryCard({ mode, query, cvName, chips }) {
  const text = mode === "cv"
    ? (cvName ? `CV: ${cvName}` : "CV analiza")
    : query || "Prompt ni naveden.";

  return (
    <article className="query-summary-card">
      <div className="query-icon"><Scale size={24} strokeWidth={1.8} /></div>
      <div>
        <span>Your query</span>
        <p>{text}</p>
        {chips.length ? (
          <div className="filter-chip-strip" aria-label="Detected filters">
            {chips.map((chip) => <span key={chip}>{chip}</span>)}
          </div>
        ) : null}
      </div>
      <div className="query-radar" aria-hidden="true">
        <span></span>
      </div>
    </article>
  );
}

function JobCard({ job, index, accent, mode, onOpen }) {
  return (
    <article
      className="motion-job"
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") onOpen();
      }}
      style={{ "--accent": accent, animationDelay: `${index * 80}ms` }}
    >
      <div className="job-logo">{initials(job.company)}</div>
      <div className="job-main">
        <span>{job.company}</span>
        <h3>{job.title}</h3>
        <p>
          <MapPin size={13} /> {job.city}, {job.country}
          {job.mode ? <> <i></i> {job.mode}</> : null}
          {job.level ? <> <i></i> {job.level}</> : null}
        </p>
        <footer>
          {job.tags.map((tag) => <em key={tag}>{tag}</em>)}
        </footer>
      </div>
      <aside>
        <strong>{mode === "idle" ? Math.min(job.match + 1, 99) : job.match}%</strong>
        <small>Compatibility</small>
        <div className="match-dots" aria-hidden="true">
          {Array.from({ length: 8 }).map((_, dotIndex) => (
            <i className={dotIndex < Math.ceil((Number(job.match) || 0) / 14) ? "active" : ""} key={dotIndex}></i>
          ))}
        </div>
        {job.confidence !== null && job.confidence !== undefined ? <small>{job.confidence}% confidence</small> : null}
        <span>{job.salary}</span>
      </aside>
      <button className="job-save-button" type="button" aria-label="Save or compare job" onClick={(event) => event.stopPropagation()}>
        <Bookmark size={19} strokeWidth={1.8} />
      </button>
    </article>
  );
}

function filterChips(filterRequest) {
  if (!filterRequest) return [];

  const job = filterRequest.job || {};
  const location = filterRequest.location || {};
  const chips = [];

  addChip(chips, job.jobname);
  addChip(chips, location.city);
  addChip(chips, location.country);
  addChip(chips, location.region);
  addChip(chips, job.experienceLevelName);
  addChip(chips, job.educationLevel);
  addChip(chips, job.requiredExperience ? `${job.requiredExperience}+ years of experience` : "");
  addListChips(chips, filterRequest.workTypes);
  addListChips(chips, filterRequest.skills);

  return [...new Set(chips)].slice(0, 18);
}

function addChip(chips, value) {
  if (value === null || value === undefined || value === "") return;
  chips.push(String(value));
}

function addListChips(chips, values) {
  if (!Array.isArray(values)) return;
  values.filter(Boolean).forEach((value) => addChip(chips, value));
}

function LoadMoreButton({ remainingJobs, hasMore, onClick }) {
  return (
    <button className="job-load-more" type="button" onClick={onClick}>
      Load more
      <span>{hasMore ? "load next page" : `${remainingJobs} hidden listings`}</span>
    </button>
  );
}

function JobDetailsModal({ job, filterRequest, onClose }) {
  const userSkills = Array.isArray(filterRequest?.skills) ? filterRequest.skills : [];

  return (
    <div className="job-details-backdrop" role="dialog" aria-modal="true" onClick={onClose}>
      <article className="job-details-page" onClick={(event) => event.stopPropagation()}>
        <button className="job-details-close" type="button" onClick={onClose} aria-label="Close job details">
          <X size={22} strokeWidth={1.8} />
        </button>
        <header>
          <div className="job-logo large">{initials(job.company)}</div>
          <div>
            <span>{job.company}</span>
            <h2>{job.title}</h2>
            <div className="job-details-meta">
              <b>{job.city}, {job.country}</b>
              <b>{job.mode}</b>
              <b>{job.level}</b>
            </div>
          </div>
          <aside>
            <strong>{job.match}%</strong>
            <span>Compatibility</span>
          </aside>
        </header>

        <div className="job-details-tags">
          {job.tags.map((tag) => <em key={tag}>{tag}</em>)}
        </div>

        <section>
          <span>Job description</span>
          <p>{job.description || "No description is available for this listing."}</p>
        </section>

        <div className="match-columns">
          <section>
            <span>What you have</span>
            <ul>
              {(userSkills.length ? userSkills : job.tags).map((skill) => (
                <li key={skill}><Check size={16} /> {skill}</li>
              ))}
            </ul>
          </section>
          <section>
            <span>What the job has</span>
            <ul>
              {job.tags.map((skill) => <li key={skill}><BriefcaseBusiness size={16} /> {skill}</li>)}
            </ul>
          </section>
        </div>

        <dl>
          <div>
            <dt>Salary</dt>
            <dd>{job.salary}</dd>
          </div>
          <div>
            <dt>Education</dt>
            <dd>{job.educationLevel}</dd>
          </div>
        </dl>
      </article>
    </div>
  );
}

function initials(value = "") {
  const letters = value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0])
    .join("");

  return letters || "J";
}
