export const europeCountryIds = [
  "AL", "AD", "AT", "BY", "BE", "BA", "BG", "HR", "CY", "CZ", "DK", "EE", "FI",
  "FR", "DE", "GR", "HU", "IS", "IE", "IT", "XK", "LV", "LI", "LT", "LU", "MT",
  "MD", "MC", "ME", "NL", "MK", "NO", "PL", "PT", "RO", "RU", "SM", "RS", "SK",
  "SI", "ES", "SE", "CH", "TR", "UA", "GB", "VA",
];

export const motionCountries = [
  {
    code: "DE",
    mapId: "DE",
    name: "Germany",
    jobs: 42,
    lat: 51.1657,
    lng: 10.4515,
    color: "#69f5ff",
    cities: [
      { name: "Berlin", jobs: 18, lat: 52.52, lng: 13.405 },
      { name: "Munich", jobs: 11, lat: 48.1351, lng: 11.582 },
      { name: "Hamburg", jobs: 8, lat: 53.5511, lng: 9.9937 },
    ],
  },
  {
    code: "NL",
    mapId: "NL",
    name: "Netherlands",
    jobs: 26,
    lat: 52.1326,
    lng: 5.2913,
    color: "#70d6ff",
    cities: [
      { name: "Amsterdam", jobs: 15, lat: 52.3676, lng: 4.9041 },
      { name: "Rotterdam", jobs: 7, lat: 51.9244, lng: 4.4777 },
    ],
  },
  {
    code: "FR",
    mapId: "FR",
    name: "France",
    jobs: 35,
    lat: 46.2276,
    lng: 2.2137,
    color: "#ff6fb7",
    cities: [
      { name: "Paris", jobs: 19, lat: 48.8566, lng: 2.3522 },
      { name: "Lyon", jobs: 8, lat: 45.764, lng: 4.8357 },
      { name: "Nice", jobs: 4, lat: 43.7102, lng: 7.262 },
    ],
  },
  {
    code: "ES",
    mapId: "ES",
    name: "Spain",
    jobs: 22,
    lat: 40.4637,
    lng: -3.7492,
    color: "#ffd166",
    cities: [
      { name: "Madrid", jobs: 10, lat: 40.4168, lng: -3.7038 },
      { name: "Barcelona", jobs: 8, lat: 41.3874, lng: 2.1686 },
    ],
  },
  {
    code: "CH",
    mapId: "CH",
    name: "Switzerland",
    jobs: 18,
    lat: 46.8182,
    lng: 8.2275,
    color: "#f7f8fb",
    cities: [
      { name: "Zurich", jobs: 11, lat: 47.3769, lng: 8.5417 },
      { name: "Geneva", jobs: 5, lat: 46.2044, lng: 6.1432 },
    ],
  },
  {
    code: "SE",
    mapId: "SE",
    name: "Sweden",
    jobs: 31,
    lat: 60.1282,
    lng: 18.6435,
    color: "#a78bfa",
    cities: [
      { name: "Stockholm", jobs: 17, lat: 59.3293, lng: 18.0686 },
      { name: "Gothenburg", jobs: 8, lat: 57.7089, lng: 11.9746 },
    ],
  },
  {
    code: "UK",
    mapId: "GB",
    name: "United Kingdom",
    jobs: 39,
    lat: 55.3781,
    lng: -3.436,
    color: "#8ef0a7",
    cities: [
      { name: "London", jobs: 21, lat: 51.5072, lng: -0.1276 },
      { name: "Manchester", jobs: 9, lat: 53.4808, lng: -2.2426 },
    ],
  },
  {
    code: "RS",
    mapId: "RS",
    name: "Serbia",
    jobs: 14,
    lat: 44.0165,
    lng: 21.0059,
    color: "#7dd3fc",
    cities: [
      { name: "Belgrade", jobs: 9, lat: 44.7866, lng: 20.4489 },
      { name: "Novi Sad", jobs: 4, lat: 45.2671, lng: 19.8335 },
    ],
  },
];

export const motionJobs = [
  {
    id: 1,
    title: "Senior React Engineer",
    company: "Northstar AI",
    city: "Berlin",
    country: "Germany",
    salary: "82k - 105k EUR",
    match: 98,
    level: "Senior",
    mode: "Hybrid",
    tags: ["React", "TypeScript", "AI tools"],
  },
  {
    id: 2,
    title: "Frontend Platform Lead",
    company: "LedgerFlow",
    city: "Amsterdam",
    country: "Netherlands",
    salary: "92k - 118k EUR",
    match: 96,
    level: "Lead",
    mode: "Remote",
    tags: ["Design systems", "Vite", "DX"],
  },
  {
    id: 3,
    title: "Product UI Engineer",
    company: "Atlas Health",
    city: "Zurich",
    country: "Switzerland",
    salary: "115k - 142k CHF",
    match: 94,
    level: "Senior",
    mode: "On-site",
    tags: ["Motion", "UX", "Data UI"],
  },
  {
    id: 4,
    title: "AI Workflow Designer",
    company: "PromptWorks",
    city: "Paris",
    country: "France",
    salary: "76k - 96k EUR",
    match: 93,
    level: "Mid-Senior",
    mode: "Hybrid",
    tags: ["LLM UX", "Research", "React"],
  },
  {
    id: 5,
    title: "Staff Frontend Engineer",
    company: "Cobalt Cloud",
    city: "Stockholm",
    country: "Sweden",
    salary: "860k - 1.1m SEK",
    match: 91,
    level: "Staff",
    mode: "Remote",
    tags: ["Architecture", "WebGL", "Perf"],
  },
  {
    id: 6,
    title: "Dashboard UX Engineer",
    company: "Helio Grid",
    city: "Madrid",
    country: "Spain",
    salary: "64k - 84k EUR",
    match: 84,
    level: "Mid",
    mode: "Hybrid",
    tags: ["Dashboards", "Charts", "UX"],
  },
];

export const liveSignals = [
  ["227", "open roles", "Europe-wide live pool"],
  ["28", "cities", "Marked on the map"],
  ["94%", "CV fit", "Dummy matching model"],
  ["18", "priority", "Apply this week"],
];

export const statCards = [
  {
    title: "Najbolj zazelene vescine",
    value: "React + AI UX",
    detail: "TypeScript, prompt workflows, design systems and performance lead the shortlist.",
    tone: "cyan",
  },
  {
    title: "Najbolj iskane vloge",
    value: "Developers",
    detail: "Developers first, then designers, chefs, therapists and product operators.",
    tone: "pink",
  },
  {
    title: "Delovna mesta po regijah",
    value: "Berlin / London",
    detail: "Berlin, London, Paris, Amsterdam and Zurich carry the strongest density.",
    tone: "lime",
  },
  {
    title: "Raven izkusenj",
    value: "Senior-heavy",
    detail: "Senior and lead roles dominate the current dummy market distribution.",
    tone: "amber",
  },
];

export const roleMix = [
  ["Developers", 94, "#69f5ff"],
  ["Designers", 36, "#ff6fb7"],
  ["Chefs", 18, "#ffd166"],
  ["Therapists", 12, "#8ef0a7"],
];

export function getMotionCities() {
  return motionCountries.flatMap((country) =>
    country.cities.map((city) => ({
      ...city,
      country: country.code,
      countryName: country.name,
      color: country.color,
    })),
  );
}
