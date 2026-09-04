# Architecture — Shri Trishakti Infra Realtors CRM

## 1. High-level

```
        ┌───────────────┐        ┌───────────────┐
        │  Web (React)  │        │ Mobile(Flutter)│
        └──────┬────────┘        └───────┬───────┘
               │  HTTPS / JSON (JWT Bearer)
               └─────────────┬───────────┘
                     ┌───────▼────────┐
                     │  REST API      │   Spring Boot 3 (Java 25)
                     │  /api/**       │
                     ├────────────────┤
                     │ Controller     │  request/response DTOs, validation
                     │ Service        │  business rules, transactions, workflow
                     │ Repository     │  Spring Data JPA
                     │ Security       │  JWT filter, method-level @PreAuthorize
                     │ Aspect         │  audit logging, activity timeline
                     └───────┬────────┘
                     ┌───────▼────────┐
                     │  PostgreSQL 16 │  normalized schema, FKs, indexes
                     └────────────────┘
```

Web and Mobile are **pure API clients** — no server-side rendering, no shared session state.
The only contract between them and the backend is the OpenAPI-documented REST API.

## 2. Backend layering

| Package                         | Responsibility |
|---------------------------------|----------------|
| `com.trishakti.crm.config`      | Security, CORS, OpenAPI, JPA auditing, Jackson, seed data |
| `com.trishakti.crm.security`    | `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetails(Service)`, `AuthController` |
| `com.trishakti.crm.domain`      | JPA entities + enums (the persistence model) |
| `com.trishakti.crm.repository`  | Spring Data JPA repositories, `@Query` for reports, `Specification`s for search |
| `com.trishakti.crm.dto`         | Request/response records grouped per module |
| `com.trishakti.crm.mapper`      | Plain Java mappers entity ⇄ DTO |
| `com.trishakti.crm.service`     | Business logic, workflow transitions, transactions |
| `com.trishakti.crm.web`         | REST controllers, one per module |
| `com.trishakti.crm.exception`   | Domain exceptions + `GlobalExceptionHandler` (RFC-7807 style body) |
| `com.trishakti.crm.common`      | `PageResponse`, `ApiError`, pagination/sort helpers, CSV/Excel utils |

### Cross-cutting

- **Validation** — Jakarta Bean Validation on request DTOs (`@NotBlank`, `@Email`, `@Positive`…). Handled centrally by `GlobalExceptionHandler` → `400` with field errors.
- **Global error handling** — every exception funnels through `GlobalExceptionHandler`; consistent JSON `{ timestamp, status, error, message, path, fieldErrors[] }`.
- **Auditing** — `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` via `AuditorAware` (resolves current JWT user). Every entity extends `BaseEntity`.
- **Audit log** — `AuditLog` table written by `AuditAspect` around `@Auditable` service methods (actor, action, entityType, entityId, before/after JSON, ip, timestamp).
- **Activity timeline** — `LeadActivity` rows appended by `LeadService` on every state change / call / note / assignment. Powers the per-lead timeline endpoint.
- **Pagination** — all list endpoints accept `page`, `size`, `sort` (Spring `Pageable`); responses wrapped in `PageResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`).
- **Search / filter** — JPA `Specification` builders per module (e.g. `LeadSpecifications.byStatus/bySource/byAssignee/byBudgetRange/textSearch`).
- **Optimized queries** — projections & `@EntityGraph` to avoid N+1; dashboard uses aggregate `@Query` (single round-trip per metric group); indexes on FK + status + follow-up date + mobile.

## 3. Security

- Stateless. `SecurityFilterChain` with `SessionCreationPolicy.STATELESS`.
- `POST /api/auth/login` → `{ accessToken, refreshToken, user }`. `POST /api/auth/refresh`.
- `JwtAuthenticationFilter` validates `Authorization: Bearer <token>`, sets `SecurityContext`.
- Passwords hashed with BCrypt (strength 10).
- Authorization:
  - URL-level rules in `SecurityConfig` (e.g. `/api/users/**` → `ADMIN`).
  - Method-level `@PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")` on services/controllers.
  - Data-level: calling team & sales exec only see leads where `assignedUser = currentUser` (enforced in service via `Specification` + guard).
- Refresh tokens stored hashed in `refresh_token` table; rotation on use; revoke on logout.

## 4. Workflow engine (leads)

`LeadService.transition(leadId, targetStatus, payload)` validates allowed transitions from a
static map (`LeadStatus.allowedNext()`), applies side effects (create `SiteVisit`, `Booking`,
`Notification`, `Task`), writes a `LeadActivity`, and returns the updated lead.
Illegal transitions → `409 Conflict` (`InvalidWorkflowTransitionException`).

## 5. Notifications & Reminders

- `Notification` rows (in-app) created on assignment, follow-up due, site visit scheduled, booking.
- `ReminderScheduler` (`@Scheduled` every 5 min) scans `follow_up` / `task` where `dueAt <= now+window`
  and `notified = false`, creates notifications, marks notified. Pluggable `NotificationChannel`
  (in-app now; email/SMS/FCM adapters are interface stubs).

## 6. Import / Export

- `POST /api/leads/import` — multipart CSV/XLSX → `LeadImportService` (Apache Commons CSV / Apache POI),
  row-level validation, returns `{ imported, skipped, errors[] }`.
- `GET /api/leads/export?format=csv|xlsx&<filters>` — streams filtered leads.

## 7. Configuration

Environment-driven (`application.yml` + profiles `default`, `docker`, `test`). All secrets via env vars
(`DB_*`, `JWT_*`, `CORS_ALLOWED_ORIGINS`). No secret is committed. `test` profile uses H2.

## 8. Web app architecture

- `src/api/` — Axios instance with JWT interceptor + refresh-on-401; typed endpoint modules.
- `src/auth/` — `AuthProvider` (context), `ProtectedRoute`, role guards.
- `src/features/<module>/` — pages + hooks (TanStack Query) + components.
- `src/components/ui/` — design-system primitives (Button, Card, Table, Badge, Modal…).
- Responsive: Tailwind breakpoints; sidebar collapses to bottom-nav on mobile widths.

## 9. Mobile app architecture

- `lib/core/` — Dio client + interceptors, secure storage for tokens, theme, router (go_router).
- `lib/features/<module>/` — data (models/repo), presentation (screens/widgets), Riverpod providers.
- Same DTO shapes as backend; `API_BASE_URL` via `--dart-define`.
