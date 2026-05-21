import { type FormEvent, useLayoutEffect, useMemo, useRef, useState } from 'react'
import * as am5 from '@amcharts/amcharts5'
import * as am5map from '@amcharts/amcharts5/map'
import am5themes_Animated from '@amcharts/amcharts5/themes/Animated'
import am5geodata_worldLow from '@amcharts/amcharts5-geodata/worldLow'

type Job = {
  id: number
  title: string
  company: string
  city: string
  country: string
  salary: string
  match: number
  level: string
  mode: string
  tags: string[]
}

type Country = {
  code: string
  mapId: string
  name: string
  jobs: number
  lat: number
  lng: number
  color: string
  cities: { name: string; jobs: number; lat: number; lng: number }[]
}

type CityPoint = {
  name: string
  jobs: number
  lat: number
  lng: number
  country: string
  countryName: string
  color: string
}

type ResultMode = 'search' | 'cv'

const europeIds = [
  'AL', 'AD', 'AT', 'BY', 'BE', 'BA', 'BG', 'HR', 'CY', 'CZ', 'DK', 'EE', 'FI',
  'FR', 'DE', 'GR', 'HU', 'IS', 'IE', 'IT', 'XK', 'LV', 'LI', 'LT', 'LU', 'MT',
  'MD', 'MC', 'ME', 'NL', 'MK', 'NO', 'PL', 'PT', 'RO', 'RU', 'SM', 'RS', 'SK',
  'SI', 'ES', 'SE', 'CH', 'TR', 'UA', 'GB', 'VA',
]

export const businessCountries: Country[] = [
  {
    code: 'DE',
    mapId: 'DE',
    name: 'Germany',
    jobs: 42,
    lat: 51.1657,
    lng: 10.4515,
    color: '#0a66c2',
    cities: [
      { name: 'Berlin', jobs: 18, lat: 52.52, lng: 13.405 },
      { name: 'Munich', jobs: 11, lat: 48.1351, lng: 11.582 },
      { name: 'Hamburg', jobs: 8, lat: 53.5511, lng: 9.9937 },
    ],
  },
  {
    code: 'NL',
    mapId: 'NL',
    name: 'Netherlands',
    jobs: 26,
    lat: 52.1326,
    lng: 5.2913,
    color: '#2f8dd3',
    cities: [
      { name: 'Amsterdam', jobs: 15, lat: 52.3676, lng: 4.9041 },
      { name: 'Rotterdam', jobs: 7, lat: 51.9244, lng: 4.4777 },
    ],
  },
  {
    code: 'FR',
    mapId: 'FR',
    name: 'France',
    jobs: 35,
    lat: 46.2276,
    lng: 2.2137,
    color: '#4b6f92',
    cities: [
      { name: 'Paris', jobs: 19, lat: 48.8566, lng: 2.3522 },
      { name: 'Lyon', jobs: 8, lat: 45.764, lng: 4.8357 },
      { name: 'Nice', jobs: 4, lat: 43.7102, lng: 7.262 },
    ],
  },
  {
    code: 'ES',
    mapId: 'ES',
    name: 'Spain',
    jobs: 22,
    lat: 40.4637,
    lng: -3.7492,
    color: '#be7c2f',
    cities: [
      { name: 'Madrid', jobs: 10, lat: 40.4168, lng: -3.7038 },
      { name: 'Barcelona', jobs: 8, lat: 41.3874, lng: 2.1686 },
    ],
  },
  {
    code: 'CH',
    mapId: 'CH',
    name: 'Switzerland',
    jobs: 18,
    lat: 46.8182,
    lng: 8.2275,
    color: '#56616f',
    cities: [
      { name: 'Zurich', jobs: 11, lat: 47.3769, lng: 8.5417 },
      { name: 'Geneva', jobs: 5, lat: 46.2044, lng: 6.1432 },
    ],
  },
  {
    code: 'SE',
    mapId: 'SE',
    name: 'Sweden',
    jobs: 31,
    lat: 60.1282,
    lng: 18.6435,
    color: '#6d62ad',
    cities: [
      { name: 'Stockholm', jobs: 17, lat: 59.3293, lng: 18.0686 },
      { name: 'Gothenburg', jobs: 8, lat: 57.7089, lng: 11.9746 },
    ],
  },
  {
    code: 'UK',
    mapId: 'GB',
    name: 'United Kingdom',
    jobs: 39,
    lat: 55.3781,
    lng: -3.436,
    color: '#1f8f78',
    cities: [
      { name: 'London', jobs: 21, lat: 51.5072, lng: -0.1276 },
      { name: 'Manchester', jobs: 9, lat: 53.4808, lng: -2.2426 },
    ],
  },
  {
    code: 'RS',
    mapId: 'RS',
    name: 'Serbia',
    jobs: 14,
    lat: 44.0165,
    lng: 21.0059,
    color: '#3387a8',
    cities: [
      { name: 'Belgrade', jobs: 9, lat: 44.7866, lng: 20.4489 },
      { name: 'Novi Sad', jobs: 4, lat: 45.2671, lng: 19.8335 },
    ],
  },
]

export const businessJobs: Job[] = [
  {
    id: 1,
    title: 'Senior React Engineer',
    company: 'Northstar AI',
    city: 'Berlin',
    country: 'Germany',
    salary: '82k - 105k EUR',
    match: 98,
    level: 'Senior',
    mode: 'Hybrid',
    tags: ['React', 'TypeScript', 'AI tools'],
  },
  {
    id: 2,
    title: 'Frontend Platform Lead',
    company: 'LedgerFlow',
    city: 'Amsterdam',
    country: 'Netherlands',
    salary: '92k - 118k EUR',
    match: 96,
    level: 'Lead',
    mode: 'Remote',
    tags: ['Design systems', 'Vite', 'DX'],
  },
  {
    id: 3,
    title: 'Product UI Engineer',
    company: 'Atlas Health',
    city: 'Zurich',
    country: 'Switzerland',
    salary: '115k - 142k CHF',
    match: 94,
    level: 'Senior',
    mode: 'On-site',
    tags: ['Motion', 'UX', 'Data UI'],
  },
  {
    id: 4,
    title: 'AI Workflow Designer',
    company: 'PromptWorks',
    city: 'Paris',
    country: 'France',
    salary: '76k - 96k EUR',
    match: 93,
    level: 'Mid-Senior',
    mode: 'Hybrid',
    tags: ['LLM UX', 'Research', 'React'],
  },
  {
    id: 5,
    title: 'Staff Frontend Engineer',
    company: 'Cobalt Cloud',
    city: 'Stockholm',
    country: 'Sweden',
    salary: '860k - 1.1m SEK',
    match: 91,
    level: 'Staff',
    mode: 'Remote',
    tags: ['Architecture', 'WebGL', 'Perf'],
  },
  {
    id: 6,
    title: 'Dashboard UX Engineer',
    company: 'Helio Grid',
    city: 'Madrid',
    country: 'Spain',
    salary: '64k - 84k EUR',
    match: 84,
    level: 'Mid',
    mode: 'Hybrid',
    tags: ['Dashboards', 'Charts', 'UX'],
  },
]

const skillStats = [
  ['React / TypeScript', '68%', 'Most requested stack'],
  ['AI workflow UX', '42%', 'Fast growing product skill'],
  ['Design systems', '39%', 'Common in scale-up roles'],
  ['Performance', '31%', 'Senior signal'],
]

const roleStats = [
  ['Developers', '94', 'Frontend, mobile, platform'],
  ['Designers', '36', 'Product and systems'],
  ['Chefs', '18', 'Hospitality and venue'],
  ['Therapists', '12', 'Clinic and wellness'],
]

const cityStats = [
  ['London', '21', 'Fintech and platform'],
  ['Paris', '19', 'Design-led orgs'],
  ['Berlin', '18', 'AI products'],
  ['Amsterdam', '15', 'Remote-first teams'],
]

const experienceStats = [
  ['Junior', '16%', 'Entry-level'],
  ['Mid', '34%', '2-5 years'],
  ['Senior', '41%', 'Ownership roles'],
  ['Lead', '9%', 'Architecture scope'],
]

export function BusinessMap({
  countries,
  cities,
}: {
  countries: Country[]
  cities: CityPoint[]
}) {
  const chartRef = useRef<HTMLDivElement | null>(null)
  const apiRef = useRef<{ reset: () => void; zoomIn: () => void; zoomOut: () => void } | null>(null)

  useLayoutEffect(() => {
    if (!chartRef.current) return undefined

    const root = am5.Root.new(chartRef.current)
    root.setThemes([am5themes_Animated.new(root)])

    const chart = root.container.children.push(
      am5map.MapChart.new(root, {
        projection: am5map.geoMercator(),
        panX: 'translateX',
        panY: 'translateY',
        wheelY: 'none',
        wheelX: 'none',
        pinchZoom: true,
        minZoomLevel: 2.25,
        maxZoomLevel: 16,
        homeZoomLevel: 2.75,
        homeGeoPoint: { latitude: 52, longitude: 13 },
      }),
    )

    const polygonSeries = chart.series.push(
      am5map.MapPolygonSeries.new(root, {
        geoJSON: am5geodata_worldLow,
        include: europeIds,
      }),
    )

    polygonSeries.mapPolygons.template.setAll({
      tooltipText: '{name}',
      interactive: true,
      cursorOverStyle: 'pointer',
      fill: am5.color(0xe7edf3),
      stroke: am5.color(0xaebdca),
      strokeWidth: 0.8,
      templateField: 'polygonSettings',
    })

    polygonSeries.mapPolygons.template.states.create('hover', {
      fill: am5.color(0xffffff),
      stroke: am5.color(0x0a66c2),
      strokeWidth: 1.4,
    })

    polygonSeries.data.setAll(
      countries.map((country) => ({
        id: country.mapId,
        name: country.name,
        polygonSettings: {
          fill: am5.color(country.color),
          fillOpacity: 0.86,
        },
      })),
    )

    const countryPoints = chart.series.push(
      am5map.MapPointSeries.new(root, {
        latitudeField: 'lat',
        longitudeField: 'lng',
      }),
    )

    countryPoints.bullets.push((root, _series, dataItem) => {
      const context = dataItem.dataContext as Country
      const container = am5.Container.new(root, {
        centerX: am5.p50,
        centerY: am5.p50,
        cursorOverStyle: 'pointer',
        tooltipText: `${context.name}: ${context.jobs} jobs`,
      })

      container.children.push(
        am5.Circle.new(root, {
          radius: 18,
          fill: am5.color(0xffffff),
          stroke: am5.color(context.color),
          strokeWidth: 3,
        }),
      )

      container.children.push(
        am5.Label.new(root, {
          text: '{jobs}',
          populateText: true,
          centerX: am5.p50,
          centerY: am5.p50,
          fontWeight: '800',
          fontSize: 12,
          fill: am5.color(0x17212b),
        }),
      )

      container.events.on('click', () => {
        chart.zoomToGeoPoint({ latitude: context.lat, longitude: context.lng }, 6, true, 720)
      })

      return am5.Bullet.new(root, { sprite: container })
    })

    countryPoints.data.setAll(countries)

    const cityPoints = chart.series.push(
      am5map.MapPointSeries.new(root, {
        latitudeField: 'lat',
        longitudeField: 'lng',
      }),
    )

    cityPoints.bullets.push((root, _series, dataItem) => {
      const context = dataItem.dataContext as CityPoint
      const container = am5.Container.new(root, {
        centerX: am5.p50,
        centerY: am5.p50,
        tooltipText: `${context.name}: ${context.jobs} jobs`,
      })

      container.children.push(
        am5.Circle.new(root, {
          radius: 14,
          fill: am5.color(context.color),
          stroke: am5.color(0xffffff),
          strokeWidth: 3,
        }),
      )

      container.children.push(
        am5.Label.new(root, {
          text: '{jobs}',
          populateText: true,
          centerX: am5.p50,
          centerY: am5.p50,
          fontSize: 11,
          fontWeight: '800',
          fill: am5.color(0xffffff),
        }),
      )

      return am5.Bullet.new(root, { sprite: container })
    })

    cityPoints.data.setAll(cities)
    cityPoints.hide(0)

    let cityMode = false
    const showCities = (countryCode?: string) => {
      cityPoints.data.setAll(countryCode ? cities.filter((city) => city.country === countryCode) : cities)
      cityPoints.show(240)
      countryPoints.hide(220)
      cityMode = true
    }

    const showCountries = () => {
      countryPoints.show(240)
      cityPoints.hide(220)
      cityMode = false
    }

    polygonSeries.mapPolygons.template.events.on('click', (event) => {
      const context = event.target.dataItem?.dataContext as { id?: string } | undefined
      const country = countries.find((item) => item.mapId === context?.id)

      if (event.target.dataItem) {
        polygonSeries.zoomToDataItem(
          event.target.dataItem as Parameters<typeof polygonSeries.zoomToDataItem>[0],
          false,
        )
      }

      if (country) {
        window.setTimeout(() => {
          chart.zoomToGeoPoint({ latitude: country.lat, longitude: country.lng }, 6, true, 560)
          showCities(country.code)
        }, 220)
      }
    })

    chart.on('zoomLevel', (zoomLevel) => {
      const level = zoomLevel ?? 0
      if (level > 5 && !cityMode) showCities()
      if (level < 3.25 && cityMode) showCountries()
    })

    const node = chartRef.current
    const wheel = { delta: 0, last: 0, timer: 0 }
    const onWheel = (event: WheelEvent) => {
      if ((event.target as HTMLElement).closest('.map-controls')) return
      event.preventDefault()
      window.clearTimeout(wheel.timer)
      wheel.delta += Math.sign(event.deltaY) * Math.min(Math.abs(event.deltaY), 110)
      wheel.timer = window.setTimeout(() => {
        wheel.delta = 0
      }, 180)

      const now = Date.now()
      if (Math.abs(wheel.delta) < 90 || now - wheel.last < 90) return

      const currentZoom = chart.get('zoomLevel') ?? 2.75
      const nextZoom = Math.min(Math.max(currentZoom * Math.exp((-wheel.delta / 430) * Math.log(2)), 2.25), 16)
      const rect = node.getBoundingClientRect()

      chart.zoomToPoint({ x: event.clientX - rect.left, y: event.clientY - rect.top }, nextZoom, false, 170)
      wheel.delta = 0
      wheel.last = now
    }

    node.addEventListener('wheel', onWheel, { passive: false })

    polygonSeries.events.on('datavalidated', () => chart.goHome(0))
    chart.appear(800, 100)

    apiRef.current = {
      reset: () => {
        showCountries()
        chart.goHome(650)
      },
      zoomIn: () => chart.zoomIn(),
      zoomOut: () => chart.zoomOut(),
    }

    return () => {
      window.clearTimeout(wheel.timer)
      node.removeEventListener('wheel', onWheel)
      apiRef.current = null
      root.dispose()
    }
  }, [cities, countries])

  return (
    <div className="business-map">
      <div className="map-controls">
        <button type="button" onClick={() => apiRef.current?.zoomIn()}>+</button>
        <button type="button" onClick={() => apiRef.current?.zoomOut()}>-</button>
        <button type="button" onClick={() => apiRef.current?.reset()}>Europe</button>
      </div>
      <div className="map-help">Click a country, scroll to zoom</div>
      <div className="map-canvas" ref={chartRef}></div>
    </div>
  )
}

function StatBlock({ title, items }: { title: string; items: string[][] }) {
  return (
    <section className="stat-block">
      <h3>{title}</h3>
      <div>
        {items.map(([label, value, detail]) => (
          <article key={label}>
            <strong>{value}</strong>
            <span>{label}</span>
            <p>{detail}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

function BusinessStats({ compact = false }: { compact?: boolean }) {
  return (
    <section className={`business-stats ${compact ? 'compact' : ''}`}>
      <div className="section-title">
        <span>Market intelligence</span>
        <h2>Nadzorna plosca s statistiko</h2>
      </div>
      <div className="stats-layout">
        <StatBlock title="Najbolj zazelene vescine" items={skillStats} />
        <StatBlock title="Najbolj iskane vloge" items={roleStats} />
        <StatBlock title="Delovna mesta po mestih/regijah" items={cityStats} />
        <StatBlock title="Raven izkusenj" items={experienceStats} />
      </div>
    </section>
  )
}

function BusinessApp() {
  const [view, setView] = useState<'home' | 'results'>('home')
  const [mode, setMode] = useState<ResultMode>('search')
  const [prompt, setPrompt] = useState('Find senior frontend roles in Europe with strong product ownership')
  const [cvName, setCvName] = useState('')

  const totalJobs = businessCountries.reduce((sum, country) => sum + country.jobs, 0)
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

  const submitPrompt = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMode(cvName ? 'cv' : 'search')
    setView('results')
  }

  const uploadCv = (fileName: string) => {
    if (!fileName) return
    setCvName(fileName)
    setMode('cv')
    window.setTimeout(() => setView('results'), 180)
  }

  return (
    <main className="business-shell">
      <header className="business-nav">
        <button type="button" onClick={() => setView('home')}>
          <b>in</b>
          JobPilot Business
        </button>
        <nav>
          <a href="/">Futuristic version</a>
          <a href="/insight.html">Insight version</a>
          <a href="/motion.html">Motion Lab</a>
          <span>{totalJobs} open roles</span>
          <span>{cities.length} hiring cities</span>
        </nav>
      </header>

      {view === 'home' ? (
        <section className="business-home">
          <section className="business-hero">
            <div>
              <span className="kicker">Professional job intelligence</span>
              <h1>One clean workspace for jobs, CV fit, market signals and hiring geography.</h1>
              <p>
                A business-focused prototype with the same product concept: prompt search, CV upload, Europe map and stats.
              </p>
            </div>

            <form className="business-search" onSubmit={submitPrompt}>
              <label>Search prompt</label>
              <textarea value={prompt} onChange={(event) => setPrompt(event.target.value)} rows={5} />
              <div>
                <label className={`business-upload ${cvName ? 'ready' : ''}`}>
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx"
                    onChange={(event) => uploadCv(event.target.files?.[0]?.name ?? '')}
                  />
                  <span>{cvName || 'Upload CV'}</span>
                </label>
                <button type="submit">Find roles</button>
              </div>
            </form>
          </section>

          <section className="business-map-section">
            <div className="section-title">
              <span>Europe radar</span>
              <h2>Hiring map before the search</h2>
            </div>
            <BusinessMap countries={businessCountries} cities={cities} />
          </section>

          <BusinessStats />
        </section>
      ) : (
        <section className="business-results">
          <div className="results-header">
            <div>
              <span className="kicker">{mode === 'cv' ? 'CV matching results' : 'Prompt results'}</span>
              <h1>{mode === 'cv' ? 'Roles ranked by CV fit' : 'Roles matching your search'}</h1>
              <p>{mode === 'cv' ? 'Dummy CV analysis with fit signals and market stats.' : prompt}</p>
            </div>
            <button type="button" onClick={() => setView('home')}>Back to home</button>
          </div>

          <div className="business-result-grid">
            <div className="business-job-list">
              {businessJobs.map((job) => (
                <article key={job.id}>
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
                    {job.tags.map((tag) => <span key={tag}>{tag}</span>)}
                  </footer>
                </article>
              ))}
            </div>

            <aside className="business-side">
              {mode === 'cv' && (
                <section className="cv-score">
                  <strong>94%</strong>
                  <span>CV match</span>
                  <p>{cvName || 'Uploaded CV'} aligns strongly with senior frontend and product UI roles.</p>
                </section>
              )}
              <BusinessStats compact />
            </aside>
          </div>

          <section className="business-map-section">
            <div className="section-title">
              <span>{mode === 'cv' ? 'CV geography' : 'Search geography'}</span>
              <h2>{mode === 'cv' ? 'Cities matching this CV' : 'Cities matching this search'}</h2>
            </div>
            <BusinessMap countries={businessCountries} cities={cities} />
          </section>
        </section>
      )}
    </main>
  )
}

export default BusinessApp
