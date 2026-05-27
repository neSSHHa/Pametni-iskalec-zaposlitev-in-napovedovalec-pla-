import { BarChart3, BriefcaseBusiness, Check, MapPin, Scale, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useComparison } from "../../context/ComparisonContext.jsx";

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
  const comparison = useComparison();
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
            onToggleCompare={() => comparison.toggleJob({ ...job, compareSourcePath: window.location.pathname })}
            selectedForCompare={comparison.isSelected(job.id)}
          />
        ))}
      </div>

      {hiddenLoadedJobs || hasMore ? (
        <LoadMoreButton remainingJobs={remainingJobs} hasMore={hasMore && !hiddenLoadedJobs} onClick={handleLoadMore} />
      ) : null}
      {selectedJob ? (
        <JobDetailsModal
          job={selectedJob}
          filterRequest={filterRequest}
          onClose={() => setSelectedJob(null)}
          onToggleCompare={() => comparison.toggleJob({ ...selectedJob, compareSourcePath: window.location.pathname })}
          selectedForCompare={comparison.isSelected(selectedJob.id)}
        />
      ) : null}
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
            {chips.map((chip) => (
              <span className={chip.type === "role" ? "role-chip" : ""} key={`${chip.type}-${chip.label}`}>
                {chip.label}
              </span>
            ))}
          </div>
        ) : null}
      </div>
      <div className="query-radar" aria-hidden="true">
        <span></span>
      </div>
    </article>
  );
}

function JobCard({ job, index, accent, mode, onOpen, onToggleCompare, selectedForCompare }) {
  return (
    <article
      className={`motion-job ${selectedForCompare ? "selected-for-compare" : ""}`}
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
      <button
        className={`job-compare-card-button ${selectedForCompare ? "active" : ""}`}
        type="button"
        aria-label={selectedForCompare ? "Remove job from comparison" : "Add job to comparison"}
        onClick={(event) => {
          event.stopPropagation();
          onToggleCompare?.();
        }}
      >
        <Scale size={17} strokeWidth={1.9} />
        <span>{selectedForCompare ? "Comparing" : "Compare"}</span>
      </button>
    </article>
  );
}

function filterChips(filterRequest) {
  if (!filterRequest) return [];

  const job = filterRequest.job || {};
  const location = filterRequest.location || {};
  const chips = [];

  addChip(chips, job.jobname, "role");
  addChip(chips, location.city);
  addChip(chips, location.country);
  addChip(chips, location.region);
  addChip(chips, job.experienceLevelName);
  addChip(chips, job.educationLevel);
  addChip(chips, job.requiredExperience ? `${job.requiredExperience}+ years of experience` : "");
  addListChips(chips, filterRequest.workTypes);
  addListChips(chips, filterRequest.skills);

  return uniqueChips(chips).slice(0, 18);
}

function addChip(chips, value, type = "filter") {
  if (value === null || value === undefined || value === "") return;
  chips.push({ label: String(value), type });
}

function addListChips(chips, values) {
  if (!Array.isArray(values)) return;
  values.filter(Boolean).forEach((value) => addChip(chips, value));
}

function uniqueChips(chips) {
  const seen = new Set();
  return chips.filter((chip) => {
    const key = chip.label.toLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function LoadMoreButton({ remainingJobs, hasMore, onClick }) {
  return (
    <button className="job-load-more" type="button" onClick={onClick}>
      Load more
      <span>{hasMore ? "load next page" : `${remainingJobs} hidden listings`}</span>
    </button>
  );
}

export function JobDetailsModal({ job, filterRequest, onClose, onToggleCompare, selectedForCompare = false }) {
  const userSkills = Array.isArray(filterRequest?.skills) ? filterRequest.skills : [];
  const detailsRef = useRef(null);

  useEffect(() => {
    detailsRef.current?.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [job?.id]);

  return (
    <div className="job-details-backdrop" role="dialog" aria-modal="true" onClick={onClose}>
      <article className="job-details-page" ref={detailsRef} onClick={(event) => event.stopPropagation()}>
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
            {job.sourceUrl ? (
              <div className="job-source-row">
                <span>Source</span>
                <a className="job-source-link" href={job.sourceUrl} target="_blank" rel="noreferrer">
                  Open source listing
                </a>
              </div>
            ) : null}
          </div>
          <aside>
            <strong>{job.match}%</strong>
            <span>Compatibility</span>
            {onToggleCompare ? (
              <button
                className={`job-details-compare-button ${selectedForCompare ? "active" : ""}`}
                type="button"
                onClick={onToggleCompare}
              >
                <Scale size={15} strokeWidth={1.8} />
                <span>{selectedForCompare ? "Comparing" : "Compare"}</span>
              </button>
            ) : null}
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
