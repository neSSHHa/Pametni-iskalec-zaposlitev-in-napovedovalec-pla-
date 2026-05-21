import {
  type CSSProperties,
  type FormEvent,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import * as am5 from '@amcharts/amcharts5'
import * as am5map from '@amcharts/amcharts5/map'
import am5themes_Animated from '@amcharts/amcharts5/themes/Animated'
import am5geodata_worldLow from '@amcharts/amcharts5-geodata/worldLow'
import './App.css'

type Job = {
  id: number
  role: string
  company: string
  city: string
  country: string
  salary: string
  match: number
  tags: string[]
  mode: string
  accent: string
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

type DesignVariant = 'aurora' | 'graphite' | 'sunrise' | 'arctic'

type ResultMode = 'prompt' | 'cv'

type StatItem = {
  label: string
  value: string
  detail: string
  tone?: string
}

const europeCountryIds = [
  'AL', 'AD', 'AT', 'BY', 'BE', 'BA', 'BG', 'HR', 'CY', 'CZ', 'DK', 'EE', 'FI',
  'FR', 'DE', 'GR', 'HU', 'IS', 'IE', 'IT', 'XK', 'LV', 'LI', 'LT', 'LU', 'MT',
  'MD', 'MC', 'ME', 'NL', 'MK', 'NO', 'PL', 'PT', 'RO', 'RU', 'SM', 'RS', 'SK',
  'SI', 'ES', 'SE', 'CH', 'TR', 'UA', 'GB', 'VA',
]

const jobs: Job[] = [
  {
    id: 1,
    role: 'Senior React Engineer',
    company: 'Northstar AI',
    city: 'Berlin',
    country: 'Germany',
    salary: '82k - 105k EUR',
    match: 98,
    tags: ['React', 'TypeScript', 'AI tools'],
    mode: 'Hybrid',
    accent: '#58c7ff',
  },
  {
    id: 2,
    role: 'Frontend Platform Lead',
    company: 'LedgerFlow',
    city: 'Amsterdam',
    country: 'Netherlands',
    salary: '92k - 118k EUR',
    match: 96,
    tags: ['Design systems', 'Vite', 'DX'],
    mode: 'Remote',
    accent: '#45d6a3',
  },
  {
    id: 3,
    role: 'Product UI Engineer',
    company: 'Atlas Health',
    city: 'Zurich',
    country: 'Switzerland',
    salary: '115k - 142k CHF',
    match: 94,
    tags: ['Motion', 'UX', 'Data UI'],
    mode: 'On-site',
    accent: '#f9bd55',
  },
  {
    id: 4,
    role: 'AI Workflow Designer',
    company: 'PromptWorks',
    city: 'Paris',
    country: 'France',
    salary: '76k - 96k EUR',
    match: 93,
    tags: ['LLM UX', 'React', 'Research'],
    mode: 'Hybrid',
    accent: '#ff7a90',
  },
  {
    id: 5,
    role: 'Staff Frontend Engineer',
    company: 'Cobalt Cloud',
    city: 'Stockholm',
    country: 'Sweden',
    salary: '860k - 1.1m SEK',
    match: 91,
    tags: ['Architecture', 'WebGL', 'Perf'],
    mode: 'Remote',
    accent: '#a48cff',
  },
  {
    id: 6,
    role: 'Creative Technologist',
    company: 'Signal Studio',
    city: 'Barcelona',
    country: 'Spain',
    salary: '68k - 88k EUR',
    match: 90,
    tags: ['Animation', 'Three.js', 'CSS'],
    mode: 'Hybrid',
    accent: '#ff8f4f',
  },
  {
    id: 7,
    role: 'React Native Product Engineer',
    company: 'Orbit Money',
    city: 'London',
    country: 'United Kingdom',
    salary: '88k - 120k GBP',
    match: 89,
    tags: ['React Native', 'Fintech', 'Mobile UX'],
    mode: 'Hybrid',
    accent: '#58c7ff',
  },
  {
    id: 8,
    role: 'Frontend Performance Engineer',
    company: 'Velvet Metrics',
    city: 'Munich',
    country: 'Germany',
    salary: '86k - 112k EUR',
    match: 88,
    tags: ['Performance', 'Bundling', 'Core Web Vitals'],
    mode: 'Remote',
    accent: '#45d6a3',
  },
  {
    id: 9,
    role: 'Design Systems Engineer',
    company: 'Blueframe',
    city: 'Paris',
    country: 'France',
    salary: '72k - 94k EUR',
    match: 87,
    tags: ['Tokens', 'Storybook', 'Accessibility'],
    mode: 'Hybrid',
    accent: '#ff7a90',
  },
  {
    id: 10,
    role: 'WebGL Interface Engineer',
    company: 'Nord Labs',
    city: 'Stockholm',
    country: 'Sweden',
    salary: '780k - 980k SEK',
    match: 86,
    tags: ['Three.js', 'Shaders', 'Interaction'],
    mode: 'Remote',
    accent: '#a48cff',
  },
  {
    id: 11,
    role: 'Senior Frontend Consultant',
    company: 'Sava Digital',
    city: 'Belgrade',
    country: 'Serbia',
    salary: '55k - 78k EUR',
    match: 85,
    tags: ['React', 'Mentoring', 'Architecture'],
    mode: 'Remote',
    accent: '#f9bd55',
  },
  {
    id: 12,
    role: 'Dashboard UX Engineer',
    company: 'Helio Grid',
    city: 'Madrid',
    country: 'Spain',
    salary: '64k - 84k EUR',
    match: 84,
    tags: ['Dashboards', 'Charts', 'UX'],
    mode: 'Hybrid',
    accent: '#ff8f4f',
  },
]

const countries: Country[] = [
  {
    code: 'DE',
    mapId: 'DE',
    name: 'Germany',
    jobs: 42,
    lat: 51.1657,
    lng: 10.4515,
    color: '#45d6a3',
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
    color: '#58c7ff',
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
    color: '#ff7a90',
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
    color: '#ff8f4f',
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
    color: '#f8f5df',
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
    color: '#a48cff',
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
    color: '#8de0b4',
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
    color: '#9edbff',
    cities: [
      { name: 'Belgrade', jobs: 9, lat: 44.7866, lng: 20.4489 },
      { name: 'Novi Sad', jobs: 4, lat: 45.2671, lng: 19.8335 },
    ],
  },
]

const designVariants: { id: DesignVariant; name: string; note: string }[] = [
  { id: 'aurora', name: 'Aurora', note: 'dark AI dashboard' },
  { id: 'graphite', name: 'Graphite', note: 'serious enterprise' },
  { id: 'sunrise', name: 'Sunrise', note: 'warm recruiting' },
  { id: 'arctic', name: 'Arctic', note: 'clean data product' },
]

const skillStats: StatItem[] = [
  { label: 'React / TypeScript', value: '68%', detail: 'najcesce trazen stack', tone: '#45d6a3' },
  { label: 'AI workflow UX', value: '42%', detail: 'brzo raste u product timovima', tone: '#58c7ff' },
  { label: 'Design systems', value: '39%', detail: 'platform i scale-up oglasi', tone: '#f9bd55' },
  { label: 'Web performance', value: '31%', detail: 'senior frontend signali', tone: '#ff7a90' },
]

const roleStats: StatItem[] = [
  { label: 'Razvijalci', value: '94', detail: 'frontend, mobile, platform', tone: '#45d6a3' },
  { label: 'Dizajnerji', value: '36', detail: 'product, UX, systems', tone: '#a48cff' },
  { label: 'Kuharji', value: '18', detail: 'hotel, venue, seasonal', tone: '#ff8f4f' },
  { label: 'Terapevti', value: '12', detail: 'clinic, wellness, remote intake', tone: '#58c7ff' },
]

const cityStats: StatItem[] = [
  { label: 'Berlin', value: '18', detail: 'AI produkti i SaaS', tone: '#45d6a3' },
  { label: 'London', value: '21', detail: 'fintech i platform teams', tone: '#8de0b4' },
  { label: 'Amsterdam', value: '15', detail: 'remote-first kompanije', tone: '#58c7ff' },
  { label: 'Paris', value: '19', detail: 'design-led product orgs', tone: '#ff7a90' },
]

const experienceStats: StatItem[] = [
  { label: 'Junior', value: '16%', detail: 'entry i internship', tone: '#9edbff' },
  { label: 'Mid', value: '34%', detail: '2-5 godina iskustva', tone: '#58c7ff' },
  { label: 'Senior', value: '41%', detail: 'ownership i mentoring', tone: '#45d6a3' },
  { label: 'Lead', value: '9%', detail: 'architecture i people scope', tone: '#f9bd55' },
]

const searchStats: StatItem[] = [
  { label: 'AI match prosek', value: '91%', detail: 'za trazeni prompt', tone: '#45d6a3' },
  { label: 'Remote role', value: '7', detail: 'najbolji fit u shortlist-u', tone: '#58c7ff' },
  { label: 'Salary range', value: '68k+', detail: 'donja granica u Evropi', tone: '#f9bd55' },
  { label: 'Top grad', value: 'Berlin', detail: 'najvise slicnih oglasa', tone: '#ff7a90' },
]

const filters = ['Remote', 'Hybrid', 'Senior', '90k+', 'React', 'Design systems', 'AI tools']

type JobEuropeMapProps = {
  countries: Country[]
  cities: CityPoint[]
  totalJobs: number
}

function JobEuropeMap({ countries, cities, totalJobs }: JobEuropeMapProps) {
  const chartRef = useRef<HTMLDivElement | null>(null)
  const apiRef = useRef<{
    zoomIn: () => void
    zoomOut: () => void
    reset: () => void
  } | null>(null)

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
        maxZoomLevel: 18,
        homeZoomLevel: 2.75,
        homeGeoPoint: { latitude: 52, longitude: 13 },
        centerMapOnZoomOut: true,
      }),
    )

    chart.chartContainer.get('background')?.setAll({
      fill: am5.color(0x8fb8d6),
      fillOpacity: 0,
    })

    const polygonSeries = chart.series.push(
      am5map.MapPolygonSeries.new(root, {
        geoJSON: am5geodata_worldLow,
        include: europeCountryIds,
      }),
    )

    polygonSeries.mapPolygons.template.setAll({
      tooltipText: '{name}',
      interactive: true,
      cursorOverStyle: 'pointer',
      fill: am5.color(0xdfe7ea),
      stroke: am5.color(0x1a2a32),
      strokeWidth: 0.7,
      templateField: 'polygonSettings',
    })

    polygonSeries.mapPolygons.template.states.create('hover', {
      fill: am5.color(0xffffff),
      stroke: am5.color(0x071116),
      strokeWidth: 1.2,
    })

    polygonSeries.mapPolygons.template.states.create('active', {
      stroke: am5.color(0x071116),
      strokeWidth: 1.8,
    })

    polygonSeries.data.setAll(
      countries.map((country) => ({
        id: country.mapId,
        name: country.name,
        code: country.code,
        jobs: country.jobs,
        polygonSettings: {
          fill: am5.color(country.color),
          fillOpacity: 0.92,
        },
      })),
    )

    const countryPointSeries = chart.series.push(
      am5map.MapPointSeries.new(root, {
        latitudeField: 'lat',
        longitudeField: 'lng',
      }),
    )

    countryPointSeries.bullets.push((root, _series, dataItem) => {
      const context = dataItem.dataContext as Country
      const container = am5.Container.new(root, {
        centerX: am5.p50,
        centerY: am5.p50,
        cursorOverStyle: 'pointer',
        tooltipText: `${context.name}: ${context.jobs} poslova`,
      })

      container.children.push(
        am5.Circle.new(root, {
          radius: 19,
          fill: am5.color(context.color),
          fillOpacity: 0.95,
          stroke: am5.color(0x071116),
          strokeWidth: 2,
        }),
      )

      container.children.push(
        am5.Label.new(root, {
          text: '{jobs}',
          populateText: true,
          centerX: am5.p50,
          centerY: am5.p50,
          fontSize: 13,
          fontWeight: '900',
          fill: am5.color(0x071116),
        }),
      )

      container.events.on('click', () => {
        chart.zoomToGeoPoint({ latitude: context.lat, longitude: context.lng }, 6.2, true, 900)
      })

      return am5.Bullet.new(root, { sprite: container })
    })

    countryPointSeries.data.setAll(countries)

    const cityPointSeries = chart.series.push(
      am5map.MapPointSeries.new(root, {
        latitudeField: 'lat',
        longitudeField: 'lng',
      }),
    )

    cityPointSeries.bullets.push((root, _series, dataItem) => {
      const context = dataItem.dataContext as CityPoint
      const container = am5.Container.new(root, {
        centerX: am5.p50,
        centerY: am5.p50,
        cursorOverStyle: 'pointer',
        tooltipText: `${context.name}, ${context.countryName}: ${context.jobs} poslova`,
      })

      container.children.push(
        am5.Circle.new(root, {
          radius: 15,
          fill: am5.color(context.color),
          fillOpacity: 0.96,
          stroke: am5.color(0x071116),
          strokeWidth: 2,
        }),
      )

      container.children.push(
        am5.Label.new(root, {
          text: '{jobs}',
          populateText: true,
          centerX: am5.p50,
          centerY: am5.p50,
          fontSize: 11,
          fontWeight: '900',
          fill: am5.color(0x071116),
        }),
      )

      return am5.Bullet.new(root, { sprite: container })
    })

    cityPointSeries.data.setAll(cities)
    cityPointSeries.hide(0)

    let showingCities = false

    const showCities = (countryCode?: string) => {
      const nextCities = countryCode ? cities.filter((city) => city.country === countryCode) : cities

      cityPointSeries.data.setAll(nextCities)
      cityPointSeries.show(280)
      countryPointSeries.hide(220)
      showingCities = true
    }

    const showCountries = () => {
      countryPointSeries.show(280)
      cityPointSeries.hide(220)
      showingCities = false
    }

    polygonSeries.mapPolygons.template.events.on('click', (event) => {
      const dataItem = event.target.dataItem
      const context = dataItem?.dataContext as { id?: string } | undefined
      const country = countries.find((item) => item.mapId === context?.id)

      polygonSeries.mapPolygons.each((polygon) => polygon.set('active', false))
      event.target.set('active', true)

      if (dataItem) {
        polygonSeries.zoomToDataItem(dataItem as Parameters<typeof polygonSeries.zoomToDataItem>[0], false)
      }

      if (country) {
        window.setTimeout(() => {
          chart.zoomToGeoPoint({ latitude: country.lat, longitude: country.lng }, 6.1, true, 600)
          showCities(country.code)
        }, 260)
      }
    })

    chart.on('zoomLevel', (zoomLevel) => {
      const level = zoomLevel ?? 0

      if (level > 5 && !showingCities) {
        showCities()
      }

      if (level < 3.25 && showingCities) {
        polygonSeries.mapPolygons.each((polygon) => polygon.set('active', false))
        showCountries()
      }
    })

    const chartNode = chartRef.current
    const wheelState = { delta: 0, lastZoomAt: 0, resetTimer: 0 }
    const handleWheel = (event: WheelEvent) => {
      if ((event.target as HTMLElement).closest('.map-controls')) return

      event.preventDefault()
      window.clearTimeout(wheelState.resetTimer)

      const normalizedDelta = Math.sign(event.deltaY) * Math.min(Math.abs(event.deltaY), 120)

      wheelState.delta += normalizedDelta

      const now = Date.now()

      wheelState.resetTimer = window.setTimeout(() => {
        wheelState.delta = 0
      }, 180)

      if (Math.abs(wheelState.delta) < 90 || now - wheelState.lastZoomAt < 90) {
        return
      }

      const currentZoom = chart.get('zoomLevel') ?? chart.get('homeZoomLevel') ?? 2.75
      const zoomFactor = Math.exp((-wheelState.delta / 420) * Math.log(2))
      const nextZoom = Math.min(
        Math.max(currentZoom * zoomFactor, 2.25),
        18,
      )
      const bounds = chartNode.getBoundingClientRect()

      chart.zoomToPoint(
        {
          x: event.clientX - bounds.left,
          y: event.clientY - bounds.top,
        },
        nextZoom,
        false,
        180,
      )

      wheelState.delta = 0
      wheelState.lastZoomAt = now
    }

    chartNode.addEventListener('wheel', handleWheel, { passive: false })

    polygonSeries.events.on('datavalidated', () => {
      chart.goHome(0)
    })

    apiRef.current = {
      zoomIn: () => chart.zoomIn(),
      zoomOut: () => chart.zoomOut(),
      reset: () => {
        polygonSeries.mapPolygons.each((polygon) => polygon.set('active', false))
        showCountries()
        chart.goHome(700)
      },
    }

    chart.appear(900, 120)

    return () => {
      window.clearTimeout(wheelState.resetTimer)
      chartNode.removeEventListener('wheel', handleWheel)
      apiRef.current = null
      root.dispose()
    }
  }, [cities, countries])

  return (
    <div className="europe-map chart-map">
      <div className="map-controls">
        <button type="button" onClick={() => apiRef.current?.zoomIn()} aria-label="Zoom in">
          +
        </button>
        <button type="button" onClick={() => apiRef.current?.zoomOut()} aria-label="Zoom out">
          -
        </button>
        <button type="button" onClick={() => apiRef.current?.reset()}>
          Europe
        </button>
      </div>
      <div className="map-hint">Klik drzave ili scroll zoom</div>
      <div ref={chartRef} className="am-map-chart"></div>
      <div className="map-status">
        <span>{totalJobs} poslova</span>
        <span>{cities.length} gradova</span>
        <span>country to city zoom</span>
      </div>
    </div>
  )
}

function DesignSwitcher({
  value,
  onChange,
}: {
  value: DesignVariant
  onChange: (variant: DesignVariant) => void
}) {
  return (
    <div className="design-switcher" aria-label="Design variants">
      {designVariants.map((variant) => (
        <button
          key={variant.id}
          className={variant.id === value ? 'active' : ''}
          type="button"
          onClick={() => onChange(variant.id)}
          title={variant.note}
        >
          <span>{variant.name}</span>
        </button>
      ))}
    </div>
  )
}

function StatGroup({ title, items }: { title: string; items: StatItem[] }) {
  return (
    <section className="stat-group">
      <div className="stat-group-head">
        <span></span>
        <h3>{title}</h3>
      </div>
      <div className="stat-list">
        {items.map((item) => (
          <article
            className="stat-card"
            key={item.label}
            style={{ '--stat-tone': item.tone ?? '#45d6a3' } as CSSProperties}
          >
            <strong>{item.value}</strong>
            <span>{item.label}</span>
            <p>{item.detail}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

function StatsDashboard({
  compact = false,
  mode = 'home',
}: {
  compact?: boolean
  mode?: 'home' | 'search' | 'cv'
}) {
  return (
    <section className={`stats-dashboard ${compact ? 'compact' : ''}`}>
      <div className="section-heading">
        <p className="eyebrow">Nadzorna plosca</p>
        <h2>
          {mode === 'home'
            ? 'Statistika trzista pre nego sto posaljes zahtev.'
            : mode === 'cv'
              ? 'Statistika poslova koji lice na ovaj CV.'
              : 'Statistika za trazeni prompt.'}
        </h2>
      </div>
      <div className="stats-grid">
        <StatGroup title="Najbolj zazelene vescine" items={mode === 'home' ? skillStats : searchStats} />
        <StatGroup title="Najbolj iskane vloge" items={roleStats} />
        <StatGroup title="Delovna mesta po mestih/regijah" items={cityStats} />
        <StatGroup title="Raven izkusenj" items={experienceStats} />
      </div>
    </section>
  )
}

function FilterRail() {
  return (
    <div className="filter-rail" aria-label="Prototype filters">
      {filters.map((filter, index) => (
        <button className={index < 3 ? 'active' : ''} key={filter} type="button">
          {filter}
        </button>
      ))}
    </div>
  )
}

function CvMatchPanel({ cvName }: { cvName: string }) {
  return (
    <aside className="cv-match-panel">
      <div className="match-orbit">
        <strong>94%</strong>
        <span>ujemanje</span>
      </div>
      <h3>{cvName || 'CV profil'}</h3>
      <p>Dummy analiza: seniority, stack, salary fit i lokacije su uskladjeni sa shortlist-om.</p>
      <div className="cv-signal-grid">
        {[
          ['Stack fit', '97%'],
          ['Seniority', '91%'],
          ['Remote fit', '88%'],
          ['Salary fit', '84%'],
        ].map(([label, value]) => (
          <span key={label}>
            <b>{value}</b>
            {label}
          </span>
        ))}
      </div>
    </aside>
  )
}

function App() {
  const [prompt, setPrompt] = useState('Daj mi najbolje frontend poslove u Evropi koji odgovaraju mom CV-u')
  const [hasSearched, setHasSearched] = useState(false)
  const [view, setView] = useState<'home' | 'results'>('home')
  const [resultMode, setResultMode] = useState<ResultMode>('prompt')
  const [designVariant, setDesignVariant] = useState<DesignVariant>('aurora')
  const [cvName, setCvName] = useState('')
  const totalJobs = countries.reduce((sum, country) => sum + country.jobs, 0)
  const allCities = useMemo(
    () =>
      countries.flatMap((country) =>
        country.cities.map((city) => ({
          ...city,
          country: country.code,
          countryName: country.name,
          color: country.color,
        })),
      ),
    [],
  )

  const visibleJobs = useMemo(() => (hasSearched ? jobs : jobs.slice(0, 3)), [hasSearched])

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setHasSearched(true)
    setResultMode(cvName ? 'cv' : 'prompt')
    setView('results')
  }

  const uploadCv = (fileName: string) => {
    if (!fileName) return

    setCvName(fileName)
    setResultMode('cv')
    setHasSearched(true)
    window.setTimeout(() => setView('results'), 260)
  }

  return (
    <main className={`app-shell theme-${designVariant} view-${view}`}>
      <div className="nav-bar app-nav">
        <button className="brand-mark" type="button" onClick={() => setView('home')}>
          <span>J</span>
          JobPilot
        </button>
        <DesignSwitcher value={designVariant} onChange={setDesignVariant} />
        <div className="nav-stats" aria-label="Live platform stats">
          <span>{totalJobs} poslova</span>
          <span>{allCities.length} gradova</span>
          <span>AI match</span>
        </div>
      </div>

      {view === 'home' ? (
        <section className="home-page">
          <section className="hero-panel">
            <div className="hero-grid">
              <div className="hero-copy">
                <p className="eyebrow">Smart job discovery</p>
                <h1>Posalji zahtev. Dobij poslove.</h1>
                <p className="hero-text">
                  Testiraj vise vizuelnih pravaca, mapu trzista i AI shortlist pre nego sto backend uopste postoji.
                </p>
              </div>

              <form className="prompt-console" onSubmit={submitSearch}>
                <div className="console-top">
                  <span className="pulse-dot"></span>
                  <span>{cvName ? 'CV signal spreman' : 'Napisi sta trazis'}</span>
                </div>
                <textarea
                  value={prompt}
                  onChange={(event) => setPrompt(event.target.value)}
                  aria-label="Job search prompt"
                  rows={4}
                />
                <div className="console-actions">
                  <label className={`cv-upload ${cvName ? 'has-file' : ''}`}>
                    <input
                      type="file"
                      accept=".pdf,.doc,.docx"
                      onChange={(event) => uploadCv(event.target.files?.[0]?.name ?? '')}
                    />
                    <span className="upload-icon">{cvName ? 'OK' : 'CV'}</span>
                    <span>{cvName || 'Upload CV'}</span>
                  </label>
                  <button type="submit">
                    <span>Pronadji poslove</span>
                    <span className="arrow">{'->'}</span>
                  </button>
                </div>
                <div className="console-tags" aria-label="Search suggestions">
                  {['React', 'Remote', '90k+', 'Design systems'].map((tag) => (
                    <button key={tag} type="button" onClick={() => setPrompt(`${prompt}, ${tag}`)}>
                      {tag}
                    </button>
                  ))}
                </div>
              </form>
            </div>
          </section>

          <section className="map-section home-map" aria-label="Europe job map">
            <div className="section-heading">
              <p className="eyebrow">Europe radar</p>
              <h2>Mapa trzista pre pretrage.</h2>
            </div>
            <div className="map-board">
              <JobEuropeMap countries={countries} cities={allCities} totalJobs={totalJobs} />
            </div>
          </section>

          <StatsDashboard />
        </section>
      ) : (
        <section id="results" className={`results-stage result-${resultMode}`} aria-label="Job results">
          <div className="stage-header">
            <div>
              <p className="eyebrow">{resultMode === 'cv' ? 'CV shortlist' : 'Prompt shortlist'}</p>
              <h2>{resultMode === 'cv' ? 'Poslovi rangirani po ujemanju sa CV-em.' : 'Poslovi za trazeni prompt.'}</h2>
            </div>
            <button className="ghost-action" type="button" onClick={() => setView('home')}>
              Nazad na home
            </button>
            <div className="match-meter">
              <span>{resultMode === 'cv' ? 'CV signal' : 'Search signal'}</span>
              <strong>{resultMode === 'cv' ? '94%' : 'Live'}</strong>
            </div>
          </div>

          <FilterRail />

          <div className="results-grid">
            <div className="job-stack">
              {visibleJobs.map((job, index) => (
                <article
                  className="job-card"
                  key={job.id}
                  style={{ '--delay': `${index * 70}ms`, '--accent': job.accent, '--score': job.match } as CSSProperties}
                >
                  <div className="card-glow"></div>
                  <div className="job-main">
                    <div>
                      <span className="company">{job.company}</span>
                      <h3>{job.role}</h3>
                      <p>{job.city}, {job.country} - {job.mode}</p>
                    </div>
                    <div className="score-ring">
                      <span>{resultMode === 'cv' ? job.match : Math.min(job.match + 1, 99)}</span>
                      <small>%</small>
                    </div>
                  </div>
                  <div className="salary-row">
                    <strong>{job.salary}</strong>
                    <span>{resultMode === 'cv' ? 'CV fit' : 'Prompt fit'}</span>
                  </div>
                  <div className="tag-row">
                    {job.tags.map((tag) => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                </article>
              ))}
            </div>

            {resultMode === 'cv' ? (
              <div className="result-side-stack">
                <CvMatchPanel cvName={cvName} />
                <StatsDashboard compact mode="cv" />
              </div>
            ) : (
              <StatsDashboard compact mode="search" />
            )}
          </div>

          <section className="map-section results-map" aria-label="Result map">
            <div className="section-heading">
              <p className="eyebrow">Geo fit</p>
              <h2>{resultMode === 'cv' ? 'Mapa gradova za CV shortlist.' : 'Mapa za trazeni prompt.'}</h2>
            </div>
            <div className="map-board">
              <JobEuropeMap countries={countries} cities={allCities} totalJobs={totalJobs} />
            </div>
          </section>
        </section>
      )}
    </main>
  )
}

export default App
