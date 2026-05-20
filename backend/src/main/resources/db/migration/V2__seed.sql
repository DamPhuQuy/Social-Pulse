-- ============================================================
-- ROLES
-- Permissions and role-permission mappings are managed by
-- PermissionSyncService at application startup (code-first).
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('GUEST', 'Guest user with read-only access'),
    ('USER',  'Regular authenticated user'),
    ('ADMIN', 'Administrator with full access')
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

-- ============================================================
-- TOPICS
-- ============================================================

INSERT INTO topics (name, slug) VALUES
    ('Công nghệ',  'cong-nghe'),
    ('Gaming',     'gaming'),
    ('Phim ảnh',   'phim-anh'),
    ('Âm nhạc',    'am-nhac'),
    ('Thể thao',   'the-thao'),
    ('Ẩm thực',    'am-thuc'),
    ('Du lịch',    'du-lich'),
    ('Thời trang', 'thoi-trang'),
    ('Sức khỏe',   'suc-khoe'),
    ('Giáo dục',   'giao-duc'),
    ('Tài chính',  'tai-chinh'),
    ('Nghệ thuật', 'nghe-thuat'),
    ('Thú cưng',   'thu-cung'),
    ('Hài hước',   'hai-huoc'),
    ('Tâm sự',     'tam-su'),
    ('Hỏi đáp',    'hoi-dap'),
    ('Khoa học',   'khoa-hoc'),
    ('Anime',      'anime'),
    ('Sách',       'sach'),
    ('Nhiếp ảnh',  'nhiep-anh')
ON CONFLICT DO NOTHING;

DO $$
BEGIN
IF ${seedDemoData} THEN

-- ============================================================
-- USERS
-- ============================================================

INSERT INTO users (username, email, password_hash, status, verification, is_locked, failed_attempts, created_at, updated_at)
SELECT 'admin', 'admin@socialpulse.com', crypt('Admin@123', gen_salt('bf', 12)),
       'ACTIVE', 'VERIFIED', false, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@socialpulse.com');

INSERT INTO users (username, email, password_hash, status, verification, is_locked, failed_attempts, created_at, updated_at)
SELECT s.username, s.email, crypt(s.raw_password, gen_salt('bf', 12)),
       'ACTIVE', 'VERIFIED', false, 0, NOW(), NOW()
FROM (VALUES
    ('alice',   'alice@socialpulse.com',   'User@123'),
    ('bob',     'bob@socialpulse.com',     'User@123'),
    ('charlie', 'charlie@socialpulse.com', 'User@123')
) AS s(username, email, raw_password)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = s.email);

INSERT INTO users (username, email, password_hash, status, verification, is_locked, failed_attempts, created_at, updated_at)
SELECT
    format('seed_user_%s', lpad(gs::text, 3, '0')),
    format('seed.user.%s@socialpulse.com', lpad(gs::text, 3, '0')),
    crypt('User@123', gen_salt('bf', 12)),
    'ACTIVE', 'VERIFIED', false, 0,
    NOW() - make_interval(days => gs % 45),
    NOW() - make_interval(days => gs % 45)
FROM generate_series(1, 40) AS gs
ON CONFLICT DO NOTHING;

-- ============================================================
-- PROFILES
-- ============================================================

INSERT INTO profiles (user_id, display_name, bio, dob, gender, avatar_url, avatar_public_id, updated_at)
SELECT u.id, u.username, p.bio, p.dob, p.gender, NULL, NULL, NOW()
FROM users u
JOIN (VALUES
    ('alice',   'Tech enthusiast and community builder',  DATE '1999-04-12', 'FEMALE'),
    ('bob',     'Backend developer who loves clean APIs', DATE '1997-08-21', 'MALE'),
    ('charlie', 'Frontend engineer and UX explorer',      DATE '2000-01-03', 'MALE')
) AS p(username, bio, dob, gender) ON p.username = u.username
WHERE NOT EXISTS (SELECT 1 FROM profiles pr WHERE pr.user_id = u.id);

INSERT INTO profiles (user_id, display_name, bio, dob, gender, avatar_url, avatar_public_id, updated_at)
SELECT
    u.id,
    u.username,
    format('Synthetic profile for %s. Designed to stress-test discovery feeds, profile lookups, and moderation dashboards.', u.username),
    DATE '1990-01-01' + seed.ordinal,
    CASE WHEN seed.ordinal % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    NULL, NULL, NOW()
FROM users u
JOIN (
    SELECT format('seed_user_%s', lpad(gs::text, 3, '0')) AS username, gs AS ordinal
    FROM generate_series(1, 40) AS gs
) AS seed ON seed.username = u.username
ON CONFLICT (user_id) DO NOTHING;

-- Ensure admin has a profile
INSERT INTO profiles (user_id, display_name, updated_at)
SELECT u.id, u.username, CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM profiles p WHERE p.user_id = u.id);

-- ============================================================
-- USER ROLES
-- ============================================================

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'admin' OR u.email = 'admin@socialpulse.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'USER'
WHERE u.username IN ('alice', 'bob', 'charlie') OR u.username LIKE 'seed_user_%'
ON CONFLICT DO NOTHING;

-- ============================================================
-- POSTS
-- ============================================================

INSERT INTO posts (user_id, topic_id, content, image_url, image_public_id, privacy, created_at, updated_at)
SELECT u.id,
       (SELECT id FROM topics WHERE slug = 'hoi-dap'),
       sp.content, NULL, NULL, sp.privacy, NOW(), NOW()
FROM users u
JOIN (VALUES
    ('alice',   'Excited to join Social Pulse. Building in public starts today.',              'PUBLIC'),
    ('bob',     'Just shipped a small backend optimization. Response time looks better now.',  'PUBLIC'),
    ('charlie', 'Working on a new onboarding flow. Feedback is welcome.',                      'PUBLIC'),
    ('alice',   'Keeping this update private while testing a draft feature.',                  'PRIVATE')
) AS sp(username, content, privacy) ON sp.username = u.username
WHERE NOT EXISTS (SELECT 1 FROM posts p WHERE p.user_id = u.id AND p.content = sp.content);

INSERT INTO posts (
    user_id, topic_id, content, image_url, image_public_id, parent_post_id, type, privacy,
    upvote_count, downvote_count, cmt_count, view_count, share_count, hot_score,
    toxic, toxic_score, created_at, updated_at
)
SELECT
    author.id,
    (SELECT id FROM topics WHERE slug = 'hoi-dap'),
    blueprint.content, NULL, NULL, NULL, 'ORIGINAL', blueprint.privacy,
    blueprint.upvote_count, blueprint.downvote_count, 0,
    blueprint.view_count, blueprint.share_count, blueprint.hot_score,
    false, 0.0, blueprint.created_at, blueprint.created_at + INTERVAL '15 minutes'
FROM (
    SELECT u.id, u.username, row_number() OVER (ORDER BY u.username)::INT AS author_rank
    FROM users u WHERE u.username LIKE 'seed_user_%'
) AS author
JOIN LATERAL (
    SELECT
        slot,
        format('Seed post #%s from %s about topic bucket %s. This record exists to test feed ranking, cursor pagination, profile timelines, and moderation filters.',
               slot, author.username, ((author.author_rank + slot) % 12) + 1) AS content,
        CASE WHEN slot % 9 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END AS privacy,
        ((author.author_rank * 11 + slot * 7) % 260)::BIGINT AS upvote_count,
        ((author.author_rank + slot) % 9)::BIGINT AS downvote_count,
        (140 + ((author.author_rank * 37 + slot * 19) % 460))::BIGINT AS view_count,
        ((author.author_rank + slot) % 14)::BIGINT AS share_count,
        (((author.author_rank * 11 + slot * 7) % 260) * 1.8
            + ((140 + ((author.author_rank * 37 + slot * 19) % 460)) / 18.0)
            - (((author.author_rank + slot) % 9) * 1.5)) AS hot_score,
        NOW()
            - make_interval(days => (slot + author.author_rank) % 60)
            - make_interval(hours => (slot * 3 + author.author_rank) % 24)
            - make_interval(mins => (slot * 11 + author.author_rank) % 60) AS created_at
    FROM generate_series(1, 30) AS slot
) AS blueprint ON TRUE
WHERE NOT EXISTS (SELECT 1 FROM posts p WHERE p.user_id = author.id AND p.content = blueprint.content);

-- ============================================================
-- COMMENTS
-- ============================================================

INSERT INTO comments (post_id, user_id, parent_id, content, created_at, upvote_count, down_vote_count, deleted)
SELECT p.id, u.id, NULL, sc.content, NOW(), 0, 0, false
FROM posts p
JOIN (VALUES
    ('Excited to join Social Pulse. Building in public starts today.',              'bob',     'Welcome Alice, great to see your progress updates.'),
    ('Just shipped a small backend optimization. Response time looks better now.',  'charlie', 'Nice improvement. Did you profile DB queries too?'),
    ('Working on a new onboarding flow. Feedback is welcome.',                      'alice',   'I can help test this flow once it is deployed.')
) AS sc(post_content, username, content) ON sc.post_content = p.content
JOIN users u ON u.username = sc.username
WHERE NOT EXISTS (SELECT 1 FROM comments c WHERE c.post_id = p.id AND c.user_id = u.id AND c.content = sc.content);

INSERT INTO comments (post_id, user_id, parent_id, content, created_at, upvote_count, down_vote_count, deleted)
SELECT p.id, u.id, parent_comment.id, sr.reply_content, NOW(), 0, 0, false
FROM posts p
JOIN (VALUES
    ('Just shipped a small backend optimization. Response time looks better now.',
     'charlie', 'Nice improvement. Did you profile DB queries too?',
     'bob', 'Yes, added indexes and removed one expensive join. Big difference.'),
    ('Working on a new onboarding flow. Feedback is welcome.',
     'alice', 'I can help test this flow once it is deployed.',
     'charlie', 'Awesome, I will share a test link after tonight deployment.')
) AS sr(post_content, parent_author, parent_content, reply_author, reply_content) ON sr.post_content = p.content
JOIN users parent_user ON parent_user.username = sr.parent_author
JOIN comments parent_comment
    ON parent_comment.post_id = p.id AND parent_comment.user_id = parent_user.id
   AND parent_comment.parent_id IS NULL AND parent_comment.content = sr.parent_content
JOIN users u ON u.username = sr.reply_author
WHERE NOT EXISTS (SELECT 1 FROM comments c
    WHERE c.post_id = p.id AND c.user_id = u.id
      AND c.parent_id = parent_comment.id AND c.content = sr.reply_content);

-- Seed top-level comments on synthetic posts
INSERT INTO comments (post_id, user_id, parent_id, content, created_at, upvote_count, down_vote_count, deleted, edited)
WITH seed_authors AS (
    SELECT u.id, u.username, row_number() OVER (ORDER BY u.username)::INT AS author_rank
    FROM users u WHERE u.username LIKE 'seed_user_%'
),
author_pool AS (SELECT COUNT(*) AS total_authors FROM seed_authors),
seed_posts AS (
    SELECT p.id, p.user_id, p.created_at, row_number() OVER (ORDER BY p.id)::INT AS post_rank
    FROM posts p JOIN users u ON u.id = p.user_id
    WHERE u.username LIKE 'seed_user_%' AND p.content LIKE 'Seed post #%'
)
SELECT sp.id, sa.id, NULL,
    format('Top-level seed comment %s on post %s by %s. Useful for validating comment pagination, counters, and moderation queues.',
           slot.comment_slot, sp.id, sa.username),
    sp.created_at + make_interval(hours => slot.comment_slot, mins => (sp.post_rank + slot.comment_slot * 7) % 45),
    ((sp.post_rank + slot.comment_slot * 5) % 25)::BIGINT,
    ((sp.post_rank + slot.comment_slot) % 4)::BIGINT,
    false, false
FROM seed_posts sp
CROSS JOIN generate_series(1, 4) AS slot(comment_slot)
CROSS JOIN author_pool ap
JOIN seed_authors sa ON sa.author_rank = ((sp.post_rank + slot.comment_slot - 1) % ap.total_authors) + 1
WHERE sa.id <> sp.user_id
  AND NOT EXISTS (SELECT 1 FROM comments c
      WHERE c.post_id = sp.id AND c.user_id = sa.id AND c.parent_id IS NULL
        AND c.content = format('Top-level seed comment %s on post %s by %s. Useful for validating comment pagination, counters, and moderation queues.',
                               slot.comment_slot, sp.id, sa.username));

-- Seed replies on synthetic posts
INSERT INTO comments (post_id, user_id, parent_id, content, created_at, upvote_count, down_vote_count, deleted, edited)
WITH seed_authors AS (
    SELECT u.id, u.username, row_number() OVER (ORDER BY u.username)::INT AS author_rank
    FROM users u WHERE u.username LIKE 'seed_user_%'
),
author_pool AS (SELECT COUNT(*) AS total_authors FROM seed_authors),
top_level_comments AS (
    SELECT c.id, c.post_id, c.user_id AS parent_user_id, c.created_at,
           row_number() OVER (PARTITION BY c.post_id ORDER BY c.id)::INT AS comment_rank
    FROM comments c
    JOIN posts p ON p.id = c.post_id
    JOIN users u ON u.id = p.user_id
    WHERE u.username LIKE 'seed_user_%' AND c.parent_id IS NULL AND c.content LIKE 'Top-level seed comment %'
)
SELECT tlc.post_id, sa.id, tlc.id,
    format('Reply seed comment %s on comment %s by %s. Useful for nested threads, moderation actions, and read-state testing.',
           slot.reply_slot, tlc.id, sa.username),
    tlc.created_at + make_interval(mins => slot.reply_slot * 9 + (tlc.comment_rank % 12)),
    ((tlc.comment_rank + slot.reply_slot * 3) % 14)::BIGINT,
    ((tlc.comment_rank + slot.reply_slot) % 3)::BIGINT,
    false,
    CASE WHEN slot.reply_slot = 2 AND tlc.comment_rank % 5 = 0 THEN true ELSE false END
FROM top_level_comments tlc
CROSS JOIN generate_series(1, 2) AS slot(reply_slot)
CROSS JOIN author_pool ap
JOIN seed_authors sa ON sa.author_rank = ((tlc.comment_rank + slot.reply_slot + tlc.post_id::INT) % ap.total_authors) + 1
WHERE sa.id <> tlc.parent_user_id
  AND NOT EXISTS (SELECT 1 FROM comments c
      WHERE c.post_id = tlc.post_id AND c.user_id = sa.id AND c.parent_id = tlc.id
        AND c.content = format('Reply seed comment %s on comment %s by %s. Useful for nested threads, moderation actions, and read-state testing.',
                               slot.reply_slot, tlc.id, sa.username));

-- Sync post counters
UPDATE posts p
SET cmt_count = stats.comment_count,
    hot_score = (p.upvote_count * 1.8 + stats.comment_count * 2.6 + p.view_count / 18.0 + p.share_count * 3.1 - p.downvote_count * 1.5),
    updated_at = GREATEST(p.updated_at, NOW())
FROM (
    SELECT c.post_id, COUNT(*)::BIGINT AS comment_count
    FROM comments c
    JOIN posts p2 ON p2.id = c.post_id
    JOIN users u ON u.id = p2.user_id
    WHERE u.username LIKE 'seed_user_%' AND p2.content LIKE 'Seed post #%'
    GROUP BY c.post_id
) AS stats
WHERE p.id = stats.post_id;

END IF;
END $$;
