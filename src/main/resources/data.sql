-- Dummy seed data for local dev. Runs on every startup via
-- `spring.sql.init.mode=always` (set in application-dev.properties only).
-- Every INSERT is guarded by WHERE NOT EXISTS, so it's idempotent — safe to re-run.
--
-- All seeded accounts share the SAME password: "Secret123!"
--   BCrypt hash below was generated with cost=10. Don't reuse in prod.

-- =====================================================================
-- USERS
-- =====================================================================

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'root', 'root@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Root', 'Admin', NULL, TRUE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'root');

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'ada', 'ada@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Ada', 'Lovelace', '1990-12-10', FALSE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'ada');

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'linus', 'linus@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Linus', 'Torvalds', '1985-06-15', FALSE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'linus');

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'carol', 'carol@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Carol', 'Stein', '1995-03-22', FALSE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'carol');

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'dave', 'dave@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Dave', 'Cooper', '1998-11-02', FALSE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'dave');

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'grace', 'grace@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Grace', 'Hopper', '2000-12-09', FALSE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'grace');

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, admin, token_version, created_at, updated_at)
SELECT 'alice', 'alice@example.com', '$2b$10$QTxcc5br7icRwRIe.4IRSe4cFiX7VenTEA0ttUJZTY6yZok58M/v2', 'Alice', 'Wonder', '2001-07-04', FALSE, 0, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'alice');

-- =====================================================================
-- TUTOR PROFILES — two APPROVED, one PENDING, one REJECTED
-- =====================================================================

-- Ada — APPROVED math tutor
INSERT INTO tutor_profiles (user_id, bio, expertise, qualifications, years_of_experience, hourly_rate_cents, status, applied_at, reviewed_at, reviewed_by_user_id, rejection_reason, created_at, updated_at)
SELECT u.id, 'Mathematician and computing pioneer. 10 years tutoring experience.', 'MATH', 'MSc Mathematics, University of London; ICM speaker 2018', 10, 7500, 'APPROVED', NOW(6), NOW(6), (SELECT id FROM users WHERE username = 'root'), NULL, NOW(6), NOW(6)
FROM users u
WHERE u.username = 'ada'
  AND NOT EXISTS (SELECT 1 FROM tutor_profiles WHERE user_id = u.id);

-- Linus — APPROVED programming tutor
INSERT INTO tutor_profiles (user_id, bio, expertise, qualifications, years_of_experience, hourly_rate_cents, status, applied_at, reviewed_at, reviewed_by_user_id, rejection_reason, created_at, updated_at)
SELECT u.id, 'Software engineer with 25 years of experience. Specializes in systems programming.', 'PROGRAMMING', 'MSc Computer Science, University of Helsinki; kernel maintainer', 25, 10000, 'APPROVED', NOW(6), NOW(6), (SELECT id FROM users WHERE username = 'root'), NULL, NOW(6), NOW(6)
FROM users u
WHERE u.username = 'linus'
  AND NOT EXISTS (SELECT 1 FROM tutor_profiles WHERE user_id = u.id);

-- Carol — PENDING (admin has something to review)
INSERT INTO tutor_profiles (user_id, bio, expertise, qualifications, years_of_experience, hourly_rate_cents, status, applied_at, reviewed_at, reviewed_by_user_id, rejection_reason, created_at, updated_at)
SELECT u.id, 'Chemistry PhD candidate, eager to teach!', 'SCIENCE', 'PhD candidate in Chemistry, MIT', 2, 5000, 'PENDING', NOW(6), NULL, NULL, NULL, NOW(6), NOW(6)
FROM users u
WHERE u.username = 'carol'
  AND NOT EXISTS (SELECT 1 FROM tutor_profiles WHERE user_id = u.id);

-- Dave — REJECTED
INSERT INTO tutor_profiles (user_id, bio, expertise, qualifications, years_of_experience, hourly_rate_cents, status, applied_at, reviewed_at, reviewed_by_user_id, rejection_reason, created_at, updated_at)
SELECT u.id, 'I know some French.', 'LANGUAGES', 'High school French B2', 0, 3000, 'REJECTED', NOW(6), NOW(6), (SELECT id FROM users WHERE username = 'root'), 'Insufficient teaching experience. Please reapply with credentials.', NOW(6), NOW(6)
FROM users u
WHERE u.username = 'dave'
  AND NOT EXISTS (SELECT 1 FROM tutor_profiles WHERE user_id = u.id);

-- =====================================================================
-- COURSES — only APPROVED tutors can own them
-- =====================================================================

-- Linus' courses
INSERT INTO courses (tutor_profile_id, title, description, subject, level, price_cents, created_at, updated_at)
SELECT tp.id, 'Intro to Algorithms', 'Sorting, searching, complexity. A gentle introduction to algorithmic thinking.', 'COMPUTER_SCIENCE', 'BEGINNER', 4999, NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users u ON u.id = tp.user_id
WHERE u.username = 'linus'
  AND NOT EXISTS (SELECT 1 FROM courses WHERE title = 'Intro to Algorithms' AND tutor_profile_id = tp.id);

INSERT INTO courses (tutor_profile_id, title, description, subject, level, price_cents, created_at, updated_at)
SELECT tp.id, 'Advanced Linux Internals', 'Process scheduling, memory management, kernel hacking.', 'COMPUTER_SCIENCE', 'ADVANCED', 9999, NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users u ON u.id = tp.user_id
WHERE u.username = 'linus'
  AND NOT EXISTS (SELECT 1 FROM courses WHERE title = 'Advanced Linux Internals' AND tutor_profile_id = tp.id);

-- Ada's courses
INSERT INTO courses (tutor_profile_id, title, description, subject, level, price_cents, created_at, updated_at)
SELECT tp.id, 'Calculus 101', 'Limits, derivatives, integrals — the calculus foundations.', 'MATH', 'BEGINNER', 2999, NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users u ON u.id = tp.user_id
WHERE u.username = 'ada'
  AND NOT EXISTS (SELECT 1 FROM courses WHERE title = 'Calculus 101' AND tutor_profile_id = tp.id);

INSERT INTO courses (tutor_profile_id, title, description, subject, level, price_cents, created_at, updated_at)
SELECT tp.id, 'Linear Algebra', 'Vectors, matrices, eigenvalues. The math behind ML.', 'MATH', 'INTERMEDIATE', 3999, NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users u ON u.id = tp.user_id
WHERE u.username = 'ada'
  AND NOT EXISTS (SELECT 1 FROM courses WHERE title = 'Linear Algebra' AND tutor_profile_id = tp.id);

-- =====================================================================
-- ENROLLMENTS — anyone authenticated can enroll
-- =====================================================================

-- Grace enrolled in Intro to Algorithms (ACTIVE)
INSERT INTO enrollments (user_id, course_id, enrolled_at, status, created_at, updated_at)
SELECT u.id, c.id, NOW(6), 'ACTIVE', NOW(6), NOW(6)
FROM users u
CROSS JOIN courses c
WHERE u.username = 'grace' AND c.title = 'Intro to Algorithms'
  AND NOT EXISTS (SELECT 1 FROM enrollments WHERE user_id = u.id AND course_id = c.id);

-- Grace completed Calculus 101
INSERT INTO enrollments (user_id, course_id, enrolled_at, status, created_at, updated_at)
SELECT u.id, c.id, NOW(6), 'COMPLETED', NOW(6), NOW(6)
FROM users u
CROSS JOIN courses c
WHERE u.username = 'grace' AND c.title = 'Calculus 101'
  AND NOT EXISTS (SELECT 1 FROM enrollments WHERE user_id = u.id AND course_id = c.id);

-- Alice enrolled in Intro to Algorithms (ACTIVE)
INSERT INTO enrollments (user_id, course_id, enrolled_at, status, created_at, updated_at)
SELECT u.id, c.id, NOW(6), 'ACTIVE', NOW(6), NOW(6)
FROM users u
CROSS JOIN courses c
WHERE u.username = 'alice' AND c.title = 'Intro to Algorithms'
  AND NOT EXISTS (SELECT 1 FROM enrollments WHERE user_id = u.id AND course_id = c.id);

-- Alice cancelled Linear Algebra
INSERT INTO enrollments (user_id, course_id, enrolled_at, status, created_at, updated_at)
SELECT u.id, c.id, NOW(6), 'CANCELLED', NOW(6), NOW(6)
FROM users u
CROSS JOIN courses c
WHERE u.username = 'alice' AND c.title = 'Linear Algebra'
  AND NOT EXISTS (SELECT 1 FROM enrollments WHERE user_id = u.id AND course_id = c.id);

-- =====================================================================
-- TUTORING REQUESTS — 1:1 booking inquiries
-- =====================================================================

-- Alice → Ada — PENDING math request (Ada hasn't responded yet)
INSERT INTO tutoring_requests (student_user_id, tutor_profile_id, subject, message, status, tutor_reply, responded_at, created_at, updated_at)
SELECT u.id, tp.id, 'MATH', 'Hi! I''m struggling with multivariable calculus, weekends preferred.', 'PENDING', NULL, NULL, NOW(6), NOW(6)
FROM users u
CROSS JOIN tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
WHERE u.username = 'alice' AND tu.username = 'ada'
  AND NOT EXISTS (SELECT 1 FROM tutoring_requests WHERE student_user_id = u.id AND tutor_profile_id = tp.id);

-- Grace → Linus — ACCEPTED CS request
INSERT INTO tutoring_requests (student_user_id, tutor_profile_id, subject, message, status, tutor_reply, responded_at, created_at, updated_at)
SELECT u.id, tp.id, 'COMPUTER_SCIENCE', 'Need help with C pointers and memory management.', 'ACCEPTED', 'Happy to help — send me your availability and we''ll start next week.', NOW(6), NOW(6), NOW(6)
FROM users u
CROSS JOIN tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
WHERE u.username = 'grace' AND tu.username = 'linus'
  AND NOT EXISTS (SELECT 1 FROM tutoring_requests WHERE student_user_id = u.id AND tutor_profile_id = tp.id);

-- Dave → Ada — REJECTED math request
INSERT INTO tutoring_requests (student_user_id, tutor_profile_id, subject, message, status, tutor_reply, responded_at, created_at, updated_at)
SELECT u.id, tp.id, 'MATH', 'Looking for daily tutoring at $5/hr.', 'REJECTED', 'Sorry, that''s well below my rate. Best of luck with your search!', NOW(6), NOW(6), NOW(6)
FROM users u
CROSS JOIN tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
WHERE u.username = 'dave' AND tu.username = 'ada'
  AND NOT EXISTS (SELECT 1 FROM tutoring_requests WHERE student_user_id = u.id AND tutor_profile_id = tp.id);
