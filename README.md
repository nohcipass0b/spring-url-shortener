# spring-url-shortener

> Take-home assignment: a URL shortener built as separate Spring Boot services in one repo.

Built as a **monorepo** so the whole stack shares one build and comes up with a single
`docker compose up`. Each service still owns its own schema and could be split out later.

**Stack** — Java 21 · Spring Boot 4.1.1 · PostgreSQL 17 · Flyway · Gradle (multi-module) · Docker Compose

---

## Services

| Service | Port | Status | Description |
|---|---|---|---|
| `auth-service` | 8080 | ✅ Running | Registration, login, JWT issuance |
| `url-service` | 8081 | 🚧 Planned | Shorten and resolve URLs |
| `postgres` | 5432 | ✅ Running | Database (Docker) |

---

## Getting started

```bash
cp .env.example .env        # then set JWT_SECRET
docker compose up -d --build
```

Both containers report `healthy` in about 30 seconds:

```bash
docker compose ps
```

The API is then on `http://localhost:8080`.

### Running locally instead

Postgres still comes from Docker; the service runs on the host.

```bash
docker compose up -d postgres
export JWT_SECRET=<your-secret>          # PowerShell: $env:JWT_SECRET = "..."
./gradlew :auth-service:bootRun
```

`JWT_SECRET` has no default on purpose — see [Design decisions](#design-decisions).

---

## API

### Register

```bash
curl -X POST http://localhost:8080/api/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"secret123"}'
```

```json
{
  "status": { "code": "SUCCESS", "description": { "en": "Success" } },
  "data": {
    "id": "8fbef335-1996-4992-8f86-c6ceb6442f05",
    "email": "user@example.com",
    "createdAt": "2026-09-18T15:08:50.080Z"
  }
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"secret123"}'
```

```json
{
  "status": { "code": "SUCCESS", "description": { "en": "Success" } },
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "0iiIQ1ABBYj9Z9BDolz1aU_folpOnyyFZfVyjolLhWQ",
    "tokenType": "Bearer",
    "expiresInSeconds": 900
  }
}
```

### Response envelope

Every response — success or failure — uses the same shape, so clients parse one format:

| Code | HTTP | When |
|---|---|---|
| `SUCCESS` | 200 / 201 | Request succeeded |
| `ERR_VALIDATION` | 400 | Field validation failed; `data` lists the fields |
| `ERR_UNAUTHORIZED` | 401 | Bad credentials |
| `ERR_EMAIL_TAKEN` | 409 | Email already registered |

```json
{
  "status": { "code": "ERR_VALIDATION", "description": { "en": "Validation failed" } },
  "data": {
    "email": "must be a valid email address",
    "password": "password must be between 8 and 72 characters"
  }
}
```

---

## Design decisions

**Refresh tokens are stored hashed.** Only the SHA-256 of a refresh token reaches the
database. A database dump therefore cannot be replayed as a valid session.

**Login failures are indistinguishable.** A wrong password and an unregistered email
return byte-identical responses, so the endpoint cannot be used to enumerate which
addresses have accounts.

**Flyway owns the schema; Hibernate only checks it.** `ddl-auto=validate` means the app
refuses to start if an entity and its table have drifted apart, rather than silently
altering production data.

**`JWT_SECRET` has no fallback value.** A default would let a deployment that forgot to
set it boot successfully with a secret that is public in this repository. Failing at
startup is the safer outcome.

**Passwords use BCrypt.** The `password_hash` column is `varchar(60)` — exactly a BCrypt
digest — and the request DTO caps passwords at 72 bytes, past which BCrypt silently
ignores input.

---

## Project layout

```
.
├── auth-service/
│   ├── src/main/java/...      # controller · service · repository · entity
│   ├── src/main/resources/db/migration/
│   │   ├── V1__create_user_table.sql
│   │   └── V2__create_sessions_table.sql
│   └── Dockerfile             # multi-stage, layered, non-root
├── settings.gradle            # includes each service
├── docker-compose.yml
└── .env.example
```

---

## Not implemented yet

- `POST /api/refresh` and `POST /api/logout` — the `sessions` table and
  `Session.revoke()` exist, but no endpoint calls them yet
- `url-service` and `redirect-service`
- Automated tests
- Rate limiting on the auth endpoints
