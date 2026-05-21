import { type FormEvent, useMemo, useState } from 'react'
import { BusinessMap, businessCountries, businessJobs } from './BusinessApp'

const filters = ['All roles', 'Remote', 'Senior', 'Product UI', '90k+', 'Visa friendly']

const insightMetrics = [
  ['Fit velocity', '91%', 'Prompt and CV signal strength'],
  ['Market heat', 'High', 'Berlin, London, Paris'],
  ['Salary band', '82k+', 'Median shortlisted range'],
  ['Open density', '227', 'Across tracked EU regions'],
]

const pipeline = [
  ['Discover', '227', 'open roles'],
  ['Qualified', '64', 'strong matches'],
  ['Priority', '18', 'apply this week'],
  ['Watchlist', '31', 'track later'],
]

type InsightMode = 'search' | 'cv'

type InsightAppProps = {
  resultMode?: InsightMode
}

function MetricStrip() {
  return (
    <div className="metric-strip">
      {insightMetrics.map(([label, value, detail]) => (
        <article key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
          <p>{detail}</p>
        </article>
      ))}
    </div>
  )
}

function PipelinePanel() {
  return (
    <aside className="pipeline-panel">
      <div className="panel-title">
        <span>Operational view</span>
        <h3>Match pipeline</h3>
      </div>
      <div className="pipeline-list">
        {pipeline.map(([label, value, detail], index) => (
          <div key={label}>
            <i>{index + 1}</i>
            <span>{label}</span>
            <strong>{value}</strong>
            <small>{detail}</small>
          </div>
        ))}
      </div>
    </aside>
  )
}

function ResultSnapshot({ mode }: { mode: InsightMode }) {
  return (
    <aside className={`result-snapshot result-snapshot-${mode}`}>
      <span>{mode === 'cv' ? 'CV result page' : 'Prompt result page'}</span>
      <strong>{mode === 'cv' ? '94%' : 'Live'}</strong>
      <h3>{mode === 'cv' ? 'Roles ranked by CV fit.' : 'Roles ranked by prompt intent.'}</h3>
      <p>
        {mode === 'cv'
          ? 'Dummy CV layer shows fit score, salary range, skills and city density for the uploaded profile.'
          : 'Dummy prompt layer shows all jobs as if they were matched to the requested search intent.'}
      </p>
      <div>
        <a href="/insight-prompt.html">Prompt result</a>
        <a href="/insight-cv.html">CV result</a>
      </div>
    </aside>
  )
}

function InsightApp({ resultMode }: InsightAppProps = {}) {
  const isResultPage = Boolean(resultMode)
  const [query, setQuery] = useState('Senior frontend roles with product ownership in Europe')
  const [mode, setMode] = useState<InsightMode>(resultMode ?? 'search')
  const [cvName, setCvName] = useState('')
  const [activeFilter, setActiveFilter] = useState(filters[0])

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

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const nextMode: InsightMode = 'search'
    setMode(nextMode)
    window.location.href = '/insight-prompt.html'
  }

  const uploadCv = (fileName: string) => {
    if (!fileName) return
    setCvName(fileName)
    setMode('cv')
    window.setTimeout(() => {
      window.location.href = '/insight-cv.html'
    }, isResultPage ? 0 : 140)
  }

  return (
    <main className={`insight-shell ${isResultPage ? `insight-result insight-result-${mode}` : ''}`}>
      <aside className="insight-sidebar">
        <div className="insight-logo">
          <b>JP</b>
          <span>Insight</span>
        </div>
        <nav>
          <a className={!isResultPage ? 'active' : ''} href="/insight.html#overview">Overview</a>
          <a className={isResultPage && mode === 'search' ? 'active' : ''} href="/insight-prompt.html">Prompt result</a>
          <a className={isResultPage && mode === 'cv' ? 'active' : ''} href="/insight-cv.html">CV result</a>
          <a href="#roles">Roles</a>
          <a href="#map">Map</a>
          <a href="#analytics">Analytics</a>
        </nav>
        <div className="version-links">
          <a href="/">Futuristic</a>
          <a href="/business.html">Business</a>
          <a href="/motion.html">Motion Lab</a>
          <a href="/motion-prompt.html">Motion prompt</a>
          <a href="/motion-cv.html">Motion CV</a>
        </div>
      </aside>

      <section className="insight-main">
        <header className="insight-topbar">
          <form className="command-search" onSubmit={submit}>
            <span>Search</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} />
            <button type="submit">Run</button>
          </form>
          <label className={`cv-drop ${cvName ? 'ready' : ''}`}>
            <input
              type="file"
              accept=".pdf,.doc,.docx"
              onChange={(event) => uploadCv(event.target.files?.[0]?.name ?? '')}
            />
            {cvName || 'Upload CV'}
          </label>
        </header>

        <section className="insight-hero" id="overview">
          <div>
            <span className="kicker">{isResultPage ? (mode === 'cv' ? 'CV result workspace' : 'Prompt result workspace') : 'Research-led workspace'}</span>
            <h1>
              {isResultPage
                ? mode === 'cv'
                  ? 'CV match results with market evidence beside every role.'
                  : 'Prompt results shaped like a serious decision workspace.'
                : 'Scan jobs like a product dashboard, not a marketing page.'}
            </h1>
            <p>
              {isResultPage
                ? 'This route opens directly into the result experience, with jobs, filters, stats and map already in context.'
                : 'This version prioritizes search, filters, comparison and live market context. It is built for repeated use.'}
            </p>
          </div>
          {isResultPage ? <ResultSnapshot mode={mode} /> : <PipelinePanel />}
        </section>

        <MetricStrip />

        <section className="workspace-grid">
          <section className="results-workbench" id="roles">
            <div className="filter-row">
              {filters.map((filter) => (
                <button
                  className={filter === activeFilter ? 'active' : ''}
                  key={filter}
                  type="button"
                  onClick={() => setActiveFilter(filter)}
                >
                  {filter}
                </button>
              ))}
            </div>

            <div className="workbench-head">
              <div>
                <span>{mode === 'cv' ? 'CV-ranked shortlist' : 'Prompt-ranked shortlist'}</span>
                <h2>{businessJobs.length} priority roles</h2>
              </div>
              <strong>{mode === 'cv' ? '94% CV match' : 'Live query'}</strong>
            </div>

            <div className="dense-jobs">
              {businessJobs.map((job, index) => (
                <article key={job.id}>
                  <div className="job-rank">{String(index + 1).padStart(2, '0')}</div>
                  <div>
                    <h3>{job.title}</h3>
                    <p>{job.company} - {job.city}, {job.country}</p>
                    <footer>
                      {job.tags.map((tag) => <span key={tag}>{tag}</span>)}
                    </footer>
                  </div>
                  <aside>
                    <strong>{mode === 'cv' ? job.match : Math.min(job.match + 1, 99)}%</strong>
                    <span>{job.salary}</span>
                  </aside>
                </article>
              ))}
            </div>
          </section>

          <aside className="context-column">
            <section className="decision-card">
              <span>Recommended next move</span>
              <h3>{mode === 'cv' ? 'Apply to Berlin and Amsterdam roles first.' : 'Narrow by seniority and salary band.'}</h3>
              <p>Dummy reasoning layer showing where explainable AI output would live.</p>
            </section>
            <section className="mini-analytics" id="analytics">
              {[
                ['Skills', 'React, TS, UX'],
                ['Roles', 'Frontend, Lead'],
                ['Cities', 'Berlin, London'],
                ['Level', 'Senior-heavy'],
              ].map(([label, value]) => (
                <div key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                </div>
              ))}
            </section>
          </aside>
        </section>

        <section className="insight-map-section" id="map">
          <div className="map-head">
            <div>
              <span className="kicker">Geo intelligence</span>
              <h2>Map sits under the decision workspace, not above it.</h2>
            </div>
            <p>Click countries to zoom, scroll for city detail, compare density by marker count.</p>
          </div>
          <BusinessMap countries={businessCountries} cities={cities} />
        </section>
      </section>
    </main>
  )
}

export default InsightApp
