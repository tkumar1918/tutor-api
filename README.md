# Tutor API

A beginner-friendly Spring Boot REST API that demonstrates the standard patterns you'll see in real production codebases:

- Layered architecture: Controller → Service → Repository → Entity
- DTO request/response separation with Bean Validation
- Entity↔DTO mapping with **MapStruct**
- Global exception handling with a uniform `ErrorResponse`
- JPA relationships (one-to-many, join entity for many-to-many)
- Pagination, sorting, dynamic filtering (`JpaSpecificationExecutor`)
- **JWT** stateless authentication + role-based authorization
- **OpenAPI / Swagger UI** documentation
- Tests at every layer (`@DataJpaTest`, Mockito unit, `@WebMvcTest`, `@SpringBootTest`)

## Domain

```
       ┌────────┐
       │  User  │ (login + name + email + DOB + admin flag)
       └───┬────┘
           │ 0..1 (optional, created when applying to teach)
           ▼
   ┌────────────────┐
   │ TutorProfile   │ status = PENDING / APPROVED / REJECTED
   └───┬────────────┘
       │ 1..N (only APPROVED profiles can own courses)
       ▼
   ┌────────┐         ┌──────────────┐
   │ Course │◄────────┤  Enrollment  │──► User (the learner)
   └────────┘  1..N   └──────────────┘
```

Authorities are **derived from user state** at login:
- Every authenticated user can act as a learner (enroll, browse) — no explicit role
- `ROLE_TUTOR` is granted when the user has an `APPROVED` `TutorProfile`
- `ROLE_ADMIN` is granted when `users.admin = true`

One human can be both — register, enroll in some courses (learner mode), and later apply to teach.

## Prerequisites

- **JDK 25** (or set `JAVA_HOME` to whichever JDK ≥ 17 you use after editing `<java.version>` in [pom.xml](pom.xml))
- **MySQL 8** reachable on `:3306` — easiest path is a Docker container (one line below)
- Maven wrapper (`./mvnw`) — no separate Maven install needed

## Quickstart

The backend needs a MySQL it can reach. Pick one of:

**Option A — Hosted MySQL (Aiven / RDS / DigitalOcean / etc.)**

```bash
cp .env.example .env
# Edit .env: set SPRING_DATASOURCE_URL + DB_USER + DB_PASSWORD to your hosted MySQL.
# See .env.example for the exact shape, including the Aiven-style sslMode=REQUIRED URL.
docker compose up --build
open http://localhost:8080/swagger-ui.html
```

**Option B — Local MySQL in a container**

```bash
# 1. Start MySQL
docker run -d --name tutor-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=tutor_api mysql:8

# 2. Run the app on your host (default JDBC URL points at localhost:3306)
./mvnw spring-boot:run

# 3. Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Seeded accounts (dev profile only)

The app runs [`src/main/resources/data.sql`](src/main/resources/data.sql) at startup when the `dev` profile is active. Every insert is guarded by `WHERE NOT EXISTS`, so it's safe across restarts. **All seeded accounts share the same password: `Secret123!`**

| Username | Role / state | Useful for testing |
|---|---|---|
| `root` | **ADMIN** | reviewing tutor applications, listing all enrollments |
| `ada` | tutor, **APPROVED** (MATH) | logging in as a working tutor (owns Calculus 101, Linear Algebra) |
| `linus` | tutor, **APPROVED** (PROGRAMMING) | second working tutor (owns Intro to Algorithms, Advanced Linux Internals) |
| `carol` | tutor, **PENDING** | the application root has to review |
| `dave` | tutor, **REJECTED** | demonstrates the reject-with-reason flow |
| `grace` | regular user | already enrolled in 2 courses (one ACTIVE, one COMPLETED) |
| `alice` | regular user | already enrolled in 2 courses (one ACTIVE, one CANCELLED) |

To wipe and re-seed cleanly:

```bash
docker exec tutor-mysql mysql -uroot -proot \
  -e 'DROP DATABASE tutor_api; CREATE DATABASE tutor_api;'
# Restart the app — Hibernate recreates the schema and data.sql refills it.
```

To disable seeding (e.g., when running against a real DB), set `SPRING_PROFILE=prod`.

## Configuration

Override via env vars (defaults shown):

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/tutor_api?...` | Full JDBC URL — override for Docker Compose, RDS, etc. |
| `DB_USER` | `root` | MySQL user |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | dev value | Base64-encoded HS256 secret (use 256+ bits in prod) |
| `JWT_EXPIRATION_MS` | `3600000` | 1 hour |
| `SPRING_PROFILE` | `dev` | `dev` (SQL logs) or `prod` (strict) |

## Roles & flow

> Looking for "who can do what?" → see **[ROLES.md](ROLES.md)** for the full capability matrix and per-role walkthroughs. The summary below is just the registration flow.

Single registration. Every user starts as a regular learner. To also teach, the user **applies** and an admin reviews:

```
register ──► learner (can enroll right away)
   │
   └─► POST /me/tutor-application (with bio, expertise, hourly rate)
              │
              ▼
        TutorProfile (PENDING)
              │
              ▼
     admin reviews ──► APPROVED  ──► ROLE_TUTOR granted on next login
                  └──► REJECTED  ──► reason returned; user remains a learner
```

- **Regular user** (default) — browse catalog, enroll, manage own enrollments, edit own profile, apply to become a tutor, send 1:1 tutoring requests to any approved tutor.
- **`ROLE_TUTOR`** — additionally: create/edit/delete own courses; see who enrolled in own courses; accept/reject 1:1 tutoring requests sent to them.
- **`ROLE_ADMIN`** — additionally: list/approve/reject tutor applications, list all students/enrollments/tutoring-requests, override ownership.
  - Provisioned out-of-band: `UPDATE users SET admin=true WHERE username='someone';` then re-login.

> An approved tutor earns two ways: **packaged courses** (flat `priceCents` per course) and **1:1 private sessions** (the profile's `hourlyRateCents`). The two prices are independent — see [API.md → The two business models](API.md).

## Sample flows

**Apply to teach, get approved, list a course:**

```bash
# 1. Anyone registers — single endpoint
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"ada","email":"ada@example.com","password":"Secret123!",
       "firstName":"Ada","lastName":"Lovelace","dateOfBirth":"1990-12-10"}' \
  | jq -r .data.token)

# 2. Apply to become a tutor
curl -X POST http://localhost:8080/api/v1/me/tutor-application \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"bio":"Mathematician","expertise":"MATH",
       "qualifications":"MSc Mathematics","yearsOfExperience":10,
       "hourlyRateCents":7500}'

# 3. Until approved, /me/tutor-application shows status=PENDING
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me/tutor-application

# 4. (Admin side) approve the application
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"root","password":"...."}' | jq -r .data.token)
curl -X POST http://localhost:8080/api/v1/admin/tutor-applications/1/review \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"APPROVED"}'

# 5. Ada re-logs in to refresh her JWT (now includes ROLE_TUTOR)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"ada","password":"Secret123!"}' | jq -r .data.token)

# 6. Now she can create courses — tutor derived from JWT, no tutorId in body
curl -X POST http://localhost:8080/api/v1/courses \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Intro to Algorithms","subject":"COMPUTER_SCIENCE",
       "level":"BEGINNER","priceCents":4999}'

# 7. List my own courses
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/courses/mine
```

**Any registered user enrolls in a course:**

```bash
# Browse the catalog (no auth needed)
curl 'http://localhost:8080/api/v1/courses?subject=COMPUTER_SCIENCE'

# Enroll — no userId in body, derived from JWT
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"courseId":1}'

# See my enrollments
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/enrollments/mine
```

**Inspect who I'm logged in as:**

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me
# → { success, data: { user: {...}, authorities: ["ROLE_TUTOR"], tutorProfile: {...} } }
```

## Project layout

The project uses a **layered** package structure — every class is grouped by its role (controller, service, repository, …), not by its domain. This makes the pattern visible at a glance: open `controller/` to see every controller side-by-side.

```
src/main/java/dev/tushar/tutorapi/
├── TutorApiApplication.java
├── config/             OpenAPI + JPA auditing config beans
├── security/           SecurityConfig, JwtService, JwtAuthenticationFilter,
│                       CustomUserDetailsService (computes ROLE_TUTOR / ROLE_ADMIN
│                       from user state on every login), CurrentUserService
├── controller/         AuthController, CurrentUserController, TutorController,
│                       TutorApplicationController, CourseController, EnrollmentController
├── service/            AuthService, TutorService(+Impl), TutorApplicationService,
│                       CourseService(+Impl), EnrollmentService
├── repository/         UserRepository, TutorProfileRepository, CourseRepository,
│                       EnrollmentRepository
├── entity/             BaseEntity, User, TutorProfile, Course, Enrollment
│   └── enums/          Expertise, Subject, Level, EnrollmentStatus, TutorApplicationStatus
├── dto/
│   ├── request/        RegisterRequest, LoginRequest, UserUpdateRequest,
│   │                   TutorApplicationRequest, TutorApplicationReviewRequest,
│   │                   TutorUpdateRequest, CourseCreateRequest, CourseUpdateRequest,
│   │                   EnrollmentCreateRequest, EnrollmentStatusUpdateRequest
│   └── response/       ApiResponse, ErrorResponse, PageResponse, AuthResponse,
│                       CurrentUserResponse, UserResponse, TutorProfileResponse,
│                       CourseResponse, EnrollmentResponse
├── mapper/             UserMapper, TutorProfileMapper, CourseMapper, EnrollmentMapper
├── exception/          ResourceNotFoundException, DuplicateResourceException,
│                       BusinessRuleException, GlobalExceptionHandler
└── specification/      CourseSpecifications (JpaSpecificationExecutor predicates)
```

Read the **tutor-application chain** first to see the whole role-elevation flow end-to-end:
[TutorProfile entity](src/main/java/dev/tushar/tutorapi/entity/TutorProfile.java) →
[TutorProfileRepository](src/main/java/dev/tushar/tutorapi/repository/TutorProfileRepository.java) →
[TutorApplicationService](src/main/java/dev/tushar/tutorapi/service/TutorApplicationService.java) →
[TutorApplicationController](src/main/java/dev/tushar/tutorapi/controller/TutorApplicationController.java) →
[CustomUserDetailsService](src/main/java/dev/tushar/tutorapi/security/CustomUserDetailsService.java) (where the APPROVED status becomes a `ROLE_TUTOR` authority).

## Tests

```bash
./mvnw test           # unit + slice tests (uses H2 in-memory)
./mvnw verify         # everything
```

Tests mirror the layered layout:
- [repository/TutorProfileRepositoryTest](src/test/java/dev/tushar/tutorapi/repository/TutorProfileRepositoryTest.java) — `@DataJpaTest` slice (H2 in-memory), covers `searchApproved`, `findByUserId`
- [integration/AuthFlowIntegrationTest](src/test/java/dev/tushar/tutorapi/integration/AuthFlowIntegrationTest.java) — full register → apply → admin-approves → tutor creates course → other user enrolls flow

## Endpoints

> Full schemas and request bodies are in [API.md](API.md). Quick reference below.

| Method | Path | Who |
|---|---|---|
| POST | `/api/v1/auth/register` | public |
| POST | `/api/v1/auth/login` | public |
| GET / PUT | `/api/v1/me` | any authenticated |
| POST / GET | `/api/v1/me/tutor-application` | any authenticated |
| GET | `/api/v1/admin/tutor-applications` | ADMIN |
| POST | `/api/v1/admin/tutor-applications/{id}/review` | ADMIN |
| GET | `/api/v1/tutors`, `/api/v1/tutors/{id}`, `/api/v1/tutors/{id}/courses` | public (only APPROVED tutors are listed) |
| PUT | `/api/v1/tutors/me` | TUTOR (must be APPROVED) |
| DELETE | `/api/v1/tutors/{id}` | ADMIN |
| GET | `/api/v1/courses`, `/api/v1/courses/{id}` | public |
| POST | `/api/v1/courses` | TUTOR (tutor derived from JWT) |
| GET | `/api/v1/courses/mine` | TUTOR |
| PUT / DELETE | `/api/v1/courses/{id}` | TUTOR (owner) or ADMIN |
| POST | `/api/v1/enrollments` | any authenticated |
| GET | `/api/v1/enrollments/mine` | any authenticated |
| GET (list) | `/api/v1/enrollments` | ADMIN |
| GET | `/api/v1/enrollments/{id}` | owner / course's TUTOR / ADMIN |
| PATCH / DELETE | `/api/v1/enrollments/{id}` | owner or ADMIN |

## Patterns demonstrated, by file

| Pattern | Where to look |
|---|---|
| DTO/Entity separation | [dto/request/](src/main/java/dev/tushar/tutorapi/dto/request/) and [dto/response/](src/main/java/dev/tushar/tutorapi/dto/response/) vs. [entity/](src/main/java/dev/tushar/tutorapi/entity/) |
| Bean Validation | `*Request.java` under [dto/request/](src/main/java/dev/tushar/tutorapi/dto/request/) |
| Global exception handling | [exception/GlobalExceptionHandler](src/main/java/dev/tushar/tutorapi/exception/GlobalExceptionHandler.java) |
| Mapper (MapStruct) | [mapper/TutorProfileMapper](src/main/java/dev/tushar/tutorapi/mapper/TutorProfileMapper.java) |
| Service interface + impl | [service/TutorService](src/main/java/dev/tushar/tutorapi/service/TutorService.java) + [TutorServiceImpl](src/main/java/dev/tushar/tutorapi/service/TutorServiceImpl.java) |
| Derived + `@Query` repos | [repository/TutorProfileRepository](src/main/java/dev/tushar/tutorapi/repository/TutorProfileRepository.java) |
| `JpaSpecificationExecutor` filters | [specification/CourseSpecifications](src/main/java/dev/tushar/tutorapi/specification/CourseSpecifications.java) + [CourseServiceImpl](src/main/java/dev/tushar/tutorapi/service/CourseServiceImpl.java) |
| Pagination | every controller `list()` method under [controller/](src/main/java/dev/tushar/tutorapi/controller/) |
| State machine (PENDING→APPROVED/REJECTED) | [service/TutorApplicationService](src/main/java/dev/tushar/tutorapi/service/TutorApplicationService.java) |
| Derived authorities (no role column) | [security/CustomUserDetailsService](src/main/java/dev/tushar/tutorapi/security/CustomUserDetailsService.java) |
| JWT auth | [security/JwtService](src/main/java/dev/tushar/tutorapi/security/JwtService.java) + [JwtAuthenticationFilter](src/main/java/dev/tushar/tutorapi/security/JwtAuthenticationFilter.java) |
| Method security | `@PreAuthorize` on [CourseController](src/main/java/dev/tushar/tutorapi/controller/CourseController.java) |
| Ownership-after-role check | `assertCanModify()` in [CourseServiceImpl](src/main/java/dev/tushar/tutorapi/service/CourseServiceImpl.java) and [EnrollmentService](src/main/java/dev/tushar/tutorapi/service/EnrollmentService.java) |
| Auditing | [entity/BaseEntity](src/main/java/dev/tushar/tutorapi/entity/BaseEntity.java) |
| OpenAPI bearer scheme | [config/OpenApiConfig](src/main/java/dev/tushar/tutorapi/config/OpenApiConfig.java) |
