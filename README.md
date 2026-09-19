# spring-url-shortener

> Take-home assignment: a URL shortener built as separate Spring Boot services in one repo.

Built as a **monorepo** so the whole stack shares one build and comes up with a single
`docker compose up`. Each service still owns its own schema and could be split out later.

**Stack** — Java 21 · Spring Boot 4.1.1 · PostgreSQL 17 · Flyway · Gradle (multi-module) · Docker Compose

---

## Services

| Service | Port | Description |
|---|---|---|
| `auth-service` | 8080 | Registration, login, JWT issuance |
| `url-service` | 8081 | Shorten, list, deactivate, redirect |
| `postgres` | 5432 | Two databases: `authdb` and `urldb` |

---

## Architecture ideal


```
  client
    |  POST /api/shorten   Authorization: Bearer <token>
    v
  +------+   1. is this token valid?    +--------------------+
  | API  | ------------------------->   | auth-service :8080 | --> authdb
  | GATE |                              |                    |
  | WAY  | <-------------------------   |  owns JWT_SECRET   |
  +------+   2. yes, user id = ...      +--------------------+
    |
    |  3. forward upstream, user id in a header
    v
  +--------------------+
  | url-service :8081  | --> urldb
  |  trusts the header |
  +--------------------+
```

## Getting started

```bash
cp .env.example .env        # then set JWT_SECRET
docker compose up -d --build
```

All three containers report `healthy` in under a minute:

```bash
docker compose ps
```

auth-service is then on `http://localhost:8080`, url-service on `http://localhost:8081`.

### Running locally instead if(docker compose up -d --build) skip this part kub

```bash
docker compose up -d postgres
export JWT_SECRET=<your-secret>          # PowerShell: $env:JWT_SECRET = "..."
./gradlew :auth-service:bootRun
./gradlew :url-service:bootRun
```

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

### Shorten a URL

`token` from the login response goes in the `Authorization` header for every url-service
call except the redirect.

```bash
curl -X POST http://localhost:8081/api/shorten \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"original_url":"https://example.com/a/very/long/path"}'
```

```json
{
  "status": { "code": "SUCCESS", "description": { "en": "Success" } },
  "data": { "short_url": "http://localhost:8081/r/WLApoNN" }
}
```

### List your links

```bash
curl http://localhost:8081/api/urls -H "Authorization: Bearer $TOKEN"
```

```json
{
  "status": { "code": "SUCCESS", "description": { "en": "Success" } },
  "data": [
    {
      "id": "9df907ca-9226-47cf-b4d7-80d678940a81",
      "code": "WLApoNN",
      "short_url": "http://localhost:8081/r/WLApoNN",
      "original_url": "https://example.com/a/very/long/path",
      "active": true,
      "click_count": 0,
      "created_at": "2026-09-19T03:35:46.925Z"
    }
  ]
}
```

### Deactivate and re-activate

```bash
curl -X DELETE http://localhost:8081/api/urls/$ID -H "Authorization: Bearer $TOKEN"
curl -X PUT http://localhost:8081/api/urls/$ID/activate -H "Authorization: Bearer $TOKEN"
```

A deactivated link answers 404 on redirect. Re-activating brings it back with its click
count intact.

### Redirect

Public, no token. This is what the short URL points at.

```bash
curl -i http://localhost:8081/r/WLApoNN
# HTTP/1.1 302
# Location: https://example.com/a/very/long/path
```

### Response envelope

Every response — success or failure — uses the same shape, so clients parse one format:

| Code | HTTP | When |
|---|---|---|
| `SUCCESS` | 200 / 201 | Request succeeded |
| `ERR_VALIDATION` | 400 | Field validation failed; `data` lists the fields |
| `ERR_UNAUTHORIZED` | 401 | Bad credentials, or a missing or invalid token |
| `ERR_NOT_FOUND` | 404 | No such link, or it is not yours |
| `ERR_EMAIL_TAKEN` | 409 | Email already registered (auth-service only) |

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

## Logs

Every response have `X-Request-Id` in header. Send your own it keep yours, if not it
generate one. So when something wrong just grep that id and you see all line of it.

```bash
docker compose logs -f auth-service
```

```
WARN  [urlshortener-auth-service,a3f1c2d8] ApiExceptionHandler  : POST /api/register rejected: ERR_EMAIL_TAKEN
INFO  [urlshortener-auth-service,a3f1c2d8] RequestLoggingFilter : POST /api/register -> 409 in 14ms
```

`WARN` = caller do wrong, no stack trace. `ERROR` = our bug, full stack trace. Read the
last `Caused by`, that one is real cause.

No request body in log, because /register and /login send password as plain text.

### Plan

Right now it only print to stdout and you must grep by yourself. Next step is ship it to
Elasticsearch: the service keep writing to stdout, then a consumer like Filebeat or
Fluent Bit read from docker and push to ELK, so nothing change in the code.

Before that the log should be JSON instead of text (one line one event), otherwise the
stack trace become many event and `requestId` is not a field you can filter. Then in
Kibana you search by request id and see everything of that one call.

---

## Testing

```bash
./gradlew test                    # both services
./gradlew :auth-service:test      # one service
./gradlew :url-service:test --tests "*CodeGeneratorTest*"     # one class
```

`./gradlew build` runs the formatting check, the tests and the coverage gate together.

Gradle skips a test task whose inputs have not changed and reports `UP-TO-DATE`. Add
`--rerun-tasks` to force it, or `-i` to see each test name as it runs.

### Reading the results

The console prints `BUILD SUCCESSFUL`, or the names of the tests that failed. The full
report, with stack traces, is written to:

```
auth-service/build/reports/tests/test/index.html
url-service/build/reports/tests/test/index.html
```


```
auth-service/build/reports/jacoco/test/html/index.html
url-service/build/reports/jacoco/test/html/index.html
```


## Project layout

```
.
├── auth-service/
│   ├── src/main/java/...          # controller · service · repository · entity
│   ├── src/test/java/...
│   ├── src/main/resources/db/migration/
│   │   ├── V1__create_user_table.sql
│   │   └── V2__create_sessions_table.sql
│   └── Dockerfile                 # multi-stage, layered, non-root
├── url-service/
│   ├── src/main/java/...          # same layout, plus security/ for the JWT filter
│   ├── src/test/java/...
│   ├── src/main/resources/db/migration/
│   │   └── V1__create_short_urls_table.sql
│   └── Dockerfile
├── docker/postgres-init/          # creates urldb alongside authdb
├── config/spring-formatter.xml    # same rules the build enforces
├── settings.gradle                # includes each service
├── docker-compose.yml
└── .env.example
```

---

## Not implemented yet

- `POST /api/refresh` and `POST /api/logout` — the `sessions` table and
  `Session.revoke()` exist, but no endpoint calls them yet
- the logger part that log request response consume and provide for monitors to like elastic elk
