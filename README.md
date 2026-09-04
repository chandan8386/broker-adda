# Shri Trishakti Infra Realtors Pvt Ltd — Property Lead Management / CRM

A full-stack Property Lead Management & CRM platform for property sales (Flats, Villas, Plots, Office Spaces).
Leads flow from advertisements → calling team → site visits → bookings → purchases.

```
client visit/
├── backend/        Java 25 + Spring Boot 3 + PostgreSQL + JPA/Hibernate + Spring Security + JWT + Swagger
├── web/            React 18 + Vite + TypeScript + Tailwind (professional CRM dashboard)
├── mobile/         Flutter (Android + iOS) consuming the same REST API
├── docs/           Architecture, ER/database schema, API notes
├── postman/        Postman collection for all endpoints
└── docker-compose.yml   App + PostgreSQL, one command
```

## Tech Stack

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 25, Spring Boot 3.5, Maven, Spring Data JPA, Hibernate, Spring Security 6, JWT (jjwt), springdoc-openapi (Swagger UI), Bean Validation, programmatic seed data |
| Database  | PostgreSQL 16 (local, Docker and Render), normalized schema, foreign keys, indexes, JPA auditing |
| Web       | React 18, Vite, TypeScript, React Router, TanStack Query, Axios, Tailwind CSS, Recharts |
| Mobile    | Flutter 3 (Dart), Dio, Riverpod, shared REST API |
| DevOps    | Docker, Docker Compose, environment-based configuration (`.env`) |

## Roles

- **ADMIN** – full access, user/team management, audit logs, all reports.
- **SALES_MANAGER** – lead assignment, all leads, all reports, team performance.
- **CALLING_TEAM** – assigned leads, calls, follow-ups, mark interested, schedule site visits.
- **SALES_EXECUTIVE** – interested leads, site visits, negotiation, bookings, purchases, payments.

## Lead Workflow

```
NEW → ASSIGNED → CALLING → CONNECTED / NOT_CONNECTED → INTERESTED / NOT_INTERESTED
   → SITE_VISIT_SCHEDULED → SITE_VISIT_DONE → NEGOTIATION → BOOKING → PURCHASED / CLOSED / LOST
```

## Quick Start (Docker — recommended)

```bash
cp .env.example .env          # adjust secrets if you like
docker compose up --build
```

| Service        | URL                                            |
|----------------|------------------------------------------------|
| Backend API    | http://localhost:8080/api                      |
| Swagger UI     | http://localhost:8080/api/swagger-ui.html      |
| OpenAPI JSON   | http://localhost:8080/api/v3/api-docs          |
| Web app        | http://localhost:5173                          |
| PostgreSQL     | localhost:5432 (db `trishakti_crm`)            |

### Seed / test accounts (password for all: `Password@123`)

| Role            | Username / Email                    |
|-----------------|------------------------------------|
| Admin           | `admin@trishakti.com`              |
| Sales Manager   | `manager@trishakti.com`            |
| Calling Team    | `caller1@trishakti.com`            |
| Sales Executive | `sales1@trishakti.com`            |

## Local Development (without Docker)

### Backend
```bash
cd backend
# Requires a running PostgreSQL with database `trishakti_crm`
#   docker compose up -d postgres      (easiest)
#   or: createdb -U postgres trishakti_crm
export DB_URL=jdbc:postgresql://localhost:5432/trishakti_crm
export DB_USER=trishakti DB_PASSWORD=trishakti JWT_SECRET=change-me-please-32-bytes-minimum-secret
mvn spring-boot:run
```

For a quick local run with no database at all, use the built-in file-backed H2 profile
(H2 in PostgreSQL compatibility mode):

```bash
cd backend
mvn -Plocal spring-boot:run -Dspring-boot.run.profiles=local
```

This starts the API at `http://localhost:8080/api` and creates the demo accounts and data in
`backend/data/`. Use PostgreSQL for shared or production environments.

### Web
```bash
cd web
npm install
echo "VITE_API_BASE_URL=http://localhost:8080/api" > .env.local
npm run dev
```

### Mobile
```bash
cd mobile
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api   # Android emulator
```

## Deploy — Backend on Render, Frontend on Vercel

Full step-by-step (CORS handshake, custom domains, backups, external-database option) is in
**[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)**. Short version:

### 1. Backend + DB on Render (Blueprint)

Render Dashboard → **New → Blueprint** → pick this repo. [`render.yaml`](render.yaml) provisions:

| Resource | Detail |
|---|---|
| `trishakti-crm-db` | managed **PostgreSQL** (free) — same engine as local dev and Docker, so there is no dev/prod drift |
| `trishakti-crm-api` | Docker web service from `backend/Dockerfile`, `PORT` auto-detected, health `/api/actuator/health`, `JWT_SECRET` auto-generated, `DB_*` auto-wired from the database |

Want to use Neon / Supabase / RDS instead? Delete the `databases:` block in `render.yaml` and set
`DB_URL` / `DB_USER` / `DB_PASSWORD` on the service — see `docs/DEPLOYMENT.md`.

### 2. Frontend on Vercel

Import the repo → **Root Directory `web`** (Vite auto-detected; [`web/vercel.json`](web/vercel.json)
handles SPA routing). Set for Production **and** Preview:

```text
VITE_API_BASE_URL=https://YOUR-RENDER-DOMAIN.onrender.com/api
```

### 3. Wire CORS

On Render → `trishakti-crm-api` → Environment, set
`CORS_ALLOWED_ORIGINS=https://YOUR-PROJECT.vercel.app` (comma-separated; entries with `*`,
e.g. `https://*.vercel.app`, are matched as patterns for preview URLs). Save → redeploy →
log in from the Vercel URL with `admin@trishakti.com` / `Password@123`.

## Current Cross-Platform Clients

The responsive React client in `web/` consumes the Spring REST API for login, lead search,
lead creation, workflow transitions, dashboard metrics, and pipeline views.

Large CSV imports use the resumable chunk API rather than the legacy single-request import:
`POST /api/leads/imports` starts a session, `POST /api/leads/imports/{uploadId}/chunks`
accepts up to 500 validated rows, `GET /api/leads/imports/{uploadId}` returns bounded status,
and `POST /api/leads/imports/{uploadId}/complete` finalizes it. The web UI parses CSV input
incrementally, sends one chunk at a time, prevents duplicate clicks, and shows progress.

```bash
cd web && npm install && npm run dev
```

The Windows wrapper in `desktop/` packages the same production bundle as an NSIS installer:

```bash
cd desktop && npm install && npm run dist
```

The Android wrapper in `mobile/` uses Capacitor and produces a release APK after Android SDK
and Gradle are installed:

```bash
cd mobile
npm install
npm run android:add       # first run only
npm run android:release
```

Set `VITE_API_BASE_URL` to the reachable backend address before building a client. Native
builds require the platform toolchains: JDK 17+, Android SDK/Gradle for APKs, and Windows
build tools for the Electron installer.

## Modules

Dashboard · Lead Management · Lead Assignment · Calling / Follow-up · Customer Management ·
Property Management · Site Visit Management · Sales / Purchase Management · Task & Reminder ·
Reports & Analytics · User / Team Management · Notifications · Excel/CSV Import-Export ·
Activity timeline per lead · Audit logs.

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — system architecture, layering, security, conventions.
- [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) — ER diagram + table dictionary.
- [`docs/schema.sql`](docs/schema.sql) — reference DDL.
- [`postman/Trishakti-CRM.postman_collection.json`](postman/Trishakti-CRM.postman_collection.json) — importable API collection.
- Swagger UI (live) — generated from code at runtime.

## Testing

```bash
cd backend && ./mvnw test        # unit + integration tests (H2 in-memory for integration)
```

## License

Proprietary — © Shri Trishakti Infra Realtors Pvt Ltd.
