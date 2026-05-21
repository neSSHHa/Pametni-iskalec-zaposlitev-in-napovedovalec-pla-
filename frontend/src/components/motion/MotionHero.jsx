function MotionResultCard({ mode, score }) {
  return (
    <aside className={`motion-result-card motion-result-card-${mode}`}>
      <span>{mode === "cv" ? "CV result route" : "Prompt result route"}</span>
      <strong>{score}%</strong>
      <h2>{mode === "cv" ? "CV fit matrix is already active." : "Prompt intent is already active."}</h2>
      <p>
        {mode === "cv"
          ? "This page behaves like the post-upload state: jobs are ranked by CV match, with market signals and map context."
          : "This page behaves like the post-prompt state: jobs are ranked by search intent, with stats and city demand below."}
      </p>
      <div>
        <a href="/motion-prompt">Prompt result</a>
        <a href="/motion-cv">CV result</a>
      </div>
    </aside>
  );
}

const processingModes = [
  ["fast", "Fast", "instant"],
  ["thinking", "Thinking", "main AI"],
];

export default function MotionHero({
  mode,
  score,
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

  return (
    <section className={`motion-hero ${resultPage ? "motion-result-hero" : ""}`}>
      <div className="motion-copy">
        <span className="motion-kicker">
          {resultPage ? (activeResultMode === "cv" ? "CV match output" : "Prompt match output") : "Pametni iskalec zaposlitev"}
        </span>
        <h1>
          {resultPage ? (
            activeResultMode === "cv" ? (
              <>
                <span>CV</span>
                <span>match</span>
                <span>ignition.</span>
              </>
            ) : (
              <>
                <span>Prompt</span>
                <span>result</span>
                <span>pulse.</span>
              </>
            )
          ) : (
            <>
              <span>Najdi</span>
              <span>pravo delo</span>
              <span>hitreje.</span>
            </>
          )}
        </h1>
        <p>
          {resultPage
            ? "Rezultati so povezani z backend API-jem, CV/prompt filtrom, zemljevidom povprasevanja in analitiko."
            : "Vnesi prompt ali nalozi CV. Sistem uporabi obstojece API-je, razvrsti oglase in prikaze statistiko trga."}
        </p>
      </div>

      {resultPage && !loading ? (
        <MotionResultCard mode={activeResultMode} score={score} />
      ) : (
        <form className="motion-command" onSubmit={onPromptSubmit}>
          <div className="command-glow"></div>
          <label>Prompt za iskanje</label>
          <textarea
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Npr. junior Java developer remote, React frontend v Ljubljani, medicinska sestra Maribor..."
            rows={5}
          />
          {status ? <p className="motion-status">{status}</p> : null}
          {error ? <p className="motion-error">{error}</p> : null}
          <div className="motion-mode-switch" role="group" aria-label="Processing mode">
            {processingModes.map(([value, label, detail]) => (
              <button
                key={value}
                type="button"
                className={processingMode === value ? "active" : ""}
                onClick={() => onProcessingModeChange(value)}
              >
                <span>{label}</span>
                <small>{detail}</small>
              </button>
            ))}
          </div>
          <div className="motion-actions">
            <label className={`motion-upload ${cvName ? "ready" : ""}`}>
              <input
                type="file"
                accept=".pdf,.doc,.docx"
                onChange={(event) => onCvUpload(event.target.files?.[0])}
              />
              <span>{cvName ? "CV nalozen" : "Nalozi CV"}</span>
              <strong>{cvName || "PDF / DOCX"}</strong>
            </label>
            <button type="submit" disabled={loading}>
              <span>{loading ? "Obdelujem..." : mode === "idle" ? "Poisci" : "Osvezi"}</span>
              <i></i>
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
