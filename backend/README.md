# Trishakti CRM — Backend (Spring Boot)

Java 21+ · Spring Boot 3.5 · Spring Data JPA · Spring Security 6 · JWT · PostgreSQL 16 · springdoc-openapi.

## Run locally

```bash
# needs a PostgreSQL 14+ with an existing database `trishakti_crm`
#   createdb -U postgres trishakti_crm
#   (or just: docker compose up -d postgres)
export DB_URL="jdbc:postgresql://localhost:5432/trishakti_crm"
export DB_USER=trishakti DB_PASSWORD=trishakti
export JWT_SECRET="please-change-this-to-a-long-random-secret-at-least-32-bytes"
mvn spring-boot:run
```

No database handy? `mvn spring-boot:run -Plocal -Dspring-boot.run.profiles=local` runs against a
file-backed H2 in PostgreSQL compatibility mode (`backend/data/`).

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- Health: `http://localhost:8080/api/actuator/health`

On first start with an empty DB, `DataSeeder` creates roles, 6 users (password `Password@123`),
3 projects, 6 properties and ~17 leads spanning the workflow. Disable with `SEED_ENABLED=false`.

## Build & test

```bash
mvn clean package          # -> target/trishakti-crm.jar
mvn test                   # unit + @SpringBootTest (H2, profile `test`)
```

## Docker

```bash
docker build -t trishakti-backend .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/trishakti_crm \
  -e DB_USER=trishakti -e DB_PASSWORD=trishakti \
  -e JWT_SECRET=change-me-32-bytes-minimum-secret-value \
  trishakti-backend
```

## Layout

```
com.trishakti.crm
├── config        SecurityConfig, OpenApiConfig, AuditorAwareImpl, RoleInitializer, DataSeeder
├── security      JwtService, JwtAuthenticationFilter, UserPrincipal, SecurityUtils
├── domain        JPA entities + enums (BaseEntity has JPA auditing + @Version)
├── repository    Spring Data repos (+ Specifications, projection reports, @EntityGraph)
├── dto           request/response records grouped per module
├── mapper        CrmMappers (entity -> DTO)
├── service       AuthService, LeadService (+workflow engine), LeadAssignmentService,
│                 CallService, SiteVisitService, SalesService, PropertyService,
│                 CustomerService, DashboardService, TaskService, NotificationService,
│                 AuditService, LeadImportExportService, ReminderScheduler
├── web           one @RestController per module
├── exception     DomainExceptions + GlobalExceptionHandler (consistent JSON errors)
└── common        PageResponse<T>, ApiError
```

### Profiles

| Profile | Datasource | Used by |
|---------|-----------|---------|
| *(default)* | PostgreSQL via `DB_URL` / `DB_USER` / `DB_PASSWORD` | local dev |
| `docker` | PostgreSQL via `DB_URL` from docker-compose | `docker compose up` |
| `render` | PostgreSQL, URL composed from `DB_HOST`/`DB_PORT`/`DB_NAME` | Render deploy |
| `local` | file-backed H2 (PostgreSQL mode) | zero-setup demo |
| `test` | in-memory H2 (PostgreSQL mode) | `mvn test` |

### Notes on Java version
`pom.xml` targets the version set in `<java.version>`. If you build on an older JDK, drop
`<java.version>` back to `21` — no source changes are required (no preview features are used).
