import { useEffect, useState } from "react";

const accentColors = ["#69f5ff", "#ff6fb7", "#ffd166", "#8ef0a7", "#a78bfa"];
const PAGE_SIZE = 5;

export default function MotionJobsPanel({
  mode,
  score,
  jobs = [],
  totalCount,
  filterRequest,
  hasMore = false,
  loading = false,
  error = "",
  onLoadMore,
}) {
  const displayJobs = jobs;
  const resultCount = totalCount ?? displayJobs.length;
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [selectedJob, setSelectedJob] = useState(null);
  const visibleJobs = displayJobs.slice(0, visibleCount);
  const hiddenLoadedJobs = Math.max(displayJobs.length - visibleCount, 0);
  const remainingJobs = Math.max(resultCount - visibleJobs.length, 0);

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
      <div className="panel-head">
        <div>
          <span>{mode === "cv" ? "CV-ranked roles" : mode === "search" ? "Prompt-ranked roles" : "Vsi oglasi"}</span>
          <h2>{resultCount} {mode === "idle" ? "aktivnih oglasov" : "ujemajocih vlog"}</h2>
        </div>
        <b>{score}%</b>
      </div>
      {loading ? <p className="motion-status">Cakam odgovor API-ja...</p> : null}
      {error ? <p className="motion-error">{error}</p> : null}
      <FilterChips filterRequest={filterRequest} />
      {hiddenLoadedJobs || hasMore ? (
        <LoadMoreButton remainingJobs={remainingJobs} hasMore={hasMore && !hiddenLoadedJobs} onClick={handleLoadMore} />
      ) : null}

      <div className="job-stream">
        {!loading && !displayJobs.length ? (
          <p className="motion-status">Ni oglasov za trenutni izbor.</p>
        ) : null}
        {!loading && displayJobs.length ? (
          <p className="motion-status">Prikazujem {visibleJobs.length} od {resultCount} rezultatov.</p>
        ) : null}
        {visibleJobs.map((job, index) => (
          <article
            className="motion-job"
            key={job.id}
            role="button"
            tabIndex={0}
            onClick={() => setSelectedJob(job)}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") setSelectedJob(job);
            }}
            style={{ "--accent": accentColors[index % accentColors.length], animationDelay: `${index * 95}ms` }}
          >
            <div className="job-index">{String(index + 1).padStart(2, "0")}</div>
            <div>
              <span>{job.company}</span>
              <h3>{job.title}</h3>
              <p>{job.city}, {job.country} - {job.mode} - {job.level}</p>
            </div>
            <aside>
              <strong>{mode === "idle" ? Math.min(job.match + 1, 99) : job.match}%</strong>
              <small>compatibility</small>
              {job.confidence !== null && job.confidence !== undefined ? (
                <small>{job.confidence}% confidence</small>
              ) : null}
              <span>{job.salary}</span>
            </aside>
            <footer>
              {job.tags.map((tag) => <em key={tag}>{tag}</em>)}
            </footer>
          </article>
        ))}
      </div>
      {hiddenLoadedJobs || hasMore ? (
        <LoadMoreButton remainingJobs={remainingJobs} hasMore={hasMore && !hiddenLoadedJobs} onClick={handleLoadMore} />
      ) : null}
      {selectedJob ? <JobDetailsPage job={selectedJob} onClose={() => setSelectedJob(null)} /> : null}
    </section>
  );
}

function FilterChips({ filterRequest }) {
  const chips = filterChips(filterRequest);
  if (!chips.length) return null;

  return (
    <div className="filter-chip-strip" aria-label="Active filters">
      {chips.map((chip) => (
        <span key={`${chip.label}-${chip.value}`}>
          <b>{chip.label}</b>
          {chip.value}
        </span>
      ))}
    </div>
  );
}

function filterChips(filterRequest) {
  if (!filterRequest) return [];

  const job = filterRequest.job || {};
  const location = filterRequest.location || {};
  const chips = [];

  addChip(chips, "Role", job.jobname);
  addChip(chips, "City", location.city);
  addChip(chips, "Country", location.country);
  addChip(chips, "Region", location.region);
  addChip(chips, "Level", job.experienceLevelName);
  addChip(chips, "Education", job.educationLevel);
  addChip(chips, "Experience", job.requiredExperience ? `${job.requiredExperience}y` : "");
  addListChips(chips, "Work", filterRequest.workTypes);
  addListChips(chips, "Skill", filterRequest.skills);

  return chips;
}

function addChip(chips, label, value) {
  if (value === null || value === undefined || value === "") return;
  chips.push({ label, value: String(value) });
}

function addListChips(chips, label, values) {
  if (!Array.isArray(values)) return;
  values.filter(Boolean).forEach((value) => addChip(chips, label, value));
}

function LoadMoreButton({ remainingJobs, hasMore, onClick }) {
  return (
    <button className="job-load-more" type="button" onClick={onClick}>
      Load more
      <span>{hasMore ? "nalozi naslednjo stran" : `${remainingJobs} skritih oglasov`}</span>
    </button>
  );
}

function JobDetailsPage({ job, onClose }) {
  return (
    <div className="job-details-backdrop">
      <article className="job-details-page">
        <header>
          <button className="job-details-close" type="button" onClick={onClose}>Nazaj</button>
          <span>{job.company}</span>
          <h2>{job.title}</h2>
          <div className="job-details-meta">
            <b>{job.match}% compatibility</b>
            {job.confidence !== null && job.confidence !== undefined ? (
              <b>{job.confidence}% confidence</b>
            ) : null}
            <b>{job.city}, {job.country}</b>
            <b>{job.mode}</b>
            <b>{job.level}</b>
          </div>
        </header>
        <div className="job-details-layout">
          <section>
            <span>Opis vloge</span>
            <p>{job.description || "Opis za ta oglas ni naveden."}</p>
            <footer>
              {job.tags.map((tag) => <em key={tag}>{tag}</em>)}
            </footer>
          </section>
          <dl>
            <div>
              <dt>Izobrazba</dt>
              <dd>{job.educationLevel}</dd>
            </div>
            <div>
              <dt>Placa</dt>
              <dd>{job.salary}</dd>
            </div>
            <div>
              <dt>Objavljeno</dt>
              <dd>{job.postedDate || "Ni navedeno"}</dd>
            </div>
            <div>
              <dt>Vir</dt>
              <dd>{job.sourceUrl || "Ni navedeno"}</dd>
            </div>
          </dl>
        </div>
      </article>
    </div>
  );
}
