import { type CSSProperties, type FormEvent, useMemo, useState } from 'react'
import { BusinessMap, businessCountries, businessJobs } from './BusinessApp'

const navLinks = [
  ['Future', '/'],
  ['Business', '/business.html'],
  ['Insight', '/insight.html'],
  ['Prompt', '/motion-prompt.html'],
  ['CV', '/motion-cv.html'],
]

const liveSignals = [
  ['227', 'open roles', 'Europe-wide live pool'],
  ['28', 'cities', 'Marked on the map'],
  ['94%', 'CV fit', 'Dummy matching model'],
  ['18', 'priority', 'Apply this week'],
]

const statCards = [
  {
    title: 'Najbolj zazelene vescine',
    value: 'React + AI UX',
    detail: 'TypeScript, prompt workflows, design systems and performance lead the shortlist.',
    tone: 'cyan',
  },
  {
    title: 'Najbolj iskane vloge',
    value: 'Developers',
    detail: 'Developers first, then designers, chefs, therapists and product operators.',
    tone: 'pink',
  },
  {
    title: 'Delovna mesta po regijah',
    value: 'Berlin / London',
    detail: 'Berlin, London, Paris, Amsterdam and Zurich carry the strongest density.',
    tone: 'lime',
  },
  {
    title: 'Raven izkusenj',
    value: 'Senior-heavy',
    detail: 'Senior and lead roles dominate the current dummy market distribution.',
    tone: 'amber',
  },
]

const roleMix = [
  ['Developers', 94, '#69f5ff'],
  ['Designers', 36, '#ff6fb7'],
  ['Chefs', 18, '#ffd166'],
  ['Therapists', 12, '#8ef0a7'],
]

type MotionMode = 'idle' | 'search' | 'cv'

type MotionAppProps = {
  resultMode?: Exclude<MotionMode, 'idle'>
}

function MotionResultCard({ mode, score }: { mode: Exclude<MotionMode, 'idle'>; score: number }) {
  return (
    <aside className={`motion-result-card motion-result-card-${mode}`}>
      <span>{mode === 'cv' ? 'CV result route' : 'Prompt result route'}</span>
      <strong>{score}%</strong>
      <h2>{mode === 'cv' ? 'CV fit matrix is already active.' : 'Prompt intent is already active.'}</h2>
      <p>
        {mode === 'cv'
          ? 'This page behaves like the post-upload state: jobs are ranked by CV match, with market signals and map context.'
          : 'This page behaves like the post-prompt state: jobs are ranked by search intent, with stats and city demand below.'}
      </p>
      <div>
        <a href="/motion-prompt.html">Prompt result</a>
        <a href="/motion-cv.html">CV result</a>
      </div>
    </aside>
  )
}

function MotionApp({ resultMode }: MotionAppProps = {}) {
  const isResultPage = Boolean(resultMode)
  const [query, setQuery] = useState('Find senior frontend roles with motion, product ownership and EU relocation')
  const [mode, setMode] = useState<MotionMode>(resultMode ?? 'idle')
  const [cvName, setCvName] = useState('')

  const cities = useMemo(
    () =>
      businessCountries.flatMap((country) =>
        country.cities.map((city) => ({
          ...city,
          country: country.code,
          countryName: country.name,
          color: country.color,
        })),
      ),
    [],
  )

  const topCities = useMemo(() => [...cities].sort((a, b) => b.jobs - a.jobs).slice(0, 7), [cities])
  const cityMax = Math.max(...topCities.map((city) => city.jobs))

  const submitPrompt = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMode('search')
    window.location.href = '/motion-prompt.html'
  }

  const uploadCv = (fileName: string) => {
    if (!fileName) return
    setCvName(fileName)
    setMode('cv')
    window.setTimeout(() => {
      window.location.href = '/motion-cv.html'
    }, isResultPage ? 0 : 140)
  }

  const modeLabel = mode === 'cv' ? 'CV pulse active' : mode === 'search' ? 'Prompt pulse active' : 'Prototype idle'
  const score = mode === 'cv' ? 96 : mode === 'search' ? 91 : 88
  const activeResultMode: Exclude<MotionMode, 'idle'> = mode === 'cv' ? 'cv' : 'search'

  return (
    <main className={`motion-shell mode-${mode} ${isResultPage ? `motion-result-page motion-result-${activeResultMode}` : ''}`}>
      <div className="motion-bg" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <aside className="motion-rail">
        <a className="motion-mark" href="/">
          <b>J</b>
          <span>JobPilot</span>
        </a>
        <nav aria-label="Design versions">
          {navLinks.map(([label, href]) => (
            <a href={href} key={label}>{label}</a>
          ))}
        </nav>
        <div className="rail-meter">
          <span>Signal</span>
          <strong>{score}%</strong>
        </div>
      </aside>

      <section className="motion-stage">
        <header className="motion-topline">
          <div>
            <span>Motion Lab</span>
            <strong>{modeLabel}</strong>
          </div>
          <div className="ticker">
            <span>Berlin 18</span>
            <span>London 21</span>
            <span>Paris 19</span>
            <span>Amsterdam 15</span>
            <span>Zurich 11</span>
          </div>
        </header>

        <section className={`motion-hero ${isResultPage ? 'motion-result-hero' : ''}`}>
          <div className="motion-copy">
            <span className="motion-kicker">{isResultPage ? (activeResultMode === 'cv' ? 'CV match output' : 'Prompt match output') : 'Not an everyday job board'}</span>
            <h1>
              {isResultPage ? (
                activeResultMode === 'cv' ? (
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
                  <span>Command</span>
                  <span>the market</span>
                  <span>like live data.</span>
                </>
              )}
            </h1>
            <p>
              {isResultPage
                ? 'Direct result page with the animated job stack, score orbit, Europe map, city bars and stats already switched on.'
                : 'Same product concept, completely different skin: animated job stack, CV pulse, map radar and stats dashboard in one kinetic workspace.'}
            </p>
          </div>

          {isResultPage ? (
            <MotionResultCard mode={activeResultMode} score={score} />
          ) : (
            <form className="motion-command" onSubmit={submitPrompt}>
              <div className="command-glow"></div>
              <label>Search prompt</label>
              <textarea value={query} onChange={(event) => setQuery(event.target.value)} rows={5} />
              <div className="motion-actions">
                <label className={`motion-upload ${cvName ? 'ready' : ''}`}>
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx"
                    onChange={(event) => uploadCv(event.target.files?.[0]?.name ?? '')}
                  />
                  <span>{cvName ? 'CV locked' : 'Upload CV'}</span>
                  <strong>{cvName || 'PDF / DOCX'}</strong>
                </label>
                <button type="submit">
                  <span>{mode === 'idle' ? 'Launch pulse' : 'Refresh pulse'}</span>
                  <i></i>
                </button>
              </div>
            </form>
          )}
        </section>

        <section className="signal-strip" aria-label="Live signals">
          {liveSignals.map(([value, label, detail], index) => (
            <article key={label} style={{ animationDelay: `${index * 90}ms` }}>
              <strong>{value}</strong>
              <span>{label}</span>
              <p>{detail}</p>
            </article>
          ))}
        </section>

        <section className="motion-grid">
          <section className="motion-panel job-panel">
            <div className="panel-head">
              <div>
                <span>{mode === 'cv' ? 'CV-ranked roles' : 'Pulse-ranked roles'}</span>
                <h2>{businessJobs.length} roles falling into place</h2>
              </div>
              <b>{score}%</b>
            </div>

            <div className="job-stream">
              {businessJobs.map((job, index) => (
                <article
                  className="motion-job"
                  key={job.id}
                  style={{ '--accent': roleMix[index % roleMix.length][2], animationDelay: `${index * 95}ms` } as CSSProperties}
                >
                  <div className="job-index">{String(index + 1).padStart(2, '0')}</div>
                  <div>
                    <span>{job.company}</span>
                    <h3>{job.title}</h3>
                    <p>{job.city}, {job.country} - {job.mode} - {job.level}</p>
                  </div>
                  <aside>
                    <strong>{mode === 'cv' ? job.match : Math.min(job.match + 1, 99)}%</strong>
                    <span>{job.salary}</span>
                  </aside>
                  <footer>
                    {job.tags.map((tag) => <em key={tag}>{tag}</em>)}
                  </footer>
                </article>
              ))}
            </div>
          </section>

          <aside className="motion-panel score-panel">
            <div className="score-orbit">
              <span></span>
              <strong>{score}</strong>
              <small>{mode === 'cv' ? 'CV match' : 'market fit'}</small>
            </div>
            <div className="role-bars">
              {roleMix.map(([label, value, color]) => (
                <div key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                  <i style={{ '--bar': `${Number(value)}%`, '--color': color } as CSSProperties}></i>
                </div>
              ))}
            </div>
          </aside>
        </section>

        <section className="motion-map-panel" id="map">
          <div className="panel-head">
            <div>
              <span>Europe radar</span>
              <h2>Zoom from countries into city demand.</h2>
            </div>
            <p>Scroll/touchpad zoom is intentionally calmer here too, and the map keeps the amCharts interaction.</p>
          </div>
          <BusinessMap countries={businessCountries} cities={cities} />
        </section>

        <section className="city-equalizer">
          {topCities.map((city, index) => (
            <article key={`${city.country}-${city.name}`} style={{ animationDelay: `${index * 80}ms` }}>
              <span>{city.name}</span>
              <div>
                <i style={{ width: `${Math.max(18, (city.jobs / cityMax) * 100)}%` }}></i>
              </div>
              <strong>{city.jobs}</strong>
            </article>
          ))}
        </section>

        <section className="motion-stats">
          {statCards.map((card, index) => (
            <article className={`tone-${card.tone}`} key={card.title} style={{ animationDelay: `${index * 110}ms` }}>
              <span>{card.title}</span>
              <strong>{card.value}</strong>
              <p>{card.detail}</p>
            </article>
          ))}
        </section>
      </section>
    </main>
  )
}

export default MotionApp
