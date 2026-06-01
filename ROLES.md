# Roles & Capabilities

The human-readable map of who can do what in the Tutor API. For exact request/response shapes see [API.md](API.md).

## The four states a request can be in

| State | Meaning | How to recognize |
|---|---|---|
| **Anonymous** | No `Authorization` header, or invalid/expired token | `CurrentUserResponse` cannot be fetched |
| **Regular user** | Valid JWT — `authorities` is empty `[]` | default after registration |
| **Tutor** | Valid JWT — `authorities` contains `ROLE_TUTOR` | only after admin approves the user's tutor application |
| **Admin** | Valid JWT — `authorities` contains `ROLE_ADMIN` | only when `users.admin = TRUE` in the database |

Authorities **stack** — a single human can be a tutor *and* an admin. Every authenticated user is implicitly a "regular user" too (they keep that capability even after becoming a tutor or admin).

### Token versioning — how role changes propagate to live sessions

Every `User` row has a `token_version` counter (default `0`). It's embedded in every JWT as the `tv` claim. The auth filter rejects any JWT whose `tv` doesn't match the live DB value — the request comes back **401 Unauthorized**.

Admin approval bumps the target user's `token_version` by 1, so:

1. Eve logs in → JWT carries `tv=0`.
2. Eve applies to be a tutor.
3. Admin approves → server runs `user.token_version = 1`.
4. Eve's existing JWT (`tv=0`) now mismatches → every subsequent request is **401**.
5. Eve hits `/auth/login` again → new JWT with `tv=1` and `authorities: ["ROLE_TUTOR"]`.

**Frontend handling**: treat 401 on a request that was previously authenticated as "your session needs refreshing" — clear the cached JWT, route to login. (A token *expiry* 401 looks the same — same UX works for both.)

Rejection does **not** bump `token_version` (authorities don't change). The user just sees the rejection reason on their next `/me/tutor-application` call.

---

## Capability matrix

✅ = allowed · ❌ = forbidden (403) · 🟡 = ownership/state checked at runtime (see notes)

| Action | Anonymous | Regular user | Tutor (APPROVED) | Admin |
|---|---|---|---|---|
| **Auth** | | | | |
| `POST /auth/register` | ✅ | ✅ | ✅ | ✅ |
| `POST /auth/login` | ✅ | ✅ | ✅ | ✅ |
| **Identity** | | | | |
| `GET /me` | ❌ 401 | ✅ | ✅ | ✅ |
| `PUT /me` (edit name/DOB) | ❌ 401 | ✅ | ✅ | ✅ |
| `GET /me/notifications` (action counts) | ❌ 401 | ✅ (all zeros) | ✅ (tutor count) | ✅ (admin count) |
| **Tutor applications** | | | | |
| `POST /me/tutor-application` (apply) | ❌ 401 | ✅ | 🟡 409 (already exists) | 🟡 409 |
| `GET /me/tutor-application` | ❌ 401 | 🟡 404 if none | ✅ | 🟡 404 if none |
| `GET /admin/tutor-applications` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |
| `POST /admin/tutor-applications/{id}/review` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |
| **Tutor catalog** | | | | |
| `GET /tutors` (only APPROVED) | ✅ | ✅ | ✅ | ✅ |
| `GET /tutors/{id}` (only APPROVED) | ✅ | ✅ | ✅ | ✅ |
| `GET /tutors/{id}/courses` | ✅ | ✅ | ✅ | ✅ |
| `PUT /tutors/me` (edit own profile) | ❌ 401 | ❌ 403 | ✅ | ❌ 403 (no profile of own) |
| `DELETE /tutors/{id}` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |
| **Course catalog** | | | | |
| `GET /courses` | ✅ | ✅ | ✅ | ✅ |
| `GET /courses/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /courses/mine` | ❌ 401 | ❌ 403 | ✅ | ❌ 403 |
| `POST /courses` (create own course) | ❌ 401 | ❌ 403 | ✅ | ❌ 403 (no tutor profile) |
| `PUT /courses/{id}` | ❌ 401 | ❌ 403 | 🟡 owner only | ✅ |
| `DELETE /courses/{id}` | ❌ 401 | ❌ 403 | 🟡 owner only | ✅ |
| `GET /courses/{id}/enrollments` ("my students") | ❌ 401 | ❌ 403 | 🟡 owner only | ✅ |
| **Enrollments** | | | | |
| `POST /enrollments` (enroll self) | ❌ 401 | ✅ | ✅ | ✅ |
| `GET /enrollments/mine` | ❌ 401 | ✅ | ✅ | ✅ |
| `GET /enrollments` (list all) | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |
| `GET /enrollments/{id}` | ❌ 401 | 🟡 if owner | 🟡 if course's tutor | ✅ |
| `PATCH /enrollments/{id}/status` | ❌ 401 | 🟡 if owner | ❌ 403 | ✅ |
| `DELETE /enrollments/{id}` (unenroll) | ❌ 401 | 🟡 if owner | ❌ 403 | ✅ |
| **Tutoring requests (1:1 bookings)** | | | | |
| `POST /tutoring-requests` (send to a tutor) | ❌ 401 | ✅ | ✅ | ✅ |
| `GET /tutoring-requests/mine` (outgoing) | ❌ 401 | ✅ | ✅ | ✅ |
| `GET /tutoring-requests/incoming` (received) | ❌ 401 | ❌ 404 | ✅ | 🟡 404 if no profile |
| `GET /tutoring-requests/{id}` | ❌ 401 | 🟡 student | 🟡 targeted tutor | ✅ |
| `PATCH /tutoring-requests/{id}` (accept/reject) | ❌ 401 | ❌ 403 | 🟡 targeted tutor only | ❌ 403 |
| `DELETE /tutoring-requests/{id}` (cancel) | ❌ 401 | 🟡 student only | ❌ 403 | ✅ |
| `GET /admin/tutoring-requests` (overview) | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |

### Notes on the 🟡 cells

- **Tutor owner check** on `PUT/DELETE /courses/{id}` — the tutor must be the one who created the course. Any other tutor gets 403 *"You don't own this course"*. Admins bypass.
- **Enrollment owner check** on `PATCH /status` and `DELETE` — only the enrolled user (or an admin) can change/cancel. A tutor cannot drop a student from their course (use admin for moderation).
- **Enrollment visibility** on `GET /enrollments/{id}` — the enrolled user sees their own, the tutor of the enrolled course sees who's in their course, admin sees everything.
- **Tutoring request visibility** on `GET /tutoring-requests/{id}` — only the student who sent it, the tutor who received it, or an admin. No public access.
- **Tutoring request respond** (`PATCH`) — *only* the tutor on the request can accept/reject. Admins do **not** bypass this on purpose: accepting on someone's behalf would be a footgun. If admin moderation is needed, do it via DB / a future admin-only override endpoint.
- **Tutoring request cancel** (`DELETE`) — soft-cancel that flips status to CANCELLED; the row stays. Only valid while still PENDING.
- **Tutor sees their own incoming requests** via `GET /tutoring-requests/incoming` — requires an APPROVED profile. PENDING/REJECTED applicants get **422**; non-applicants get **404**.

---

## Per-role walkthroughs

### As an anonymous visitor

You can:

- Browse `/courses` and `/tutors` (only APPROVED tutors and their courses appear here)
- Inspect any specific course or tutor by id
- Hit `/auth/register` to become a regular user
- Hit `/auth/login` if you already have an account

You cannot:

- See enrollments — not even count of students per course
- See PENDING or REJECTED tutor profiles
- See anyone's email or date of birth (only first/last name + bio + expertise + rate)

### As a regular user (newly registered)

Plus everything anonymous can do, you can now:

- View and edit your own profile via `/me` and `PUT /me`
- Enroll in any course via `POST /enrollments` (course id is enough — the server picks you up from the JWT)
- See your own enrollments via `/enrollments/mine`
- Mark your enrollments `COMPLETED` or `CANCELLED`, or delete them outright
- Apply to become a tutor via `POST /me/tutor-application`

You cannot:

- Create courses (yet — you need to be APPROVED first)
- See or modify anyone else's enrollments
- Hit any `/admin/*` endpoint

### As a tutor (an APPROVED user)

Plus everything a regular user can do, you can now:

- Create courses via `POST /courses` — the `tutorId` is derived from your JWT, not the body
- See your own courses via `/courses/mine`
- Update and delete courses you own (`PUT/DELETE /courses/{id}`)
- Update your tutor profile (bio, expertise, qualifications, experience, rate) via `PUT /tutors/me`
- See who is enrolled in your courses via `GET /courses/{id}/enrollments` (the "my students" view) or per-row via `GET /enrollments/{id}`
- See 1:1 tutoring requests sent to you via `GET /tutoring-requests/incoming`, and accept or reject them via `PATCH /tutoring-requests/{id}`

You cannot:

- Edit another tutor's profile or courses (403 *"You don't own this course"*)
- Mark a student's enrollment as `COMPLETED` or kick them out — only the student themselves or an admin can change enrollment state
- Approve/reject tutor applications — that's admin-only
- Accept or reject a tutoring request that wasn't sent to you (403 *"Only the targeted tutor can respond"*)

If your application is still PENDING or got REJECTED, you don't have `ROLE_TUTOR` yet — you're still a regular user. You'll see your application status via `GET /me/tutor-application` or `GET /me` (the `tutorProfile` field appears with `status: "PENDING"`/`"REJECTED"`).

### As an admin

Plus everything a tutor *and* regular user can do, you can now:

- Review tutor applications (`GET /admin/tutor-applications?status=PENDING`)
- Approve or reject applications with a reason (`POST /admin/tutor-applications/{id}/review`)
- List all tutor applications across all states (`GET /admin/tutor-applications`) and all enrollments (`GET /enrollments`)
- Override every ownership check — edit any course, modify any enrollment, delete any tutor

You should NOT (no enforcement, but it's bad practice):

- Approve your own tutor application — if you're an admin, apply for `ROLE_TUTOR` like everyone else, and have *another* admin review it (or accept the conflict-of-interest in dev)
- Use admin powers for normal student/tutor work — keep an unprivileged account for personal enrollments so you can verify the regular user flow

> Admins are NOT created via the API. The first admin is provisioned by SQL:
>
> ```sql
> UPDATE users SET admin = TRUE WHERE username = 'someone';
> ```
>
> The user must log in again to get a JWT with `ROLE_ADMIN`.

---

## State transitions — how a user moves between roles

```
[anonymous]
    │
    │ POST /auth/register
    ▼
[regular user]  ──────────────────────────────┐
    │                                          │
    │ POST /me/tutor-application                │ UPDATE users SET admin=TRUE ...
    ▼                                          │ then re-login
[regular user, application PENDING]            │
    │                                          │
    │ admin reviews                             │
    ├─ APPROVED ──► re-login ──► [tutor]       │
    │                              │           │
    │                              └───────────┤
    │                                          │
    └─ REJECTED ──► [regular user, REJECTED]   ▼
                       (can't re-apply yet)  [admin]
                                                │
                                                │ (admins keep tutor/regular caps too)
                                                ▼
                                          [admin + tutor + regular]
```

Key facts:
- Going from regular → tutor needs **admin approval** + a **fresh JWT**.
- Going from regular → admin needs **direct SQL access** + a **fresh JWT**. There is intentionally no API for this.
- A REJECTED application **cannot be re-submitted** today — the user is stuck. (Not a hard limitation, just not implemented; the unique constraint on `tutor_profiles.user_id` prevents a second `POST /me/tutor-application`.)
- Once `ROLE_TUTOR` is granted, it persists until an admin manually wipes the `TutorProfile` (`DELETE /tutors/{id}`) or flips the status field directly in the DB.

---

## Hard rules that hold regardless of role

- **Identity is always the JWT.** No endpoint accepts `userId`, `studentId`, or `tutorId` in the request body for actions that mutate the caller's own data. This means a student cannot enroll on someone else's behalf, and a tutor cannot create a course "as" another tutor — even with a malformed payload.
- **Public catalog only shows `APPROVED` tutors.** PENDING and REJECTED profiles never appear in `GET /tutors` and return 404 on `GET /tutors/{id}`.
- **No soft delete.** `DELETE` is a hard delete. Use `PATCH /enrollments/{id}/status` with `CANCELLED` if you want to preserve history.
- **Email is unique across all users.** Trying to register with an email already in use → 409. (Same for username.)
- **A user can apply to be a tutor at most once.** Second `POST /me/tutor-application` → 409 regardless of the first application's state. Re-applying after a rejection requires manual cleanup of the `tutor_profiles` row.

---

## Seeded accounts for trying each role

All share password `Secret123!`. See [`data.sql`](src/main/resources/data.sql).

| Username | Role state | Try this |
|---|---|---|
| `root` | Admin | log in, review Carol's PENDING application via `/admin/tutor-applications?status=PENDING` |
| `ada` | Tutor (APPROVED) | log in, hit `/courses/mine` to see her two MATH courses; hit `/tutoring-requests/incoming` — Alice has a PENDING request, Dave's was REJECTED |
| `linus` | Tutor (APPROVED) | log in, hit `/courses/mine` to see his two CS courses; hit `/tutoring-requests/incoming` — Grace's CS request is already ACCEPTED |
| `carol` | Regular user (application PENDING) | log in, hit `/me/tutor-application` to see status; try `POST /courses` → 403 |
| `dave` | Regular user (application REJECTED) | log in, hit `/me/tutor-application` to see the rejection reason; `/tutoring-requests/mine` shows his REJECTED 1:1 request to Ada |
| `grace` | Regular user (2 enrollments, 1 tutoring request) | log in, hit `/enrollments/mine` — one ACTIVE, one COMPLETED; `/tutoring-requests/mine` shows her ACCEPTED 1:1 request to Linus |
| `alice` | Regular user (2 enrollments, 1 tutoring request) | log in, hit `/enrollments/mine` — one ACTIVE, one CANCELLED; `/tutoring-requests/mine` shows her PENDING 1:1 request to Ada |
