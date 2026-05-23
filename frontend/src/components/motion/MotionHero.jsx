import { Send, Sparkles, Upload, Zap } from "lucide-react";

const processingModes = [
  ["fast", Zap, "Instant", "hitro"],
  ["thinking", Sparkles, "Thinking", "AI"],
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
          {resultPage ? (activeResultMode === "cv" ? "CV rezultati" : "Prompt rezultati") : "Pametni iskalec zaposlitev"}
        </span>
        <h1>
          {resultPage ? (
            activeResultMode === "cv" ? (
              <>
                <span>CV rezultati.</span>
              </>
            ) : (
              <>
                <span>Prompt rezultati.</span>
              </>
            )
          ) : (
            <>
              <span>Najdi delo,</span>
              <span className="hero-emphasis">ki ti res ustreza.</span>
            </>
          )}
        </h1>
        <p>
          {resultPage
            ? "Rezultati so razvrsceni po kompatibilnosti, z dodatno statistiko trga."
            : "Nalozi CV ali opisi zelje v naravnem jeziku. Sistem pripravi filter, izracuna kompatibilnost in prikaze najbolj primerne oglase."}
        </p>
      </div>

      {resultPage && !loading ? null : (
        <form className="motion-command" onSubmit={onPromptSubmit}>
          {status && mode !== "idle" ? <p className="motion-status">{status}</p> : null}
          {error ? <p className="motion-error">{error}</p> : null}

          <div className="motion-mode-switch" role="group" aria-label="Processing mode">
            {processingModes.map(([value, Icon, label, detail]) => (
              <button
                key={value}
                type="button"
                className={processingMode === value ? "active" : ""}
                onClick={() => onProcessingModeChange(value)}
              >
                <Icon aria-hidden="true" size={20} strokeWidth={1.9} />
                <span>{label}</span>
                <small>{detail}</small>
              </button>
            ))}
          </div>

          <div className="entry-card-grid">
            <article className="entry-card">
              <span>Iz zivljenjepisa</span>
              <h2>Nalozi CV</h2>
              <p>Sistem izlusci vescine, izkusnje in preference, nato razvrsti primerne oglase.</p>
              <label className={`motion-upload ${cvName ? "ready" : ""}`}>
                <input
                  type="file"
                  accept=".pdf,.doc,.docx"
                  onChange={(event) => onCvUpload(event.target.files?.[0])}
                />
                <span className="upload-icon">
                  <Upload aria-hidden="true" size={38} strokeWidth={1.8} />
                </span>
                <strong>{cvName || "Povleci PDF ali klikni za izbiro"}</strong>
                <small>{cvName ? "CV je pripravljen za analizo" : "PDF, DOCX"}</small>
              </label>
              <button className="cv-submit" type="button" disabled={loading}>
                <Upload aria-hidden="true" size={18} strokeWidth={1.9} />
                <span>{loading ? "Analiziram..." : cvName ? "CV nalozen" : "Analiziraj CV"}</span>
              </button>
            </article>

            <article className="entry-card">
              <span>Iz opisa</span>
              <h2>Vpisi prompt</h2>
              <p>Opisi, kaksno delo isces, brez filtrov in dropdownov.</p>
              <textarea
                value={query}
                onChange={(event) => onQueryChange(event.target.value)}
                placeholder="Npr. Iscem remote frontend vlogo v Ljubljani, 3+ leta izkusenj z Reactom..."
                rows={5}
              />
              <button className="prompt-submit" type="submit" disabled={loading || !query.trim()}>
                <Send aria-hidden="true" size={18} strokeWidth={1.9} />
                <span>{loading ? "Obdelujem..." : "Poslji poizvedbo"}</span>
              </button>
            </article>
          </div>
        </form>
      )}
    </section>
  );
}
