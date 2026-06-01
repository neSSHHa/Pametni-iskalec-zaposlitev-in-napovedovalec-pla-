# Smart Job Platform
RIS project - Smart job search and salary prediction application

## Overview
Smart Job Platform is an application for searching, filtering and analyzing job listings. The system combines a Spring Boot backend, a React frontend, a separate AI service and a MySQL database to provide a practical job-search experience.

The application enables users to browse jobs, search by natural language, filter by location, skills, work type, education and experience level, analyze CV text and view analytics about the job market.

## Vision
Smart Job Platform is designed to make job searching faster, clearer and more intelligent. The goal of the project is to provide a modern digital tool that helps users discover relevant jobs, understand market trends and use AI-assisted search to transform natural language or CV text into structured job filters.

The application is intended for job seekers, students, career changers and anyone who wants a more practical overview of available job opportunities. The architecture separates the AI component from the main backend so the AI service can be replaced or upgraded without changing the rest of the system.

## Vocabulary
| Term | Meaning in the application |
| --- | --- |
| Job | A job listing with title, company, description, location, skills, work type, salary and source URL. |
| Job Search | A function that allows users to find jobs by keyword, title or company. |
| Filtering | Advanced search based on job, location, work type, skills, education level, experience level and salary. |
| Natural Language Search | AI-assisted search where the user writes a normal sentence and the system converts it into structured filters. |
| AI Service | A separate service that receives prompt text and allowed values, calls the AI provider and returns extracted filters. |
| Allowed Values Cache | Backend cache containing skills, education levels, experience levels and work types loaded from the database. |
| Skill | A professional or domain skill connected to jobs, for example Java, Cooking, Docker or Communication. |
| Work Type | Job work mode or employment type, such as Remote, Hybrid, On-site or Full-time. |
| Experience Level | The seniority level required for a job, such as Junior, Mid, Senior or Lead. |
| Education Level | The education requirement connected to a job listing. |
| Location | City, region, country and coordinates used for job filtering and map visualization. |
| CV Analysis | Feature that extracts text from a CV and uses it to build a job filter or job matching profile. |
| Analytics | Dashboard data about jobs, skills, roles, locations, salaries and other market indicators. |
| Dataset Import | Temporary development utility used to load the current job dataset into MySQL. |

## Architecture
The application is split into independent components:

- `frontend` - React application served through Nginx.
- `backend` - Spring Boot REST API, database access, filtering logic, analytics and CV processing.
- `ai-service` - Spring Boot AI worker that builds prompts and communicates with OpenRouter.
- `mysql` - MySQL database used by the backend.

Main communication flow:

```text
Frontend -> Backend API -> MySQL
Frontend -> Backend API -> AI Service -> OpenRouter
Dataset Import -> MySQL
```

The frontend does not call the AI service directly. It calls the backend, and the backend forwards only the required AI requests to the AI service.

## Developer Documentation

### Project Structure
```text
SmartJobPlatform/
├── backend/                         # Backend application (Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/si/um/feri/smartjobs/
│   │   │   │   ├── ai/              # AI API facade, DTOs, cache and client for ai-service
│   │   │   │   ├── analytics/       # Analytics controllers, services and DTOs
│   │   │   │   ├── auth/            # Authentication and JWT logic
│   │   │   │   ├── config/          # Spring configuration
│   │   │   │   ├── cv/              # CV text extraction and CV-based job matching
│   │   │   │   ├── job/             # Job entity, repository, DTOs, controller and service
│   │   │   │   ├── location/        # Location entity and repository
│   │   │   │   ├── recommendation/  # Recommendation logic
│   │   │   │   ├── seed/            # Data seeding helpers
│   │   │   │   ├── skill/           # Skill entity and repository
│   │   │   │   ├── user/            # User profile and account logic
│   │   │   │   └── SmartJobsApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
│
├── ai-service/                      # Separate AI service (Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/si/um/feri/smartjobs/ai/
│   │   │   │   ├── client/          # OpenRouter client and prompt construction
│   │   │   │   ├── config/          # AI service configuration
│   │   │   │   ├── controller/      # Internal AI REST endpoints
│   │   │   │   ├── dto/             # Request and response DTOs
│   │   │   │   └── AiServiceApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                        # Frontend application (React + Vite)
│   ├── src/
│   │   ├── components/              # React components
│   │   │   ├── motion/              # Map and motion UI components
│   │   │   └── ...
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
├── data-ingestion/                  # Temporary dataset import utility
│   ├── src/main/java/
│   │   └── ImportSloveniaData.java
│   ├── data/                        # CSV and JSON import files
│   ├── Dockerfile
│   └── pom.xml
│
├── docker/
│   ├── docker-compose.yml           # Main Docker Compose configuration
│   ├── docker-compose.dev.yml       # Development and data-import profile
│   ├── docker-compose.prod.yml
│   ├── docker-compose.test.yml
│   └── docker-compose.integration.yml
│
└── README.md
```

## Tools, Frameworks and Versions

### Backend (`backend/`)
- Java: 17
- Spring Boot: 3.3.5
- Spring Web: REST API
- Spring Data JPA: Database access
- Hibernate ORM: Entity persistence
- MySQL Connector/J: MySQL database connection
- H2: Test database
- Apache Tika: CV text extraction
- Maven: Dependency management and build
- Docker: Multi-stage build with Maven and Eclipse Temurin

### AI Service (`ai-service/`)
- Java: 17
- Spring Boot: 3.3.5
- Spring Web: Internal REST API
- RestTemplate: Communication with OpenRouter
- OpenRouter API: AI provider integration
- Maven: Dependency management and build
- Docker: Multi-stage build with Maven and Eclipse Temurin

### Frontend (`frontend/`)
- React: 18.3.1
- React DOM: 18.3.1
- Vite: 5.4.10
- Axios: API communication
- Lucide React: Icons
- amCharts 5: Map and chart visualization
- Vitest: Frontend testing
- Docker: Multi-stage build with Node.js and Nginx

### Dataset Import Utility (`data-ingestion/`)
- Java: 21
- Maven: Build and execution
- MySQL Connector/J: Database import connection
- org.json: JSON processing
- CSV/JSON files: Source data for jobs, locations, skills and relations

### Database
- MySQL: 8.4
- Host port: 3307
- Container port: 3306
- Default database: `smartjobs`

### Development Tools
- Docker
- Docker Compose
- Git
- IntelliJ IDEA, Eclipse or VS Code

## Coding Standards

### Java (Backend and AI Service)
- Use `camelCase` for variables and methods.
- Use `PascalCase` for classes and records.
- Organize packages by application domain and responsibility.
- Use Spring annotations such as `@Service`, `@Component`, `@RestController` and `@RequestMapping`.
- Keep DTOs separate from entities.
- Keep database access inside repository and service layers.
- Keep the AI service independent from the database.
- Prefer clear service boundaries and small request/response DTOs.

### JavaScript/React (Frontend)
- Use functional components and React hooks.
- Use `PascalCase` for component names.
- Use `camelCase` for functions, variables and props.
- Keep reusable UI code inside `components/`.
- Keep API calls centralized where possible.
- Use JSX syntax and Vite development conventions.
- Keep visual components responsive and readable.

### Docker and Configuration
- Keep environment-specific values in `.env` or Docker Compose environment variables.
- Do not hard-code secrets in source code.
- Use service names inside Docker network, for example `http://ai-service:8090`.
- Use `AI_SERVICE_URL` to configure backend-to-AI-service communication.
- Use the production logging runbook in [`docs/production-logging.md`](docs/production-logging.md) to collect and search logs with Alloy, Loki and Grafana.

## Installation Instructions

### Prerequisites
Before installing and running the application, make sure you have:

- Docker
- Docker Compose
- Git
- Optional for local development: Java 17, Java 21, Maven and Node.js

### Installation Steps

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd Pametni-iskalec-zaposlitev-in-napovedovalec-pla-
```

#### 2. Configure Environment Variables
Create or update the `.env` file in the project root if needed.

Example:

```env
MYSQL_DATABASE=smartjobs
MYSQL_ROOT_PASSWORD=nenadnenad
CORS_ALLOWED_ORIGINS=http://localhost:3000
VITE_API_BASE_URL=http://localhost:8080/api

OPENROUTER_URL=https://openrouter.ai/api/v1
OPENROUTER_API_KEY=your_openrouter_api_key
OPENROUTER_REFERER=http://localhost:3000
OPENROUTER_TITLE=Smart Jobs Platform
OPENROUTER_MODEL=poolside/laguna-xs.2:free
```

#### 3. Start the Application with Docker Compose
Run this command from the project root:

```bash
docker-compose -f docker/docker-compose.yml up -d --build
```

This command starts:

- MySQL database on port `3307`
- AI service on port `8090`
- Spring Boot backend on port `8080`
- React frontend on port `3000`

#### 4. Import Data and Train the Salary Model
If the database is empty or the import files changed, run:

```powershell
.\docker\import-data-and-train.ps1
```

This loads the current project dataset into MySQL, refreshes backend caches and trains a fresh salary model. Normal application starts reuse the saved model from the `salary_models` Docker volume.

#### 5. Check that the Application Works
Open:

- Frontend: `http://localhost:3000`
- Backend jobs API: `http://localhost:8080/api/jobs`
- AI service health check: `http://localhost:8090/api/ai/health`
- Salary service health check: `http://localhost:8091/health`

Expected AI health response:

```text
ok
```

#### 6. Stop the Application
```bash
docker-compose -f docker/docker-compose.yml down
```

To remove containers and database volume:

```bash
docker-compose -f docker/docker-compose.yml down -v
```

## Developer Instructions

### Running Backend Locally
```bash
cd backend
mvn spring-boot:run
```

The backend expects MySQL to be available and the AI service URL to be configured:

```env
AI_SERVICE_URL=http://localhost:8090
```

### Running AI Service Locally
```bash
cd ai-service
mvn spring-boot:run
```

The AI service requires an OpenRouter API key for AI extraction:

```env
OPENROUTER_API_KEY=your_openrouter_api_key
```

### Running Frontend Locally
```bash
cd frontend
npm install
npm run dev
```

Frontend development server:

```text
http://localhost:3000
```

### Running Tests and Builds
Backend:

```bash
cd backend
mvn test
mvn package
```

AI service:

```bash
cd ai-service
mvn test
mvn package
```

Frontend:

```bash
cd frontend
npm run build
npm test
```

## API Overview

### Backend API
- `GET /api/jobs` - list jobs
- `POST /api/jobs/filter` - filter jobs with structured criteria
- `POST /api/jobs/text-search` - search jobs by text
- `POST /api/ai/jobs/filter` - natural language job search
- `POST /api/ai/jobs/extract` - debug AI filter extraction
- `POST /api/cv/extract-text` - extract text from a CV file
- `POST /api/cv/jobs/filter` - filter jobs from CV text
- `POST /api/cv/extract-filter` - extract filters from CV text
- `POST /api/cv/rewrite-profile` - rewrite CV into a short profile text
- `GET /api/analytics` - analytics overview
- `GET /api/analytics/dashboard` - dashboard data

### AI Service API
The AI service is intended for internal backend communication.

- `GET /api/ai/health` - service health check
- `POST /api/ai/jobs/extract` - extract job filters from prompt text
- `POST /api/ai/cv/extract` - extract job filters from CV text
- `POST /api/ai/cv/rewrite` - rewrite CV text into a profile summary

## How to Contribute

### Development Workflow
1. Create or update code in the appropriate module.
2. Run the relevant build and tests.
3. Start the application locally and manually test the changed feature.
4. Check Docker Compose if the change affects service communication.

### Commit Messages
Use clear commit messages that describe the change:

```bash
git add .
git commit -m "Add AI service integration"
git push origin main
```

### Adding a New Feature

Backend:

- Add or update entity classes if the database model changes.
- Add or update repositories for database access.
- Add DTOs for request and response objects.
- Add service logic in the appropriate domain package.
- Add REST endpoints in controllers.
- Update tests where needed.

AI Service:

- Add DTOs for new AI request or response shapes.
- Add service/client logic for AI prompt construction.
- Keep the service independent from the database.
- Keep the API contract stable for the backend.

Frontend:

- Add a component inside `src/components/`.
- Add API integration through the existing frontend API pattern.
- Update routing or page structure if needed.
- Test the page in desktop and mobile viewport sizes.

Dataset Import Utility:

- Add or update CSV/JSON files in `data-ingestion/data/`.
- Update importer logic if the file structure changes.
- Run the importer after changing seed data.

## Code Review
Before submitting changes:

- Make sure the application builds successfully.
- Make sure Docker Compose starts all required services.
- Verify that the backend can reach the AI service.
- Verify that the database contains expected data.
- Check that no secrets are committed.
- Keep code readable, focused and consistent with existing project structure.
