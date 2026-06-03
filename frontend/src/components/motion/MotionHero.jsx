import { Brain, CheckCircle2, FileUp, Scale, Search, Target, Zap, Sparkles } from "lucide-react";

const processingModes = [
  ["fast", Zap, "Instant", "fast"],
  ["thinking", Sparkles, "Thinking", "AI"],
];

export default function MotionHero({
  mode,
  query,
  cvName,
  resultPage,
  loading,
  status,
  error,
  processingMode,
  onProcessingModeChange,
  onQueryChange,
  onPromptSubmit,
  onCvUpload,
}) {
  const activeResultMode = mode === "cv" ? "cv" : "search";

  if (resultPage) {
    return (
      <section className="motion-hero motion-result-hero">
        <div className="motion-copy">
          <span className="motion-kicker">{activeResultMode === "cv" ? "CV results" : "Prompt results"}</span>
          <h1><span>{activeResultMode === "cv" ? "CV results." : "Prompt results."}</span></h1>
          <p>Results are ranked by compatibility.</p>
        </div>
      </section>
    );
  }

  return (
    <section className="motion-hero home-hero">
      <div className="motion-copy">
        <div className="radar-bg" aria-hidden="true">
          <span className="radar-dot d1"></span>
          <span className="radar-dot d2"></span>
          <span className="radar-dot d3"></span>
          <span className="radar-dot d4"></span>
          <span className="radar-dot d5"></span>
        </div>
        <h1>Upload your CV or write a prompt.</h1>
        <p>Get matched job listings right away.</p>
      </div>

      <div className="home-mode-switch" role="group" aria-label="Processing mode">
        {processingModes.map(([value, Icon, label, detail]) => (
          <button
            key={value}
            type="button"
            className={processingMode === value ? "active" : ""}
            onClick={() => onProcessingModeChange(value)}
          >
            <Icon size={14} strokeWidth={1.9} />
            <span>{label}</span>
            <small>{detail}</small>
          </button>
        ))}
      </div>

      <form className="motion-command search-panel" onSubmit={onPromptSubmit}>
        {status && mode !== "idle" ? <p className="motion-status">{status}</p> : null}
        {error ? <p className="motion-error">{error}</p> : null}

        <div className="panel-side">
          <div className="tab-title">
            <FileUp size={24} strokeWidth={1.9} />
            <span>Upload CV</span>
          </div>
          <label className={`upload-box ${cvName ? "ready" : ""}`}>
            <input
              type="file"
              accept=".pdf,.doc,.docx"
              onChange={(event) => onCvUpload(event.target.files?.[0])}
            />
            <span className="file-icon" aria-hidden="true">
              <span className="file-lines"><i></i><i></i><i></i></span>
            </span>
            <strong>{cvName || "Drag your CV here"}</strong>
            <span className="upload-or">or</span>
            <span className="btn-primary">{cvName ? "CV selected" : "Upload CV"}</span>
            <small>{cvName ? "Document is ready for analysis" : "PDF, DOCX supported"}</small>
          </label>
          <p className="europass-helper">
            No CV yet? Create one with the{" "}
            <a
              href="https://europa.eu/europass/eportfolio/screen/cv-editor?lang=en&previous=https:%2F%2Feuropa.eu%2Feuropass%2Fen"
              target="_blank"
              rel="noreferrer"
            >
              Europass CV builder
            </a>.
          </p>
        </div>

        <div className="panel-side">
          <div className="tab-title">
            <Search size={24} strokeWidth={1.9} />
            <span>Prompt Search</span>
          </div>
          <div className="prompt-box">
            <h3>Describe what you want</h3>
            <p>Add details so the system can return better matches.</p>
            <textarea
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder="E.g. I am looking for a junior .NET role in Maribor, remote or hybrid, salary from 1800 EUR, 2+ years of experience, C++, SQL..."
            />
            <button className="search-btn" type="submit" disabled={loading || !query.trim()}>
              <Search size={21} strokeWidth={2} />
              <span>{loading ? "Processing..." : "Search jobs"}</span>
            </button>
          </div>
        </div>
      </form>

      <h2 className="section-title">How it works</h2>
      <section className="steps">
        <StepCard number="1" icon={<FileUp size={38} />} title="Upload a CV or write a prompt" text="Add your CV or describe the ideal job you want." />
        <StepCard number="2" icon={<Brain size={38} />} title="AI analysis" text="The system analyzes your experience, skills and preferences." />
        <StepCard number="3" icon={<Target size={38} />} title="Job matching" text="You get a list of roles that best fit your profile." />
        <StepCard number="4" icon={<Scale size={38} />} title="Compare opportunities" text="Save and compare several jobs in one place." />
      </section>

      <section className="mission">
        <div className="mission-content">
          <div className="eyebrow">Our goal</div>
          <h2>More than a regular job search</h2>
          <ul className="check-list">
            <li><CheckCircle2 size={18} /> Remove endless scrolling through irrelevant listings</li>
            <li><CheckCircle2 size={18} /> Use AI to understand what the candidate really wants</li>
            <li><CheckCircle2 size={18} /> Connect CV, preferences and the labor market in one system</li>
            <li><CheckCircle2 size={18} /> Enable transparent job comparison</li>
            <li><CheckCircle2 size={18} /> Help people find better jobs faster</li>
          </ul>
          <button className="outline-btn" type="button">Learn more about our mission</button>
        </div>

        <div className="mission-visual" aria-hidden="true">
          <div className="float-badge one">Save time</div>
          <div className="float-badge two">Smart analysis</div>
          <div className="float-badge three">Transparent</div>
          <div className="phone-card">
            <div className="mini-radar"></div>
          </div>
        </div>
      </section>
    </section>
  );
}

function StepCard({ number, icon, title, text }) {
  return (
    <article className="step-card">
      <div className="step-number">{number}</div>
      <div className="step-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{text}</p>
    </article>
  );
}
