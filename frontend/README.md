# Frontend

React user interface for Job Radar.

## Responsibilities

- job search with natural-language prompts and structured filters;
- CV upload and CV-based job matching;
- job details and comparison;
- labour-market analytics;
- salary prediction display;
- Keycloak login, registration and logout;
- role-based access to the admin panel.

The frontend communicates only with the Backend API and Keycloak. It does not access MySQL, the AI Service or the Salary Service directly.

## Technology

- React 18
- Vite 5
- Axios
- amCharts 5
- Lucide React
- Nginx for the Docker production build

## Configuration

For the Docker setup, keep the frontend variables in the shared `.env` in the repository root:

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=smartjobs
VITE_KEYCLOAK_CLIENT_ID=jobradar-frontend
```

## Local development

Requirements: Node.js 20+ and npm.

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

The Backend API and Keycloak must be available for API and authentication flows.

## Commands

```bash
npm run dev       # development server
npm run build     # production build
npm run preview   # preview production build
```

## Authentication flow

1. The frontend redirects the user to Keycloak.
2. Keycloak returns an authorization code to `/auth/callback`.
3. The frontend exchanges the code using PKCE.
4. Tokens are stored under `jobradar-auth` in `localStorage`.
5. Axios adds the access token as a Bearer token to backend requests.

## Main structure

```text
src/
|-- api/          Backend and authentication clients
|-- components/   Reusable UI and feature components
|-- context/      Authentication and comparison state
|-- hooks/        Shared React hooks
|-- pages/        Application views
`-- styles/       Application styling
```

## Docker

Run from the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d --build frontend
```
