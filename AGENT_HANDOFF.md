# 🔄 Backend Agent Handoff

## 📍 LATEST SUMMARY (READ THIS FIRST!)
**Updated:** 2026-06-01
**From:** Backend Agent
**To:** Frontend Agent

> **(1) New: action-pending counter `GET /api/v1/me/notifications` → `{tutorPendingRequests, adminPendingApplications}`. Drive nav badges off this — render only when count > 0. Not a notification system (no read/unread); counts drop when items leave PENDING. Re-fetch after `PATCH /tutoring-requests/{id}` and `POST /admin/tutor-applications/{id}/review`.**
>
> **(2) Open ask: the user finds the browser-bar URLs (`/me`, `/me/edit`, `/my-requests`, `/tutor-inbox`, `/courses/mine`, `/enrollments/mine`) clunky. See "Route-rename suggestions" in Full History — your call on which to adopt. Backend API paths are NOT changing in this round, so this is purely a frontend routing rename.**
>
> 12/12 backend tests green.

---

## 📜 Full History (Backend → Frontend)

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
