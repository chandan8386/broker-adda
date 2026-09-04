# Trishakti CRM — Web (React + Vite + TypeScript)

Professional CRM dashboard. Talks only to the REST API (`VITE_API_BASE_URL`).

## Run

```bash
npm install
cp .env.example .env.local          # point VITE_API_BASE_URL at the backend
npm run dev                         # http://localhost:5173
```

## Build

```bash
npm run build      # -> dist/
npm run preview
```

## Docker

```bash
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080/api -t trishakti-web .
docker run -p 5173:80 trishakti-web
```

## Structure

```
src/
├── main.tsx / App.tsx        bootstrap + route table (role-guarded)
├── lib/
│   ├── api.ts                axios instance, JWT attach, refresh-on-401, error formatting
│   ├── types.ts              shared API DTO types
│   ├── format.ts             money / date helpers (date-fns + Intl)
│   └── meta.ts               /company enums for dropdowns
├── auth/                     AuthProvider (context), ProtectedRoute
├── components/               Layout (sidebar + mobile bottom-nav), ui primitives, Modal
└── pages/
    Login, Dashboard, Leads, LeadDetail (timeline + call/follow-up/site-visit + workflow),
    Assignments, FollowUps, SiteVisits, Sales, Properties, Customers, Tasks, Reports,
    Users, Audit, Notifications
```

Responsive: Tailwind breakpoints; the sidebar becomes a bottom nav under `md`.
Data layer: TanStack Query (caching, pagination via `keepPreviousData`, background refetch).
