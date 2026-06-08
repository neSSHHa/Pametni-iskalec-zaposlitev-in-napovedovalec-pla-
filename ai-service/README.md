# AI Service

Internal Spring Boot service for AI-assisted processing.

## Responsibilities

- extract structured job filters from natural-language prompts;
- extract job-search criteria from CV text;
- rewrite CV text into a concise candidate profile;
- communicate with OpenRouter;
- isolate AI provider logic from the main backend.

The Frontend never calls this service directly. Requests flow through the Backend API.

## Technology

- Java 17
- Spring Boot 3.3.5
- Spring Web
- OpenRouter API

## Configuration

```env
AI_SERVICE_PORT=8090
OPENROUTER_URL=https://openrouter.ai/api/v1
OPENROUTER_API_KEY=your_api_key
OPENROUTER_MODEL=poolside/laguna-xs.2:free
OPENROUTER_REFERER=http://localhost:3000
OPENROUTER_TITLE=Smart Jobs Platform
```

Create an API key at [OpenRouter](https://openrouter.ai).

## Local development

Requirements: Java 17 and Maven.

### Windows (PowerShell)

```powershell
$env:OPENROUTER_API_KEY="your_api_key"
$env:OPENROUTER_URL="https://openrouter.ai/api/v1"
$env:OPENROUTER_MODEL="poolside/laguna-xs.2:free"
$env:OPENROUTER_REFERER="http://localhost:3000"
$env:OPENROUTER_TITLE="Smart Jobs Platform"
cd ai-service
mvn spring-boot:run
```

### macOS/Linux

```bash
export OPENROUTER_API_KEY="your_api_key"
export OPENROUTER_URL="https://openrouter.ai/api/v1"
export OPENROUTER_MODEL="poolside/laguna-xs.2:free"
export OPENROUTER_REFERER="http://localhost:3000"
export OPENROUTER_TITLE="Smart Jobs Platform"
cd ai-service
mvn spring-boot:run
```

The service is available at `http://localhost:8090`.

## Endpoints

```text
GET  /api/ai/health
POST /api/ai/jobs/extract
POST /api/ai/cv/extract
POST /api/ai/cv/rewrite
```

Health check:

```text
http://localhost:8090/api/ai/health
```

## Request flow

```text
Frontend -> Backend API -> AI Service -> OpenRouter
```

The service returns structured JSON to the backend. It does not access MySQL.

## Docker

Set `OPENROUTER_API_KEY` in the root `.env`, then run from the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d --build ai-service
```
