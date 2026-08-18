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

-- Alice → Linus — ACCEPTED (makes Alice eligible to review Linus below)
INSERT INTO tutoring_requests (student_user_id, tutor_profile_id, subject, message, status, tutor_reply, responded_at, created_at, updated_at)
SELECT u.id, tp.id, 'COMPUTER_SCIENCE', 'Want to learn data structures in C.', 'ACCEPTED', 'Sure — let''s set up a weekly slot.', NOW(6), NOW(6), NOW(6)
FROM users u
CROSS JOIN tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
WHERE u.username = 'alice' AND tu.username = 'linus'
  AND NOT EXISTS (SELECT 1 FROM tutoring_requests WHERE student_user_id = u.id AND tutor_profile_id = tp.id);

-- Grace → Ada — ACCEPTED (makes Grace eligible to review Ada below)
INSERT INTO tutoring_requests (student_user_id, tutor_profile_id, subject, message, status, tutor_reply, responded_at, created_at, updated_at)
SELECT u.id, tp.id, 'MATH', 'Need help with linear algebra proofs.', 'ACCEPTED', 'Glad to help — weekday evenings work for me.', NOW(6), NOW(6), NOW(6)
FROM users u
CROSS JOIN tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
WHERE u.username = 'grace' AND tu.username = 'ada'
  AND NOT EXISTS (SELECT 1 FROM tutoring_requests WHERE student_user_id = u.id AND tutor_profile_id = tp.id);

-- =====================================================================
-- REVIEWS — a student may review a tutor after an ACCEPTED request.
-- Linus ends up at 4.5 (2 reviews), Ada at 5.0 (1 review).
-- =====================================================================

-- Grace → Linus (from the seeded ACCEPTED CS request)
INSERT INTO reviews (tutor_profile_id, student_user_id, rating, comment, created_at, updated_at)
SELECT tp.id, su.id, 5, 'Explained pointers and memory management better than any textbook. Highly recommend.', NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
CROSS JOIN users su
WHERE tu.username = 'linus' AND su.username = 'grace'
  AND NOT EXISTS (SELECT 1 FROM reviews WHERE tutor_profile_id = tp.id AND student_user_id = su.id);

-- Alice → Linus
INSERT INTO reviews (tutor_profile_id, student_user_id, rating, comment, created_at, updated_at)
SELECT tp.id, su.id, 4, 'Great with data structures. Sessions ran a little long but very thorough.', NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
CROSS JOIN users su
WHERE tu.username = 'linus' AND su.username = 'alice'
  AND NOT EXISTS (SELECT 1 FROM reviews WHERE tutor_profile_id = tp.id AND student_user_id = su.id);

-- Grace → Ada
INSERT INTO reviews (tutor_profile_id, student_user_id, rating, comment, created_at, updated_at)
SELECT tp.id, su.id, 5, 'Made linear algebra finally click. Patient and very clear.', NOW(6), NOW(6)
FROM tutor_profiles tp
JOIN users tu ON tu.id = tp.user_id
CROSS JOIN users su
WHERE tu.username = 'ada' AND su.username = 'grace'
  AND NOT EXISTS (SELECT 1 FROM reviews WHERE tutor_profile_id = tp.id AND student_user_id = su.id);
