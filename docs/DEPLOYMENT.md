# Deployment — Backend on Render, Frontend on Vercel

```
Vercel  (web/, static React build)  ──HTTPS──►  Render (Spring Boot API, Docker)  ──►  Render PostgreSQL
   VITE_API_BASE_URL = https://<api>.onrender.com/api          CORS_ALLOWED_ORIGINS = https://<web>.vercel.app
```

Both platforms deploy from the **same GitHub repo**. Push it first:

```bash
cd "d:\client visit"
git init && git add . && git commit -m "Trishakti CRM"
git branch -M main
git remote add origin https://github.com/<you>/trishakti-crm.git
git push -u origin main
```

---

## 1. Backend + database on Render

The repo has a **Blueprint** at [`render.yaml`](../render.yaml) that provisions the API service
**and** a free managed PostgreSQL instance, wired together automatically.

> The app runs on **PostgreSQL everywhere** — local dev, `docker compose`, and Render — so what you
> test locally is what runs in production. The `render` Spring profile
> (`backend/src/main/resources/application-render.yml`) only composes Render's injected
> `DB_HOST`/`DB_PORT`/`DB_NAME` into a JDBC URL and enables `sslmode=require`.

### Steps

1. **Render Dashboard → New → Blueprint** → connect the GitHub repo → **Apply**.
   Render creates:
   - `trishakti-crm-db` — PostgreSQL (free)
   - `trishakti-crm-api` — Docker web service, built from `backend/Dockerfile`
2. First build takes ~5–8 min (Maven downloads + compile + package inside Docker).
3. When it goes **live**, note the URL, e.g. `https://trishakti-crm-api.onrender.com`.
   - API base: `https://trishakti-crm-api.onrender.com/api`
   - Swagger: `…/api/swagger-ui.html`
   - Health: `…/api/actuator/health`
4. On first boot `SEED_ENABLED=true` seeds the demo users (password `Password@123`):
   `admin@ / manager@ / caller1@ / sales1@ trishakti.com`.
   Flip `SEED_ENABLED` to `false` in the service’s **Environment** tab once you have real data.

### Env vars (managed by `render.yaml`, editable in the dashboard)

| Key | Source | Notes |
|-----|--------|-------|
| `SPRING_PROFILES_ACTIVE` | `render` | selects the PostgreSQL profile |
| `JWT_SECRET` | auto-generated | stable across redeploys |
| `DB_HOST/PORT/NAME/USER/PASSWORD` | `fromDatabase` | composed into a JDBC URL by the profile |
| `CORS_ALLOWED_ORIGINS` | **you set this** | after the Vercel deploy — see step 3 below |
| `SEED_ENABLED` | `true` | demo data on first boot |

### Notes / limits

- Free Render services **sleep after 15 min idle**; the next request cold-starts in ~50 s.
- Free PostgreSQL is deleted after 90 days of the trial — upgrade the DB plan for anything real.
- `forward-headers-strategy: framework` is set so Spring sees `https` behind Render’s proxy.

---

## 2. Frontend on Vercel

1. **Vercel → Add New → Project** → import the same repo.
2. **Root Directory:** `web`  (Vercel then auto-detects Vite; [`web/vercel.json`](../web/vercel.json)
   pins the build command, output dir and SPA rewrites).
3. **Environment Variables** (Production **and** Preview):

   | Key | Value |
   |-----|-------|
   | `VITE_API_BASE_URL` | `https://trishakti-crm-api.onrender.com/api` |

   (Include `/api`, no trailing slash. Vite inlines `VITE_*` at build time, so **redeploy** after changing it.)
4. **Deploy.** You get `https://<project>.vercel.app`.

---

## 3. Connect them (CORS)

On **Render → trishakti-crm-api → Environment**, set:

```
CORS_ALLOWED_ORIGINS = https://<project>.vercel.app,https://*-<your-team>.vercel.app
```

- First value = your production domain (exact).
- Second (optional) = wildcard pattern for Vercel **preview** URLs. Entries containing `*` are
  matched as patterns (`setAllowedOriginPatterns`); credentials still work.
- Add any custom domains here too. Save → Render redeploys.

Verify:

```bash
curl -i -X OPTIONS https://trishakti-crm-api.onrender.com/api/auth/login \
  -H "Origin: https://<project>.vercel.app" \
  -H "Access-Control-Request-Method: POST"
# expect: 200 + access-control-allow-origin echoing your origin
```

Then open the Vercel URL and log in with `admin@trishakti.com` / `Password@123`.

---

## 4. Custom domains (optional)

- **Vercel:** Project → Settings → Domains → add `crm.yourcompany.com` (CNAME).
- **Render:** Service → Settings → Custom Domains → add `api.yourcompany.com`.
- Update `VITE_API_BASE_URL` (Vercel) and `CORS_ALLOWED_ORIGINS` (Render) to the new hosts, redeploy both.

---

## Alternative: external PostgreSQL instead of Render's

To use Neon, Supabase, Aiven, Railway, RDS or your own server instead of Render's managed DB:

1. Create a PostgreSQL 14+ database there; note host / port / db / user / password.
2. In `render.yaml`, delete the `databases:` block and the 5 `fromDatabase` env vars.
3. On the Render service set instead:
   - `SPRING_PROFILES_ACTIVE` → *(leave unset — the default profile already targets PostgreSQL)*
   - `DB_URL` → `jdbc:postgresql://<host>:<port>/<db>?sslmode=require`
   - `DB_USER`, `DB_PASSWORD`
4. Redeploy.

---

## Alternative: one-container deploy (Render Docker, no Blueprint)

If you don’t want the Blueprint: **New → Web Service → Docker**, root `backend/`, and add the
env vars from the table above manually, pointing `DB_URL`/`DB_USER`/`DB_PASSWORD` at any
reachable PostgreSQL.

---

## Backups

Render → database → **Backups** for point-in-time recovery on paid plans. Manual dump:

```bash
pg_dump "postgresql://USER:PASSWORD@HOST:PORT/DB?sslmode=require" -Fc -f trishakti-crm.dump
pg_restore -d "postgresql://..." --clean --if-exists trishakti-crm.dump
```

---

## CI sanity check before pushing

```bash
cd backend && mvn -q test           # 12 tests, H2 in PostgreSQL mode
cd ../web  && npm ci && npm run build
```
