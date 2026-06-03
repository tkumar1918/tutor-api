# 🔄 Backend Agent Handoff

## 📍 LATEST SUMMARY (READ THIS FIRST!)
**Updated:** 2026-06-03
**From:** Backend Agent
**To:** Frontend Agent

> **🚩 PRIORITY — API path convention.** Stop rewriting `/api/` anywhere. Backend's single source of truth is `@RequestMapping("/api/v1/...")` and that's the URL every layer should pass through unchanged. Three concrete changes in `tutor-ui` (round 7 below has the full design rationale + diagram):
>
> 1. **Revert the global search-replace `/v1/` → `/api/v1/`** in your `src/`. Code should reference the API exactly as API.md documents it: `axios.get('/api/v1/courses')` (not `/v1/courses`).
> 2. **`vite.config.ts` proxy: delete the `rewrite` line.** Should be just `{ '/api': 'http://localhost:8080' }` — Vite passes `/api/v1/x` through to Spring as `/api/v1/x`.
> 3. **`VITE_API_BASE_URL=''`** (empty) or drop it from build args. Frontend uses pure relative URLs; both dev and prod use the same bundle.
>
> **Already fixed on my side:** nginx on `tutor.webspacehub.in` was stripping `/api/` (trailing slash on `proxy_pass http://127.0.0.1:8080/;`). Patched + reloaded — verified `https://tutor.webspacehub.in/api/v1/tutors` → 200, `/api/v1/auth/login` → 200. So as soon as you finish the three changes above, prod should light up.
>
> **Backend Aiven deploy is live + healthy** on the Oracle VM. Earlier round 6 covered the rebuild + swap fix.
>
> **Older open asks** still on the table: route-rename suggestions (round 3), action-pending notification counter (round 2).
>
> 12/12 backend tests green.

---

## 📜 Full History (Backend → Frontend)

### Backend → Frontend (2026-06-03) — round 7
**From:** Backend Agent

**Round: API path convention — stop rewriting `/api/` in every layer**

We accumulated a stack of well-meaning hacks across rounds (build-time `VITE_API_BASE_URL`, Vite proxy `rewrite`, nginx `proxy_pass http://...:8080/;` trailing slash, axios baseURL set to `/api`, search-replace of `/api/v1/` → `/v1/` in the codebase). Each one fixed the symptom of the last. Today's debug session showed the cumulative effect:

```
Browser → /api/v1/me/notifications
        Vite dev proxy  rewrite: path.replace(/^\/api/, '')  ← STRIP
        nginx (prod)    proxy_pass http://...:8080/;        ← STRIP
Backend gets: /v1/me/notifications → NoResourceFoundException 404
```

The conventional pattern is dead simple and the user explicitly asked for it: **one place owns the API path, everyone else is transparent.**

```
Browser address bar:  https://tutor.webspacehub.in
                              │
                              │  fetch('/api/v1/courses')   ← relative URL, no origin baked in
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Production:  nginx → location /api/ { proxy_pass …:8080; }       │ ← no trailing /
│ Dev:         Vite  → server.proxy = { '/api': 'http://…:8080' }  │ ← no rewrite
└─────────────────────────────────────────────────────────────────┘
                              │
                              │  GET /api/v1/courses  (unchanged)
                              ▼
                Spring  @RequestMapping("/api/v1/courses")
```

**Why this beats every variant:**
- One source of truth: backend `@RequestMapping`. API.md mirrors it.
- Same bundle in dev and prod; no build-time env var per env.
- Same-origin in the browser → no CORS preflights → fewer headers to debug.
- No string concatenation means no double-prefix bugs (`/api/api/...`).

**Concrete asks on your side (the three changes mentioned in LATEST SUMMARY):**

1. **Revert the search-replace.** In `tutor-ui/src/`, run the reverse: `/v1/` → `/api/v1/`. Every API path in `api.ts`, hooks, etc. should match what API.md documents (`/api/v1/courses`, `/api/v1/me/notifications`, etc.).
2. **`vite.config.ts`** — proxy block becomes:
   ```ts
   server: {
     proxy: {
       '/api': 'http://localhost:8080',   // no rewrite
     }
   }
   ```
3. **`VITE_API_BASE_URL=''`** (empty) — or remove from `Dockerfile` ARG / compose build args. Axios baseURL becomes `''`. All requests are pure paths: `axios.get('/api/v1/courses')`. The same bundle works on `http://localhost:5173` (Vite proxy handles `/api/`), `https://tutor.webspacehub.in` (nginx handles `/api/`), or any other origin you deploy under — without rebuilding.

**Already done on my side (so you can test immediately after your changes):**
- nginx on the prod server (`tutor.webspacehub.in`): removed trailing slash on `proxy_pass`, validated, reloaded. Confirmed `https://tutor.webspacehub.in/api/v1/tutors` → 200 and `/api/v1/auth/login` → 200 through nginx.
- Backed up the prior nginx config to `/etc/nginx/site-backups/` in case we ever need to roll back.

After your three changes land, the deployed UI at `https://tutor.webspacehub.in/` should be fully functional end-to-end without any further server work.

---

### Backend → Frontend (2026-06-02) — round 6
**From:** Backend Agent

**Round: deployment fixes on `ubuntu@92.4.81.1` (Oracle Cloud, 954 MiB RAM)**

What I did on the server (backend-side only, didn't touch your repo):

- **Diagnosed** `tutor-api Exited (137)`. Real cause was `ClassNotFoundException: com.mysql.cj.jdbc.Driver` — the image was built when we were briefly on Postgres and never rebuilt after the MySQL revert. Exit 137 was Docker's restart-loop reaper.
- **Added 2 GiB swap** at `/swapfile` and persisted in `/etc/fstab`. Without it, `docker compose build` was hitting OOM during the Maven compile (~600–800 MiB peak vs. ~450 MiB free).
- **Rebuilt + restarted** `tutor-api` via `docker compose up -d --build`. Boots in 38 s, login HTTP 200 via nginx, hitting Aiven MySQL.

What you'll see / what's yours to fix:

- **`tutor-ui` is in "unhealthy" state for ~4 hours.** Container is up but its own healthcheck fails. I left it untouched.
- **nginx routing mismatch:** see round 7 above for the resolution. The "frontend listens on 5173 but nginx expects 3000" observation from earlier was actually moot once we settled the `/api/` strip issue — nginx's `/` location proxies to wherever your container is listening; happy to update it to match whatever port you publish.

---

### Backend → Frontend (2026-06-02) — round 5
**From:** Backend Agent

**Round: rolled DB back to MySQL; compose now includes MySQL service**

Postgres / Supabase experiment from round 4 was reverted. Reason: Supabase free-tier direct host is IPv6-only and our local network has no IPv6 outbound — neither the host nor Docker bridge could route to it. The pooler workaround works but adds operational complexity (region in URL, special username format, paused-project failure modes) that isn't worth it for a tutorial repo.

Result of the revert:
- pom.xml back to `mysql-connector-j`
- application.properties + application-test.properties + data.sql restored to MySQL semantics (NOW(6), `MODE=MYSQL`, etc.)
- README quickstart split into "Option A: docker compose up" and "Option B: backend on host, MySQL in container"
- `docker-compose.yml` now defines **two services**: `mysql:8` (with named volume, healthcheck) + `backend` (depends_on mysql healthy)
- `.env` / `.env.example` defaults wire backend to `jdbc:mysql://mysql:3306/tutor_api`

**Nothing in this revert touches the API contract.** All endpoints, payloads, validation, and status codes are identical to round 3. Don't change anything in the frontend on account of this.

Verified end-to-end: `docker compose up` → MySQL becomes healthy in ~18s, backend boots in ~11s, `POST /auth/login` for seeded `ada` returns a JWT.

---

### Backend → Frontend (2026-06-02) — round 4 (superseded by round 5 above)
**From:** Backend Agent

**Round: containerization (backend-only) + DB migration to Postgres / Supabase**

**What landed in the backend repo:**
- `Dockerfile` — Alpine multi-stage, Spring Boot layered JAR, non-root user, JVM tuned for low-traffic showcase (MaxRAMPercentage=75, SerialGC, TieredStopAtLevel=1). Final image ~434 MB; boots in ~4 s; idles at ~260 MiB.
- `docker-compose.yml` — **backend service only.** Reads `.env` for `SPRING_DATASOURCE_URL` / `DB_USER` / `DB_PASSWORD` / `JWT_*`. Publishes 8080. mem_limit 384m. Healthcheck on `/v3/api-docs`.
- `.env.example` — tracked template; real `.env` is gitignored.
- Backend repo is now **fully self-contained** for `docker compose up`.

**Database: MySQL → Postgres** (transparent to the API contract, no schema changes you'd notice):
- pom.xml: `mysql-connector-j` → `org.postgresql:postgresql`
- application-test.properties: H2 `MODE=PostgreSQL` so tests match prod dialect
- data.sql: `NOW(6)` → `NOW()` (Postgres ignores precision args on `NOW()`)
- Tests: 12/12 green.

**Ask for you:** please add a `docker-compose.yml` in the tutor-ui repo for the frontend. Backend-side compose **deliberately does not reference `../tutor-ui`** anymore — each repo owns its own container orchestration so they stay independently cloneable.

Suggested shape for your compose (just a starting point, do whatever fits):

```yaml
services:
  frontend:
    build:
      context: .
      args:
        VITE_API_BASE_URL: http://localhost:8080
    image: tutor-ui:dev
    container_name: tutor-ui
    ports:
      - "5173:80"
    restart: unless-stopped
```

(Vite bakes `VITE_API_BASE_URL` at build time; since API calls happen from the browser — not the container — `http://localhost:8080` is the right value.)

**Local full-stack dev flow** (two terminals):
```bash
# Terminal 1
cd tutor-api && docker compose up --build

# Terminal 2
cd tutor-ui && docker compose up --build
```

**Supabase note:** the backend talks to Supabase Postgres in the user's local setup. Direct host (`db.<ref>.supabase.co:5432`) is IPv6-only — must use the transaction pooler (`aws-0-<region>.pooler.supabase.com:6543`) from Docker. Doesn't affect your work, just FYI if you ever wire your frontend to talk to Supabase directly.

---

### Backend → Frontend (2026-06-01) — round 3 (suggestion only, no code change)
**From:** Backend Agent

**Route-rename suggestions for the frontend URLs**

The user mentioned that the browser-bar URLs read awkward ("/me my stuff"). The backend `/api/v1/me/*` paths are **staying** (well-known REST convention — GitHub, Slack, Discord all use `/me`), but your **frontend routes** are yours to rename. Proposed mapping:

| Current frontend route | Suggested | Notes |
|---|---|---|
| `/me` | `/profile` | matches "profile" mental model better than "me" |
| `/me/edit` | `/profile/edit` | |
| `/me/apply-tutor` | `/become-a-tutor` or `/tutor-application/new` | "become" reads like an action |
| `/my-requests` | `/inbox/sent` or `/sent` | symmetric with `/inbox` |
| `/tutor-inbox` | `/inbox` | shorter; tutor context is implicit (only tutors see it) |
| `/courses/mine` | `/teaching` | one word, action-oriented |
| `/enrollments/mine` | `/learning` | symmetric counterpart to `/teaching` |
| `/admin/tutor-applications` | (keep) | admin paths are fine |
| `/admin/tutoring-requests` | (keep) | |

These are 100% your call — backend API paths don't change, so this is a pure react-router rename + nav label tweak. Skip any you disagree with.

**Note on a backend cleanup I'm NOT doing this round:** the backend itself has a four-pattern inconsistency for caller-scoped resources (`/me/X`, `/X/mine`, `/X/me`, `/tutoring-requests/incoming`). Unifying everything onto `/me/X` would mean breaking changes to your `api.ts`. I'm holding off until you say it's worth a coordinated round.

---

### Backend → Frontend (2026-06-01) — round 2
**From:** Backend Agent

**Round: nav badge counts**

- New endpoint: `GET /api/v1/me/notifications` (any authenticated)
- Response: `NotificationCountsResponse { tutorPendingRequests: number, adminPendingApplications: number }`
- Both fields are 0 when not applicable to the caller's role; safe to call from any logged-in user
- Frontend should render a badge on the matching nav item only when `> 0`
  - `tutorPendingRequests` → badge on **Inbox** nav (`/tutor-inbox`)
  - `adminPendingApplications` → badge on **Tutor applications** nav (`/admin/tutor-applications`)
- **Cache + invalidate** (react-query):
  - Initial fetch on app load (after auth) and on `/me` refresh
  - Invalidate after: `PATCH /tutoring-requests/{id}` (tutor responded → tutor count drops), `POST /admin/tutor-applications/{id}/review` (admin reviewed → admin count drops), `POST /me/tutor-application` (new application → admin count may rise — only matters for admins)
- **Not a notification system.** No read/unread tracking. A counter drops when the underlying item leaves PENDING, not when the caller "sees" it. Mention this in the UI copy so users don't expect "mark as read" semantics.
- Full doc: [API.md → GET /me/notifications](API.md)
- Tests: 12/12 green (added one test asserting zero-for-plain-user, populated-for-admin, populated-for-tutor)

---

### Backend → Frontend (2026-06-01) — round 1
**From:** Backend Agent

**Round: enrich tutor profile + add 1:1 tutoring-request flow**

**TutorProfile additions (response + apply + update):**
- `qualifications: string | null` — free-form, ≤500 chars (degrees, certs)
- `yearsOfExperience: number` — integer 0–60, **required** on `POST /me/tutor-application`, optional on `PUT /tutors/me`

**New resource: `TutoringRequest`** (1:1 private bookings, independent of `Course`):
- Statuses: `PENDING | ACCEPTED | REJECTED | CANCELLED`
- `POST /api/v1/tutoring-requests` — body `{tutorId, subject, message}`; any authenticated user
- `GET /api/v1/tutoring-requests/mine` — student's outgoing
- `GET /api/v1/tutoring-requests/incoming` — tutor's incoming (APPROVED only; 404 if no profile, 422 if not approved)
- `GET /api/v1/tutoring-requests/{id}` — student / targeted tutor / admin
- `PATCH /api/v1/tutoring-requests/{id}` — tutor responds `{status: ACCEPTED|REJECTED, tutorReply?}`; 403 if not targeted tutor, 422 if already terminal
- `DELETE /api/v1/tutoring-requests/{id}` — student soft-cancels (status → CANCELLED, row stays); only while PENDING
- `GET /api/v1/admin/tutoring-requests` — admin overview, filters `studentId`/`tutorId`/`status`
- Self-request blocked (422 *"You can't send a tutoring request to yourself"*)

**Docs ergonomics:**
- [API.md §4](API.md) now has a "Two business models" callout clarifying that `TutorProfile.hourlyRateCents` (1:1 rate) and `Course.priceCents` (packaged-course price) are independent — neither derives from the other. Display them separately.
- [API.md §7.7](API.md) — full endpoint reference for tutoring-requests
- [ROLES.md](ROLES.md) — capability matrix grew 7 rows + per-cell notes

**Seeded data for the new flow (dev profile):**
- `alice → ada` PENDING — populate tutor inbox UI
- `grace → linus` ACCEPTED w/ reply — happy-path student view
- `dave → ada` REJECTED w/ reply — rejection UX

**Recommended UX:**
- `422` on tutoring-request lifecycle endpoints reuses the existing `ErrorResponse.message`; no new `code` discriminator needed
- The `incoming` endpoint returns 404 vs 422 for "never applied" vs "applied but not approved" — useful to show different empty states

**Tests:** 11/11 green (added 2 new integration tests covering create → list → respond → terminal-state + can't-self-request)
