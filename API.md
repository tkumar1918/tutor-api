# Tutor API — Reference for Frontend

> Single-source-of-truth for building a client against this backend. Types are written in TypeScript-style notation; the wire format is JSON.
>
> Live interactive: `GET /swagger-ui.html`. Raw OpenAPI: `GET /v3/api-docs`.
>
> Looking for "who can call what?" — see [ROLES.md](ROLES.md) for the human-readable capability matrix.

## 1. Base URL and headers

```
Base URL:        http://localhost:8080
API prefix:      /api/v1
Content-Type:    application/json
Authorization:   Bearer <jwt>          # required for protected endpoints
```

## 2. Common envelopes

```ts
type ApiResponse<T> = {
  success: true;
  message?: string;     // present on 201 / PUT / PATCH; absent on plain GET
  data?: T;
};

type ErrorResponse = {
  timestamp: string;    // ISO-8601 instant
  status: number;       // HTTP status
  error: string;        // "Not Found", "Bad Request", ...
  code?: AuthErrorCode; // Optional machine-readable discriminator (see §8.5).
                        // Present on auth-related 401/403 so the UI can show a precise toast.
  message: string;
  path: string;
  fieldErrors?: Array<{ field: string; message: string; rejectedValue: unknown }>;
};

type AuthErrorCode =
  | 'MISSING_TOKEN'           // No Authorization header on a protected endpoint
  | 'INVALID_TOKEN'           // Signature, issuer, or format check failed
  | 'TOKEN_EXPIRED'           // JWT past its `exp`
  | 'TOKEN_VERSION_MISMATCH'  // tv claim stale — admin changed your role; re-login
  | 'INVALID_CREDENTIALS'     // /auth/login username/password mismatch
  | 'FORBIDDEN';              // Authenticated but role/ownership check failed

type PageResponse<T> = {
  content: T[];
  page: number;          // zero-indexed
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
```

## 3. Roles — derived, not stored

> Full capability matrix in [ROLES.md](ROLES.md). Quick technical summary below.

There is no `role` column on `users`. Authorities are computed at login time:

| Authority | Granted when |
|---|---|
| (none — implicit "any authenticated user") | always; lets the user enroll, browse, view own data |
| `ROLE_TUTOR` | user has a `TutorProfile` with `status = APPROVED` |
| `ROLE_ADMIN` | `users.admin = true` (set out-of-band via SQL) |

A single user can be **both** a learner (enroll in courses) AND a tutor (list courses) simultaneously. Apply for the tutor side; until approved, the user is a learner only.

To make someone an admin:

```sql
UPDATE users SET admin = TRUE WHERE username = 'someone';
-- the user must log in again to get a JWT with ROLE_ADMIN
```

## 4. Enums

```ts
type Expertise              = 'MATH' | 'SCIENCE' | 'PROGRAMMING' | 'LANGUAGES' | 'MUSIC' | 'ART' | 'OTHER';
type Subject                = 'MATH' | 'PHYSICS' | 'CHEMISTRY' | 'BIOLOGY' | 'COMPUTER_SCIENCE' | 'ENGLISH' | 'HISTORY' | 'OTHER';
type Level                  = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
type EnrollmentStatus       = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
type TutorApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
type TutoringRequestStatus  = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
```

### The two business models

An APPROVED tutor can earn in **two independent ways**:

| Model | Resource | Price field | How it works |
|---|---|---|---|
| **Packaged courses** | `Course` | `priceCents` (per-course flat fee) | Tutor publishes courses, learners enroll via `POST /enrollments` |
| **1:1 private tutoring** | `TutoringRequest` | `TutorProfile.hourlyRateCents` (per-hour rate) | Student sends a booking inquiry; tutor accepts or rejects |

`hourlyRateCents` (profile) and `priceCents` (course) are **independent** — there is no server-side relation between them. A tutor with `hourlyRateCents = 7500` can publish a course at any price. They model different products: a course is a packaged offering, 1:1 sessions are hourly bookings.

## 5. Resource types

```ts
type UserResponse = {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;   // YYYY-MM-DD
  admin: boolean;
  createdAt: string;            // ISO-8601 instant
  updatedAt: string;
};

type AuthResponse = {
  token: string;                // raw JWT (HS512)
  tokenType: 'Bearer';
  username: string;
  authorities: string[];        // e.g. [], ["ROLE_TUTOR"], ["ROLE_ADMIN"]
};

type CurrentUserResponse = {
  user: UserResponse;
  authorities: string[];        // same shape as AuthResponse.authorities
  tutorProfile?: TutorProfileResponse;  // present if the user applied to teach
};

type TutorProfileResponse = {
  id: number;
  userId: number;
  firstName: string;            // mirrored from the linked User for convenience
  lastName: string;
  // NOTE: email is intentionally NOT exposed here — the catalog is public, exposing
  // tutor email would be a PII leak. The tutor themselves can see their own email via
  // GET /api/v1/me.user.email.
  bio: string | null;
  expertise: Expertise;
  qualifications: string | null; // free-form: degrees, certifications (≤ 500 chars)
  yearsOfExperience: number;     // 0–60
  hourlyRateCents: number;       // integer cents — the implied rate for 1:1 tutoring requests
  status: TutorApplicationStatus;
  appliedAt: string;
  reviewedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
  updatedAt: string;
};

type TutoringRequestResponse = {
  id: number;
  studentId: number;
  studentName: string;            // "firstName lastName"
  tutorId: number;                // TutorProfile id (not the tutor's User id)
  tutorName: string;
  subject: Subject;
  message: string;                // what the student wants to learn / their availability
  status: TutoringRequestStatus;
  tutorReply: string | null;      // set on ACCEPTED / REJECTED, null while PENDING / CANCELLED
  createdAt: string;
  respondedAt: string | null;     // set when tutor responds or student cancels
};

type NotificationCountsResponse = {
  tutorPendingRequests: number;       // 0 if caller isn't an APPROVED tutor
  adminPendingApplications: number;   // 0 if caller isn't admin
};

type CourseResponse = {
  id: number;
  title: string;
  description: string | null;
  subject: Subject;
  level: Level;
  priceCents: number;           // integer, in cents
  tutorId: number;              // TutorProfile id, not User id
  tutorName: string;            // "firstName lastName"
  createdAt: string;
  updatedAt: string;
};

type EnrollmentResponse = {
  id: number;
  userId: number;
  userName: string;
  courseId: number;
  courseTitle: string;
  enrolledAt: string;
  status: EnrollmentStatus;
};
```

## 6. Pagination — universal query params

| Param | Default | Notes |
|---|---|---|
| `page` | `0` | zero-indexed |
| `size` | `20` | max not enforced; keep ≤ 100 |
| `sort` | per-endpoint | `field,asc` or `field,desc` |

Per-endpoint filters documented inline.

---

## 7. Endpoints

### 7.1 Auth — public

#### `POST /api/v1/auth/register`

Single registration. Creates the `User`. Everyone starts as a learner.

```ts
type RegisterRequest = {
  username: string;        // 3–50 chars, unique
  email: string;           // valid email, ≤ 120 chars, unique
  password: string;        // 8–100 chars
  firstName: string;       // ≤ 60 chars
  lastName: string;        // ≤ 60 chars
  dateOfBirth?: string;    // optional, YYYY-MM-DD, must be in the past
};
```

Responses:
- **`201 Created`** — `ApiResponse<AuthResponse>` (`message: "Registered"`)
- **`400`** — validation errors in `fieldErrors`
- **`409`** — `"Username already taken"` / `"Email already registered"`

#### `POST /api/v1/auth/login`

```ts
type LoginRequest = { username: string; password: string };
```

Responses:
- **`200 OK`** — `ApiResponse<AuthResponse>`. `authorities` reflects current user state (e.g. includes `ROLE_TUTOR` only if the user's application was approved).
- **`401`** — invalid credentials
- **`400`** — empty fields

> **JWTs are auto-invalidated on role changes.** When an admin approves a tutor application, the server bumps the user's `token_version` and the embedded `tv` claim in the user's old JWT no longer matches — so the next request comes back **401**. The client should treat 401 as "session needs refreshing" and route to login. See [ROLES.md → Token versioning](ROLES.md) for the mechanism.

---

### 7.2 Me — current user

#### `GET /api/v1/me` — any authenticated

```ts
// Returns ApiResponse<CurrentUserResponse>
```

Use this immediately after login to know which dashboard to render. `tutorProfile` is present iff the user has applied (regardless of approval status).

#### `PUT /api/v1/me` — any authenticated

Update your own name / date of birth. Email and username are immutable here.

```ts
type UserUpdateRequest = {
  firstName?: string;       // ≤ 60 chars
  lastName?: string;        // ≤ 60 chars
  dateOfBirth?: string;     // YYYY-MM-DD, past
};
```

Response: **`200`** — `ApiResponse<CurrentUserResponse>` (`message: "Profile updated"`).

> GET and PUT on `/me` return the **same** `CurrentUserResponse` shape so the client can replace its cached identity wholesale after an edit (no field-by-field merging).

#### `GET /api/v1/me/notifications` — any authenticated

Counts of items waiting for *the caller's* action right now. Drive nav-item badges off this — render a badge only when the matching count is greater than zero.

Response: **`200`** — `ApiResponse<NotificationCountsResponse>`.

| Field | Counts |
|---|---|
| `tutorPendingRequests` | Tutoring requests in the caller's inbox with `status = PENDING`. `0` when the caller isn't an APPROVED tutor. |
| `adminPendingApplications` | Tutor applications with `status = PENDING` across the whole system. `0` when the caller isn't an admin. |

> **Not a notification system.** No read/unread tracking. A counter goes down when the underlying item leaves PENDING (tutor responds, admin reviews) — not when the caller "sees" it. Re-fetch on relevant mutations (after `PATCH /tutoring-requests/{id}` or `POST /admin/tutor-applications/{id}/review`) to keep the badge fresh.

---

### 7.3 Tutor applications

Anyone authenticated can apply once. Admins review.

#### `POST /api/v1/me/tutor-application` — any authenticated

```ts
type TutorApplicationRequest = {
  bio?: string;                 // ≤ 1000 chars
  expertise: Expertise;
  qualifications?: string;      // free-form, ≤ 500 chars (degrees, certs)
  yearsOfExperience: number;    // integer 0–60
  hourlyRateCents: number;      // integer ≥ 0 (e.g. 7500 for $75/hr)
};
```

Responses:
- **`201 Created`** — `ApiResponse<TutorProfileResponse>` with `status: "PENDING"`
- **`400`** — validation
- **`409`** — `"You already have a tutor application"` (one per user)

#### `GET /api/v1/me/tutor-application` — any authenticated

Returns the calling user's own application, in whatever state.

- **`200`** — `ApiResponse<TutorProfileResponse>`
- **`404`** — caller has not applied

#### `GET /api/v1/admin/tutor-applications` — **ADMIN**

Paginated list. Default sort `appliedAt,asc`.

| Param | Type | Description |
|---|---|---|
| `status` | `TutorApplicationStatus` | filter by status (typically `PENDING`) |

Response: **`200`** — `ApiResponse<PageResponse<TutorProfileResponse>>`

#### `POST /api/v1/admin/tutor-applications/{id}/review` — **ADMIN**

```ts
type TutorApplicationReviewRequest = {
  status: 'APPROVED' | 'REJECTED';
  rejectionReason?: string;   // required iff status === 'REJECTED', ≤ 500 chars
};
```

Responses:
- **`200`** — `ApiResponse<TutorProfileResponse>` (`message: "Application reviewed"`)
- **`400`** — body validation
- **`422`** — `"Application is already APPROVED/REJECTED"` (you can't review twice) or `"rejectionReason is required when rejecting"`
- **`404`** — no such application

---

### 7.4 Tutors (public catalog + self-edit)

> Tutor profiles are created via `POST /me/tutor-application`. Only `APPROVED` profiles are visible in the public catalog.

#### `GET /api/v1/tutors` — public

Lists APPROVED tutors. Default sort `appliedAt,asc`.

| Param | Type |
|---|---|
| `expertise` | `Expertise` |
| `search` | substring match on first or last name |

Response: **`200`** — `ApiResponse<PageResponse<TutorProfileResponse>>`

#### `GET /api/v1/tutors/{id}` — public

Only returns APPROVED profiles. PENDING/REJECTED profiles return **`404`**.

Response: **`200`** — `ApiResponse<TutorProfileResponse>`

#### `GET /api/v1/tutors/{id}/courses` — public

Paginated. Default sort `title,asc`. Response: **`200`** — `ApiResponse<PageResponse<CourseResponse>>`.

#### `PUT /api/v1/tutors/me` — **TUTOR** (must be APPROVED)

```ts
type TutorUpdateRequest = {
  bio?: string;
  expertise?: Expertise;
  qualifications?: string;
  yearsOfExperience?: number;   // 0–60
  hourlyRateCents?: number;     // integer ≥ 0
};
```

Response: **`200`** — `ApiResponse<TutorProfileResponse>` (`message: "Profile updated"`).

- **`403`** — caller lacks `ROLE_TUTOR` (no application or not approved)
- **`422`** — profile exists but status is PENDING/REJECTED

#### `DELETE /api/v1/tutors/{id}` — **ADMIN**

Cascades to the tutor's courses (and their enrollments).

Response: **`204`** · **`404`** if not found.

---

### 7.5 Courses

#### `GET /api/v1/courses` — public

Paginated. Default sort `title,asc`.

| Param | Type |
|---|---|
| `tutorId` | `number` — filter by tutor (TutorProfile id) |
| `subject` | `Subject` |
| `level` | `Level` |
| `search` | substring on `title` |

Response: **`200`** — `ApiResponse<PageResponse<CourseResponse>>`

#### `GET /api/v1/courses/{id}` — public

Response: **`200`** — `ApiResponse<CourseResponse>` · **`404`** otherwise.

#### `GET /api/v1/courses/mine` — **TUTOR**

The current tutor's own courses.

#### `POST /api/v1/courses` — **TUTOR**

Tutor is derived from JWT — do NOT send a tutor id.

```ts
type CourseCreateRequest = {
  title: string;              // ≤ 200 chars
  description?: string;       // ≤ 2000 chars
  subject: Subject;
  level: Level;
  priceCents: number;         // ≥ 0
};
```

Responses:
- **`201`** — `ApiResponse<CourseResponse>` + `Location` header
- **`400`** — validation
- **`403`** — caller lacks `ROLE_TUTOR`
- **`422`** — caller has a non-approved tutor application

#### `PUT /api/v1/courses/{id}` — **TUTOR (owner)** or **ADMIN**

```ts
type CourseUpdateRequest = {
  title?: string;
  description?: string;
  subject?: Subject;
  level?: Level;
  priceCents?: number;
};
```

Response: **`200`** — `ApiResponse<CourseResponse>` · **`403`** if not owner.

#### `DELETE /api/v1/courses/{id}` — **TUTOR (owner)** or **ADMIN**

Response: **`204`** · **`403`** if not owner · **`404`** if not found.

#### `GET /api/v1/courses/{id}/enrollments` — **TUTOR (owner)** or **ADMIN**

The tutor's "my students for this course" view. Caller must own the course (or be admin).

Paginated. Default sort `enrolledAt,asc`.

| Param | Type | Description |
|---|---|---|
| `status` | `EnrollmentStatus` | filter by enrollment status |

Responses:
- **`200`** — `ApiResponse<PageResponse<EnrollmentResponse>>`
- **`401`** — no token
- **`403`** — tutor doesn't own this course (`code: "FORBIDDEN"`)
- **`404`** — course not found

---

### 7.6 Enrollments

> Any authenticated user can enroll. The user is **always the caller** — there is no `userId` field in the body.

#### `POST /api/v1/enrollments` — any authenticated

```ts
type EnrollmentCreateRequest = {
  courseId: number;
};
```

Server sets `userId = current user`, `enrolledAt = now()`, `status = ACTIVE`.

Responses:
- **`201`** — `ApiResponse<EnrollmentResponse>` + `Location` header
- **`400`** — validation
- **`404`** — course not found
- **`409`** — already enrolled in this course

#### `GET /api/v1/enrollments/mine` — any authenticated

Caller's own enrollments. Default sort `enrolledAt,asc`.

| Param | Type |
|---|---|
| `status` | `EnrollmentStatus` |

Response: **`200`** — `ApiResponse<PageResponse<EnrollmentResponse>>`

#### `GET /api/v1/enrollments` — **ADMIN**

Same filters as `/mine` plus `userId` and `courseId`.

#### `GET /api/v1/enrollments/{id}` — owner / course's TUTOR / ADMIN

Response: **`200`** · **`403`** if none of the above · **`404`** if not found.

#### `PATCH /api/v1/enrollments/{id}/status` — owner or ADMIN

```ts
type EnrollmentStatusUpdateRequest = { status: EnrollmentStatus };
```

Response: **`200`** — `ApiResponse<EnrollmentResponse>` (`message: "Status updated"`).

#### `DELETE /api/v1/enrollments/{id}` — owner or ADMIN

Hard delete. To keep history, PATCH to `CANCELLED` instead.

Response: **`204`**.

---

### 7.7 Tutoring requests — 1:1 private bookings

> Independent of `Course`. A student sends a free-form inquiry to an APPROVED tutor; the tutor accepts or rejects with an optional reply note. Sessions/payments happen out-of-band — this resource only tracks the booking intent.

#### `POST /api/v1/tutoring-requests` — any authenticated

```ts
type TutoringRequestCreateRequest = {
  tutorId: number;          // TutorProfile id; must reference an APPROVED tutor
  subject: Subject;
  message: string;          // non-blank, ≤ 1000 chars
};
```

The student id is taken from the JWT — never sent in the body. A user cannot send a request to their own tutor profile.

Responses:
- **`201`** — `ApiResponse<TutoringRequestResponse>` + `Location`. Created with `status: "PENDING"`.
- **`400`** — validation
- **`404`** — tutor not found, or exists but not APPROVED
- **`422`** — sending a request to yourself

#### `GET /api/v1/tutoring-requests/mine` — any authenticated

The caller's outgoing requests. Default sort `createdAt,desc`.

| Param | Type |
|---|---|
| `status` | `TutoringRequestStatus` |

Response: **`200`** — `ApiResponse<PageResponse<TutoringRequestResponse>>`

#### `GET /api/v1/tutoring-requests/incoming` — **TUTOR** (APPROVED)

Requests that have been sent **to** the calling tutor. Default sort `createdAt,desc`.

Same `status` filter as `/mine`.

Responses:
- **`200`** — `ApiResponse<PageResponse<TutoringRequestResponse>>`
- **`404`** — caller has never applied to be a tutor
- **`422`** — caller has a tutor profile but it's PENDING / REJECTED (not APPROVED)

#### `GET /api/v1/tutoring-requests/{id}` — student / targeted tutor / ADMIN

Response: **`200`** — `ApiResponse<TutoringRequestResponse>` · **`403`** if you're neither the student nor the targeted tutor · **`404`** if not found.

#### `PATCH /api/v1/tutoring-requests/{id}` — **the targeted tutor**

Accept or reject a PENDING request.

```ts
type TutoringRequestRespondRequest = {
  status: 'ACCEPTED' | 'REJECTED';   // anything else → 422
  tutorReply?: string;               // ≤ 1000 chars, optional explanation/note
};
```

Responses:
- **`200`** — `ApiResponse<TutoringRequestResponse>` (`message: "Response recorded"`)
- **`403`** — caller is not the tutor on this request
- **`422`** — request is already in a terminal state (ACCEPTED / REJECTED / CANCELLED)

#### `DELETE /api/v1/tutoring-requests/{id}` — the requesting student (or ADMIN)

Soft cancel — sets `status = "CANCELLED"`. Only valid while still PENDING. The row is **not** deleted; cancellations stay visible for history (use the resource's `respondedAt` to know when).

Responses:
- **`200`** — `ApiResponse<TutoringRequestResponse>` (`message: "Request cancelled"`)
- **`403`** — caller is not the student who created the request (and isn't admin)
- **`422`** — request already responded to / cancelled

#### `GET /api/v1/admin/tutoring-requests` — **ADMIN**

Cross-tenant list, paginated. Default sort `createdAt,desc`.

| Param | Type | Description |
|---|---|---|
| `studentId` | `number` | filter by student `User.id` |
| `tutorId` | `number` | filter by `TutorProfile.id` |
| `status` | `TutoringRequestStatus` | filter by status |

Response: **`200`** — `ApiResponse<PageResponse<TutoringRequestResponse>>`

---

## 8. Status code conventions

| Code | When |
|---|---|
| `200 OK` | Read or update succeeded |
| `201 Created` | Resource created; `Location` header points to it |
| `204 No Content` | Delete succeeded; body empty |
| `400 Bad Request` | Body failed Bean Validation. `fieldErrors[]` lists offenders. |
| `401 Unauthorized` | Missing/expired/invalid `Authorization: Bearer …` |
| `403 Forbidden` | Token valid but role/ownership insufficient (e.g. a non-tutor hits `POST /courses`, or a tutor edits another tutor's course) |
| `404 Not Found` | Path id does not resolve |
| `409 Conflict` | Unique constraint violated — duplicate email, duplicate enrollment, second tutor application, etc. |
| `422 Unprocessable Entity` | Business rule — e.g. reviewing an already-reviewed application, or asking for a tutor action when your application is still PENDING |
| `500 Internal Server Error` | Unhandled; check logs |

## 9. End-to-end sample flow

Tutor onboarding + a learner enrolling:

```bash
# 1. Ada registers (regular user, no tutor role yet)
ADA=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"ada","email":"ada@example.com","password":"Secret123!",
       "firstName":"Ada","lastName":"Lovelace","dateOfBirth":"1990-12-10"}' \
  | jq -r .data.token)

# 2. Ada applies to teach
curl -X POST http://localhost:8080/api/v1/me/tutor-application \
  -H "Authorization: Bearer $ADA" -H 'Content-Type: application/json' \
  -d '{"bio":"Math + computing","expertise":"MATH",
       "qualifications":"MSc Math, ICM 2018","yearsOfExperience":10,
       "hourlyRateCents":7500}'

# 3. Admin (provisioned via SQL: UPDATE users SET admin=TRUE WHERE username='root')
ADMIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"root","password":"Secret123!"}' | jq -r .data.token)

# 4. Admin lists and approves the application
APP_ID=$(curl -s -H "Authorization: Bearer $ADMIN" \
  'http://localhost:8080/api/v1/admin/tutor-applications?status=PENDING' \
  | jq -r '.data.content[0].id')

curl -X POST "http://localhost:8080/api/v1/admin/tutor-applications/$APP_ID/review" \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"status":"APPROVED"}'

# 5. Ada re-logs in to pick up ROLE_TUTOR
ADA=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"ada","password":"Secret123!"}' | jq -r .data.token)

# 6. Ada creates a course
COURSE_ID=$(curl -s -X POST http://localhost:8080/api/v1/courses \
  -H "Authorization: Bearer $ADA" -H 'Content-Type: application/json' \
  -d '{"title":"Intro to Algorithms","subject":"COMPUTER_SCIENCE",
       "level":"BEGINNER","priceCents":4999}' | jq -r .data.id)

# 7. Grace registers and enrolls
GRACE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"grace","email":"grace@example.com","password":"Secret123!",
       "firstName":"Grace","lastName":"Hopper","dateOfBirth":"2000-12-09"}' \
  | jq -r .data.token)

curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer $GRACE" -H 'Content-Type: application/json' \
  -d "{\"courseId\":$COURSE_ID}"

# 8. Grace sees her enrollment
curl -s -H "Authorization: Bearer $GRACE" \
  http://localhost:8080/api/v1/enrollments/mine

# 9. Grace also wants 1:1 sessions — sends a tutoring request to Ada
TUTOR_ID=$(curl -s 'http://localhost:8080/api/v1/tutors' \
  | jq -r '.data.content[] | select(.firstName=="Ada") | .id')

curl -X POST http://localhost:8080/api/v1/tutoring-requests \
  -H "Authorization: Bearer $GRACE" -H 'Content-Type: application/json' \
  -d "{\"tutorId\":$TUTOR_ID,\"subject\":\"MATH\",\"message\":\"Need calc help\"}"

# 10. Ada sees the incoming request and accepts it
curl -s -H "Authorization: Bearer $ADA" \
  http://localhost:8080/api/v1/tutoring-requests/incoming
```

## 10. Frontend tips

- **Always hit `GET /me` immediately after login** — that response tells you (a) the user's name/email/DOB, (b) their authorities, (c) their tutor profile + status if any. The UI should branch on `authorities` and on `tutorProfile?.status`.
- **Switch on `code` for 401/403 toasts.** Every auth failure carries a stable `ErrorResponse.code` (see §3 / §8.5):
    - `INVALID_CREDENTIALS` → "Wrong username or password" (no redirect; let them retry)
    - `TOKEN_EXPIRED` → "Your session expired" (silent redirect to login)
    - `TOKEN_VERSION_MISMATCH` → "Your roles changed — please sign in again" (silent redirect)
    - `MISSING_TOKEN` / `INVALID_TOKEN` → silent redirect to login (don't surface)
    - `FORBIDDEN` → "You don't have access to this" (toast, stay on page)
- **Don't trust the JWT for routing.** Decode it for UX hints (expiry) but always check `/me` for the live state.
- **Form errors**: parse `error.fieldErrors[]` on `400` and map `field → input`. Show `error.message` as a banner on 4xx without `fieldErrors`.
- **Pagination**: `?page=0&size=20&sort=field,asc`. The sort separator is a comma, in one parameter value.
- **Money**: both `hourlyRateCents` and `priceCents` are integers in cents — divide by 100 for display. Never serialize money as a float.
- **Dates**: `dateOfBirth` is `YYYY-MM-DD`. Timestamps (`createdAt`, `enrolledAt`, …) are ISO-8601 instants in UTC.
- **No refresh token, no password reset, no email verification** — these are listed in the README as known gaps.
