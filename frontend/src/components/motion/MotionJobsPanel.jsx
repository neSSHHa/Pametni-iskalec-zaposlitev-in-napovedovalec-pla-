import { BarChart3, BriefcaseBusiness, Check, MapPin, Scale, X } from "lucide-react";
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useComparison } from "../../context/ComparisonContext.jsx";

const accentColors = ["#8b5cf6", "#10b981", "#64748b", "#a855f7", "#3b82f6"];
const PAGE_SIZE = 7;

export default function MotionJobsPanel({
  mode,
  score,
  jobs = [],
  totalCount,
  filterRequest,
  salaryPrediction,
  salaryPredictionLoading = false,
  query = "",
  cvName = "",
  hasMore = false,
  loading = false,
  error = "",
  onLoadMore,
  onViewStatistics,
}) {
  const [sortMode, setSortMode] = useState("compatibility");
  const displayJobs = useMemo(() => sortJobs(jobs, sortMode), [jobs, sortMode]);
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
      <SalaryPredictionCard loading={salaryPredictionLoading} prediction={salaryPrediction} resultCount={resultCount} />

      {loading ? <p className="motion-status">Waiting for the API response...</p> : null}
      {error ? <p className="motion-error">{error}</p> : null}
      {!loading && !displayJobs.length ? <p className="motion-status">No listings for the current selection.</p> : null}
      {!loading && displayJobs.length ? (
        <div className="result-toolbar">
          <div className="result-toolbar-left">
            <p className="result-range">Showing {visibleJobs.length} of {Number(resultCount || 0).toLocaleString("sl-SI")} results.</p>
          </div>
          <div className="result-toolbar-actions">
            <div className="result-sort-control" aria-label="Sort results">
              <span>Sort by</span>
              <button
                className={sortMode === "compatibility" ? "active" : ""}
                type="button"
                onClick={() => setSortMode("compatibility")}
              >
                Compatibility
              </button>
              <button
                className={sortMode === "datePosted" ? "active" : ""}
                type="button"
                onClick={() => setSortMode("datePosted")}
              >
                Date posted
              </button>
            </div>
            <button className="view-statistics-button" type="button" onClick={onViewStatistics}>
              <BarChart3 size={18} strokeWidth={1.9} />
              View statistics
            </button>
          </div>
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

function SalaryPredictionCard({ loading, prediction, resultCount }) {
  if (loading) {
    return (
      <article className="salary-prediction-card salary-prediction-loading" aria-label="Loading estimated salary range">
        <div>
          <span>Austrian market salary estimate</span>
          <strong>Calculating salary range...</strong>
          <p>Job results are ready. The salary estimate is loading separately.</p>
        </div>
      </article>
    );
  }

  if (!prediction?.available) return null;

  const min = formatEuro(prediction.predictedMinSalary, prediction.currency);
  const max = formatEuro(prediction.predictedMaxSalary, prediction.currency);
  const profileCompleteness = Number(prediction.profileCompleteness) || 0;
  const zeroListingsNote = Number(resultCount) === 0
    ? " No active matching listings were found, so this estimate uses historical market data."
    : "";
  const marketAssumptionNote = prediction.marketAssumed
    ? " Austria was assumed because no country was provided."
    : "";

  return (
    <article className="salary-prediction-card" aria-label="Predicted salary range">
      <div>
        <span>Austrian market salary estimate</span>
        <strong>{min} - {max}</strong>
        <p>
          Estimated from Austrian market data for the detected role, location, skills and experience.
          {marketAssumptionNote}
          {zeroListingsNote}
        </p>
      </div>
      <aside>
        <em>{profileCompleteness}%</em>
        <span>Profile completeness</span>
        <small>Salary estimate only. Actual offers may vary.</small>
      </aside>
    </article>
  );
}

function formatEuro(value, currency = "EUR") {
  const number = Number(value);
  if (!Number.isFinite(number)) return "n/a";

  return `${number.toLocaleString("sl-SI", { maximumFractionDigits: 0 })} ${currency || "EUR"}`;
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

function isPlaceholderValue(value) {
  if (value === null || value === undefined) return true;

  const normalized = String(value).trim().toLowerCase();
  if (!normalized) return true;
  if (normalized.startsWith("unknown")) return true;

  return [
    "not specified",
    "no data",
    "podjetje ni navedeno",
    "source not available",
  ].includes(normalized);
}

function hasDisplayValue(value) {
  return !isPlaceholderValue(value);
}

function formatLocationParts(job) {
  return [job.city, job.country].filter(hasDisplayValue).join(", ");
}

function JobCard({ job, index, accent, mode, onOpen, onToggleCompare, selectedForCompare }) {
  const company = hasDisplayValue(job.company) ? job.company : "";
  const location = formatLocationParts(job);
  const metaItems = [
    location ? <><MapPin size={13} /> {location}</> : null,
    hasDisplayValue(job.mode) ? job.mode : null,
    hasDisplayValue(job.level) ? job.level : null,
    job.postedDate ? `Posted ${formatPostedDate(job.postedDate)}` : null,
  ].filter(Boolean);

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
      <div className="job-logo">{initials(company || job.title)}</div>
      <div className="job-main">
        {company ? <span>{company}</span> : null}
        <h3>{job.title}</h3>
        {metaItems.length ? (
          <p>
            {metaItems.map((item, itemIndex) => (
              <span key={itemIndex}>
                {itemIndex ? <i></i> : null}
                {item}
              </span>
            ))}
          </p>
        ) : null}
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

function sortJobs(jobs, sortMode) {
  const indexedJobs = jobs.map((job, index) => ({ job, index }));

  indexedJobs.sort((left, right) => {
    if (sortMode === "datePosted") {
      const dateDiff = dateValue(right.job.postedDate) - dateValue(left.job.postedDate);
      if (dateDiff !== 0) return dateDiff;
    }

    const matchDiff = (Number(right.job.match) || 0) - (Number(left.job.match) || 0);
    if (matchDiff !== 0) return matchDiff;
    return left.index - right.index;
  });

  return indexedJobs.map(({ job }) => job);
}

function dateValue(value) {
  if (!value) return 0;
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatPostedDate(value) {
  const parsed = Date.parse(value);
  if (!Number.isFinite(parsed)) return value;
  return new Intl.DateTimeFormat("sl-SI", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(parsed));
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
  const jobSkills = Array.isArray(job.skills) && job.skills.length ? job.skills : job.tags;
  const company = hasDisplayValue(job.company) ? job.company : "";
  const location = formatLocationParts(job);
  const detailsMeta = [
    location,
    hasDisplayValue(job.mode) ? job.mode : "",
    hasDisplayValue(job.level) ? job.level : "",
  ].filter(Boolean);
  const showEducation = hasDisplayValue(job.educationLevel);
  const detailsRef = useRef(null);
  const descriptionRef = useRef(null);
  const [descriptionExpanded, setDescriptionExpanded] = useState(false);
  const [descriptionCanExpand, setDescriptionCanExpand] = useState(false);
  const descriptionText = job.description || "No description is available for this listing.";
  const shouldShowDescriptionToggle = descriptionCanExpand || descriptionText.length > 260;

  useEffect(() => {
    detailsRef.current?.scrollTo({ top: 0, left: 0, behavior: "auto" });
    setDescriptionExpanded(false);
    setDescriptionCanExpand(false);
  }, [job?.id]);

  useLayoutEffect(() => {
    if (descriptionExpanded) return;

    const description = descriptionRef.current;
    if (!description) return;

    setDescriptionCanExpand(description.scrollHeight > description.clientHeight + 1);
  }, [job?.id, job?.description, descriptionExpanded]);

  return (
    <div className="job-details-backdrop" role="dialog" aria-modal="true" onClick={onClose}>
      <article className="job-details-page" ref={detailsRef} onClick={(event) => event.stopPropagation()}>
        <button className="job-details-close" type="button" onClick={onClose} aria-label="Close job details">
          <X size={22} strokeWidth={1.8} />
        </button>
        <header>
          <div className="job-logo large">{initials(company || job.title)}</div>
          <div>
            {company ? <span>{company}</span> : null}
            <h2>{job.title}</h2>
            {detailsMeta.length ? (
              <div className="job-details-meta">
                {detailsMeta.map((item) => <b key={item}>{item}</b>)}
              </div>
            ) : null}
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
          {jobSkills.map((tag) => <em key={tag}>{tag}</em>)}
        </div>

        <section>
          <span>Job description</span>
          <p ref={descriptionRef} className={descriptionExpanded ? "expanded" : "collapsed"}>
            {descriptionText}
          </p>
          {shouldShowDescriptionToggle ? (
            <button
              className="description-toggle-button"
              type="button"
              onClick={() => setDescriptionExpanded((expanded) => !expanded)}
            >
              {descriptionExpanded ? "See less" : "See more"}
            </button>
          ) : null}
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
              {jobSkills.map((skill) => <li key={skill}><BriefcaseBusiness size={16} /> {skill}</li>)}
            </ul>
          </section>
        </div>

        <dl>
          <div>
            <dt>Salary</dt>
            <dd>{job.salary}</dd>
          </div>
          {showEducation ? (
            <div>
              <dt>Education</dt>
              <dd>{job.educationLevel}</dd>
            </div>
          ) : null}
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
