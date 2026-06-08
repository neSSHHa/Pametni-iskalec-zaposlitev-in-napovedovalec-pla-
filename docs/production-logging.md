# Production Logging

The logging dashboard is an internal operational tool intended only for authorized developers and administrators. It is not part of the public application and requires both server access and valid Grafana credentials.

## What Runs

The single-server production logging pipeline is:

```text
application stdout/stderr -> Docker -> Grafana Alloy -> Loki -> Grafana
```

- Spring Boot already uses SLF4J and Logback to print Java logs.
- Docker captures output from every container.
- Docker rotates its local log files to prevent unlimited disk usage.
- Alloy reads Docker container logs and sends them to Loki.
- Loki stores searchable logs for seven days.
- Grafana is the browser interface for searching Loki.

Loki data, Grafana settings and Alloy positions are stored in Docker volumes. Recreating an application container does not delete the logs already stored in Loki.

## Configure The Password

Add a strong password to the root `.env` file:

```env
GRAFANA_ADMIN_PASSWORD=replace-with-a-long-random-password
```

## Start The Production Stack

From the repository root:

```bash
docker compose \
  --env-file .env \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.prod.yml \
  -f docker/docker-compose.observability.yml \
  up -d --build
```

## Open Grafana Safely

Grafana listens only on `127.0.0.1:3001` on the production server. It is not exposed directly to the internet.

Only authorized team members with SSH access to the production server and the Grafana administrator password can open the dashboard.

From your computer, create an SSH tunnel:

```bash
ssh -L 3001:127.0.0.1:3001 your-user@your-server-ip
```

Open `http://localhost:3001` in your browser and sign in as `admin` with the password from `.env`.

## Search Logs

In Grafana, open **Explore** and select the **Loki** data source.

All backend logs:

```logql
{service="backend"}
```

Backend errors:

```logql
{service="backend", level="ERROR"}
```

Errors from every service:

```logql
{level="ERROR"}
```

Follow one request across services:

```logql
{environment="production"} |= "requestId=replace-with-request-id"
```

Follow one user search and its automatic salary prediction:

```logql
{environment="production"} |= "interactionId=replace-with-interaction-id"
```

Fast-mode prompt searches:

```logql
{service="backend"} |= "event=fast.filter.extracted"
```

Thinking-mode AI extractions:

```logql
{service="ai-service"} |= "event=ai.filter.extracted"
```

CV uploads and extracted filters:

```logql
{service="backend"} |~ "event=cv.(upload.received|filter.extracted|search.completed)"
```

Salary results:

```logql
{service="salary-service"} |~ "event=salary.(predicted|unavailable)"
```

Browser API calls receive an `X-Request-ID`. Each prompt or CV search also receives an `X-Interaction-ID`, which is reused for its automatic salary prediction. The backend forwards both IDs to internal AI and salary services, and each service logs important request boundaries. Prompt searches intentionally include the submitted prompt text and extracted search filters. CV logs include extracted filters but exclude CV contents and filenames. Logs still exclude tokens and API keys.

Use the time picker in the top-right corner to select the last hour, day or seven days.

## Security Notes

- Keep passwords, JWT tokens, API keys and CV contents out of logs.
- Prompt text is stored in Loki for seven days. Avoid entering personal or sensitive information in search prompts. Revisit this choice before accepting real users.
- Alloy receives read-only access to the Docker socket so it can discover containers and read logs. Treat Alloy as a trusted infrastructure service.
- Browser-side React errors are not captured by container logs. Add a frontend error-monitoring service separately when needed.
