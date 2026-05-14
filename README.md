# Smart Job Platform

Minimalni starter za projekt:

- `backend` - Spring Boot + JPA/Hibernate ORM
- `frontend` - React prikaz poslova
- `database` - MySQL baza kroz Docker

Prazni placeholder folderi `ai-service`, `data-ingestion`, `docs`, `scripts` i `tests` ostaju za kasnije, ali trenutno nisu punjeni kodom.

## Pokretanje

```bash
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml up --build
```

URL-ovi:

- Frontend: http://localhost:3000
- Backend jobs API: http://localhost:8080/api/jobs

Backend sam pravi tabele preko ORM-a (`spring.jpa.hibernate.ddl-auto=update`) i ubacuje nekoliko demo poslova pri startu.
