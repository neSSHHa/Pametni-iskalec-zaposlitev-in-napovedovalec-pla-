# Backend API

Central Spring Boot API for Job Radar.

## Responsibilities

- job search, filtering and ranking;
- analytics and market statistics;
- CV text extraction and job matching;
- coordination of AI requests;
- salary prediction requests;
- Keycloak JWT validation and role-based authorization;
- admin overview, logs and cache management;
- database access through JPA/Hibernate.

The frontend calls the backend for application functionality. The backend coordinates MySQL, AI Service, Salary Service and Keycloak.

## Technology

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA and Hibernate
- Spring Security OAuth2 Resource Server
- MySQL
- Apache Tika
- Springdoc OpenAPI

## Configuration

Shared database, Keycloak and service URLs are defined in the root [`.env.example`](../.env.example). The defaults in `application.yml` support the standard local setup; override them only when your local services use different addresses or credentials.

## Local development

Requirements: Java 17, Maven, MySQL 8, Keycloak, AI Service and Salary Service.

Before starting the backend locally, make sure MySQL, Keycloak, AI Service and Salary Service are running.

```bash
cd backend
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

## Main API groups

```text
/api/jobs          Job listing, text search and filtering
/api/analytics     Market analytics and dashboard data
/api/cv            CV extraction and CV-based matching
/api/ai/jobs       AI-assisted job filtering
/api/salary        Salary prediction
/api/auth/me       Current authenticated Keycloak user
/api/admin         ADMIN-only overview, logs and cache operations
```

OpenAPI documentation is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Security

- `/api/auth/me` requires an authenticated user.
- `/api/admin/**` requires the Keycloak `ADMIN` role.
- Other application endpoints are currently public.
- Realm roles from the JWT are mapped to Spring roles such as `ROLE_ADMIN`.

Keycloak owns user identities, passwords and authentication. The backend validates signed JWT access tokens.

## Main package structure

```text
analytics/       Dashboard and market statistics
ai/              AI facade, clients and filter extraction
auth/            Current-user endpoint
config/          Security, CORS, OpenAPI and request logging
cv/              CV extraction and matching
job/             Jobs, filters and search
salary/          Salary Service client
admin/           Admin panel endpoints
seed/            Development seed data
```

## Docker

Run from the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d --build backend
```
