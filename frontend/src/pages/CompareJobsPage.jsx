import { ArrowLeft, BriefcaseBusiness, GraduationCap, MapPin, Scale, Sparkles, Trash2 } from "lucide-react";
import { useState } from "react";
import { JobDetailsModal } from "../components/motion/MotionJobsPanel.jsx";
import MotionShell from "../components/motion/MotionShell.jsx";
import { useComparison } from "../context/ComparisonContext.jsx";

function navigate(href) {
  window.history.pushState({}, "", href);
  window.dispatchEvent(new Event("jobradar:navigate"));
}

function isPlaceholderValue(value) {
  if (value === null || value === undefined) return true;

  const normalized = String(value).trim().toLowerCase();
  if (!normalized) return true;
  if (normalized.startsWith("unknown")) return true;

  return [
    "unknown",
    "not specified",
    "salary n/a",
    "no data",
    "podjetje ni navedeno",
    "source not available",
  ].includes(normalized);
}

function displayValue(value) {
  if (isPlaceholderValue(value)) {
    return "No data";
  }

  return String(value);
}

function hasValue(value) {
  return displayValue(value) !== "No data";
}

function formatScore(value) {
  const number = Number(value);
  return Number.isFinite(number) ? `${Math.round(number)}%` : "No data";
}

function formatSalaryRange(job) {
  const min = Number(job?.salaryMin);
  const max = Number(job?.salaryMax);
  const hasMin = Number.isFinite(min) && min > 0;
  const hasMax = Number.isFinite(max) && max > 0;

  if (!hasMin && !hasMax) return "No data";
  if (hasMin && hasMax) return `${min.toLocaleString("sl-SI")} - ${max.toLocaleString("sl-SI")} EUR`;
  return `${(hasMin ? min : max).toLocaleString("sl-SI")} EUR`;
}

function formatLocationParts(job) {
  return [job?.city, job?.country].filter(hasValue).join(", ");
}

function uniqueSkills(values = []) {
  return [...new Set(values.filter(Boolean).map((value) => String(value).trim()).filter(Boolean))];
}

function normalizeSkill(value) {
  return String(value || "").trim().toLowerCase();
}

function skillGroups(leftJob, rightJob) {
  const left = uniqueSkills(leftJob?.tags || []);
  const right = uniqueSkills(rightJob?.tags || []);
  const rightSet = new Set(right.map(normalizeSkill));
  const leftSet = new Set(left.map(normalizeSkill));

  return {
    shared: left.filter((skill) => rightSet.has(normalizeSkill(skill))),
    onlyLeft: left.filter((skill) => !rightSet.has(normalizeSkill(skill))),
    onlyRight: right.filter((skill) => !leftSet.has(normalizeSkill(skill))),
  };
}

export default function CompareJobsPage() {
  const comparison = useComparison();
  const [theme, setTheme] = useState(() => localStorage.getItem("smartjobs-theme") || "light");
  const [detailsJob, setDetailsJob] = useState(null);
  const [leftJob, rightJob] = comparison.jobs;
  const resultsPath = leftJob?.compareSourcePath || rightJob?.compareSourcePath || "/motion-prompt";
  const groups = skillGroups(leftJob, rightJob);
  const showSkills = Boolean(groups.shared.length || groups.onlyLeft.length || groups.onlyRight.length);

  const toggleTheme = () => {
    setTheme((currentTheme) => {
      const nextTheme = currentTheme === "light" ? "dark" : "light";
      localStorage.setItem("smartjobs-theme", nextTheme);
      return nextTheme;
    });
  };

  const clearAndGoHome = () => {
    comparison.clearJobs();
    navigate("/motion");
  };

  return (
    <MotionShell mode="compare" score={0} theme={theme} onThemeToggle={toggleTheme} onHomeClick={() => navigate("/motion")}>
      <section className="compare-page compact">
        <header className="compare-hero compact">
          <button className="compare-back-button" type="button" onClick={() => navigate(resultsPath)}>
            <ArrowLeft size={18} strokeWidth={1.9} />
            Results
          </button>
          <div>
            <span className="motion-kicker">Job comparison</span>
            <h1>Compare jobs</h1>
          </div>
          {comparison.jobs.length ? (
            <button className="compare-clear-button" type="button" onClick={clearAndGoHome}>
              <Trash2 size={17} strokeWidth={1.9} />
              Clear and home
            </button>
          ) : null}
        </header>

        {!comparison.jobs.length ? (
          <EmptyCompareState />
        ) : (
          <>
            <div className="compare-card-grid">
              <CompareJobCard job={leftJob} slot="A" onOpenDetails={() => setDetailsJob(leftJob)} onRemove={() => comparison.removeJob(leftJob?.id)} />
              {rightJob ? (
                <CompareJobCard job={rightJob} slot="B" onOpenDetails={() => setDetailsJob(rightJob)} onRemove={() => comparison.removeJob(rightJob?.id)} />
              ) : (
                <article className="compare-job-card compare-placeholder-card compact">
                  <Scale size={30} strokeWidth={1.7} />
                  <h2>Add one more job</h2>
                  <p>Select another result to compare side by side.</p>
                  <button type="button" onClick={() => navigate(resultsPath)}>Back to results</button>
                </article>
              )}
            </div>

            {rightJob ? (
              <div className="compare-sections">
                <CompareSection title="Key differences" icon={<Sparkles size={20} />}>
                  <CompareRow label="Match" left={formatScore(leftJob.match)} right={formatScore(rightJob.match)} highlight />
                  <CompareRow label="Location" left={`${leftJob.city}, ${leftJob.country}`} right={`${rightJob.city}, ${rightJob.country}`} />
                  <CompareRow label="Work type" left={leftJob.mode} right={rightJob.mode} />
                  <CompareRow label="Level" left={leftJob.level} right={rightJob.level} />
                  <CompareRow label="Education" left={leftJob.educationLevel} right={rightJob.educationLevel} />
                  <CompareRow label="Salary" left={formatSalaryRange(leftJob)} right={formatSalaryRange(rightJob)} keepWhenEmpty />
                </CompareSection>

                {showSkills ? (
                  <CompareSection title="Skills" icon={<Scale size={20} />}>
                    <SkillCompareBlock title="Shared" skills={groups.shared} />
                    <SkillCompareBlock title="Only job A" skills={groups.onlyLeft} />
                    <SkillCompareBlock title="Only job B" skills={groups.onlyRight} />
                  </CompareSection>
                ) : null}

                <CompareSection title="Details" icon={<BriefcaseBusiness size={20} />}>
                  <CompareRow label="Company" left={leftJob.company} right={rightJob.company} />
                  <CompareRow label="Confidence" left={formatScore(leftJob.confidence)} right={formatScore(rightJob.confidence)} />
                </CompareSection>

                <CompareSection title="Description" icon={<GraduationCap size={20} />}>
                  <CompareRow label="Summary" left={leftJob.description || "No data"} right={rightJob.description || "No data"} long />
                </CompareSection>
              </div>
            ) : null}
          </>
        )}
      </section>
      {detailsJob ? (
        <JobDetailsModal
          job={detailsJob}
          filterRequest={null}
          onClose={() => setDetailsJob(null)}
          onToggleCompare={() => comparison.toggleJob({ ...detailsJob, compareSourcePath: resultsPath })}
          selectedForCompare={comparison.isSelected(detailsJob.id)}
        />
      ) : null}
    </MotionShell>
  );
}

function EmptyCompareState() {
  return (
    <article className="compare-empty-card">
      <Scale size={42} strokeWidth={1.6} />
      <h2>No jobs selected</h2>
      <p>Choose up to two jobs from the results list.</p>
      <button type="button" onClick={() => navigate("/motion-prompt")}>Go to results</button>
    </article>
  );
}

function CompareJobCard({ job, slot, onOpenDetails, onRemove }) {
  if (!job) return null;

  const company = hasValue(job.company) ? job.company : "";
  const location = formatLocationParts(job);
  const metaItems = [
    location ? <><MapPin size={15} /> {location}</> : null,
    hasValue(job.mode) ? job.mode : null,
    hasValue(job.level) ? job.level : null,
  ].filter(Boolean);

  return (
    <article className="compare-job-card">
      <header>
        <span>{slot}</span>
        <button type="button" onClick={onRemove} aria-label="Remove from comparison">
          <Trash2 size={17} strokeWidth={1.9} />
        </button>
      </header>
      <button className="compare-job-title-button" type="button" onClick={onOpenDetails}>
        {job.title}
      </button>
      {company ? <p>{company}</p> : null}
      {metaItems.length ? (
        <div>
          {metaItems.map((item, index) => <span key={index}>{item}</span>)}
        </div>
      ) : null}
      <strong>{formatScore(job.match)}</strong>
      <small>Match</small>
    </article>
  );
}

function CompareSection({ title, icon, children }) {
  return (
    <section className="compare-section">
      <h2>{icon}{title}</h2>
      {children}
    </section>
  );
}

function CompareRow({ label, left, right, long = false, highlight = false, keepWhenEmpty = false }) {
  const leftValue = displayValue(left);
  const rightValue = displayValue(right);

  if (!keepWhenEmpty && leftValue === "No data" && rightValue === "No data") return null;

  const same = leftValue.toLowerCase() === rightValue.toLowerCase();

  return (
    <div className={`compare-row ${long ? "long" : ""} ${highlight ? "highlight" : ""} ${same ? "same" : "different"}`}>
      <span>{label}</span>
      <p data-side="Job A">{leftValue}</p>
      <p data-side="Job B">{rightValue}</p>
    </div>
  );
}

function SkillCompareBlock({ title, skills }) {
  if (!skills.length) return null;

  return (
    <div className="skill-compare-block">
      <span>{title}</span>
      <div>
        {skills.map((skill) => <em key={skill}>{skill}</em>)}
      </div>
    </div>
  );
}
