CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ensure seeded and legacy users have the expected roles after the role migration.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'admin'
   OR u.email = 'admin@socialpulse.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE u.username <> 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
  )
ON CONFLICT DO NOTHING;

-- Create a wide pool of deterministic test users for feed, pagination, and moderation scenarios.
INSERT INTO users (
    username,
    email,
    password_hash,
    status,
    verification,
    is_locked,
    failed_attempts,
    created_at,
    updated_at
)
SELECT
    format('seed_user_%s', lpad(gs::text, 3, '0')),
    format('seed.user.%s@socialpulse.com', lpad(gs::text, 3, '0')),
    crypt('User@123', gen_salt('bf', 12)),
    'ACTIVE',
    'VERIFIED',
    false,
    0,
    NOW() - make_interval(days => gs % 45),
    NOW() - make_interval(days => gs % 45)
FROM generate_series(1, 40) AS gs
ON CONFLICT DO NOTHING;

INSERT INTO profiles (
    user_id,
    bio,
    dob,
    gender,
    avatar_url,
    avatar_public_id,
    updated_at
)
SELECT
    u.id,
    format(
        'Synthetic profile for %s. Designed to stress-test discovery feeds, profile lookups, and moderation dashboards.',
        u.username
    ),
    DATE '1990-01-01' + seed.ordinal,
    CASE WHEN seed.ordinal % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    NULL,
    NULL,
    NOW()
FROM users u
JOIN (
    SELECT
        format('seed_user_%s', lpad(gs::text, 3, '0')) AS username,
        gs AS ordinal
    FROM generate_series(1, 40) AS gs
) AS seed
    ON seed.username = u.username
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE u.username LIKE 'seed_user_%'
ON CONFLICT DO NOTHING;

-- Seed a large volume of deterministic posts without bloating the repository.
INSERT INTO posts (
    user_id,
    content,
    image_url,
    image_public_id,
    parent_post_id,
    type,
    privacy,
    upvote_count,
    downvote_count,
    cmt_count,
    view_count,
    share_count,
    hot_score,
    toxic,
    toxic_score,
    created_at,
    updated_at
)
SELECT
    author.id,
    blueprint.content,
    NULL,
    NULL,
    NULL,
    'ORIGINAL',
    blueprint.privacy,
    blueprint.upvote_count,
    blueprint.downvote_count,
    0,
    blueprint.view_count,
    blueprint.share_count,
    blueprint.hot_score,
    false,
    0.0,
    blueprint.created_at,
    blueprint.created_at + INTERVAL '15 minutes'
FROM (
    SELECT
        u.id,
        u.username,
        row_number() OVER (ORDER BY u.username)::INT AS author_rank
    FROM users u
    WHERE u.username LIKE 'seed_user_%'
) AS author
JOIN LATERAL (
    SELECT
        slot,
        format(
            'Seed post #%s from %s about topic bucket %s. This record exists to test feed ranking, cursor pagination, profile timelines, and moderation filters.',
            slot,
            author.username,
            ((author.author_rank + slot) % 12) + 1
        ) AS content,
        CASE WHEN slot % 9 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END AS privacy,
        ((author.author_rank * 11 + slot * 7) % 260)::BIGINT AS upvote_count,
        ((author.author_rank + slot) % 9)::BIGINT AS downvote_count,
        (140 + ((author.author_rank * 37 + slot * 19) % 460))::BIGINT AS view_count,
        ((author.author_rank + slot) % 14)::BIGINT AS share_count,
        (
            ((author.author_rank * 11 + slot * 7) % 260) * 1.8
            + ((140 + ((author.author_rank * 37 + slot * 19) % 460)) / 18.0)
            - (((author.author_rank + slot) % 9) * 1.5)
        ) AS hot_score,
        NOW()
            - make_interval(days => (slot + author.author_rank) % 60)
            - make_interval(hours => (slot * 3 + author.author_rank) % 24)
            - make_interval(mins => (slot * 11 + author.author_rank) % 60) AS created_at
    FROM generate_series(1, 30) AS slot
) AS blueprint ON TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM posts p
    WHERE p.user_id = author.id
      AND p.content = blueprint.content
);

-- Seed top-level comments across the synthetic post set.
INSERT INTO comments (
    post_id,
    user_id,
    parent_id,
    content,
    created_at,
    upvote_count,
    down_vote_count,
    deleted,
    edited
)
WITH seed_authors AS (
    SELECT
        u.id,
        u.username,
        row_number() OVER (ORDER BY u.username)::INT AS author_rank
    FROM users u
    WHERE u.username LIKE 'seed_user_%'
),
author_pool AS (
    SELECT COUNT(*) AS total_authors FROM seed_authors
),
seed_posts AS (
    SELECT
        p.id,
        p.user_id,
        p.created_at,
        row_number() OVER (ORDER BY p.id)::INT AS post_rank
    FROM posts p
    JOIN users u ON u.id = p.user_id
    WHERE u.username LIKE 'seed_user_%'
      AND p.content LIKE 'Seed post #%'
)
SELECT
    sp.id,
    sa.id,
    NULL,
    format(
        'Top-level seed comment %s on post %s by %s. Useful for validating comment pagination, counters, and moderation queues.',
        slot.comment_slot,
        sp.id,
        sa.username
    ),
    sp.created_at + make_interval(hours => slot.comment_slot, mins => (sp.post_rank + slot.comment_slot * 7) % 45),
    ((sp.post_rank + slot.comment_slot * 5) % 25)::BIGINT,
    ((sp.post_rank + slot.comment_slot) % 4)::BIGINT,
    false,
    false
FROM seed_posts sp
CROSS JOIN generate_series(1, 4) AS slot(comment_slot)
CROSS JOIN author_pool ap
JOIN seed_authors sa
    ON sa.author_rank = ((sp.post_rank + slot.comment_slot - 1) % ap.total_authors) + 1
WHERE sa.id <> sp.user_id
  AND NOT EXISTS (
      SELECT 1
      FROM comments c
      WHERE c.post_id = sp.id
        AND c.user_id = sa.id
        AND c.parent_id IS NULL
        AND c.content = format(
            'Top-level seed comment %s on post %s by %s. Useful for validating comment pagination, counters, and moderation queues.',
            slot.comment_slot,
            sp.id,
            sa.username
        )
  );

-- Seed replies so every post has nested conversation depth.
INSERT INTO comments (
    post_id,
    user_id,
    parent_id,
    content,
    created_at,
    upvote_count,
    down_vote_count,
    deleted,
    edited
)
WITH seed_authors AS (
    SELECT
        u.id,
        u.username,
        row_number() OVER (ORDER BY u.username)::INT AS author_rank
    FROM users u
    WHERE u.username LIKE 'seed_user_%'
),
author_pool AS (
    SELECT COUNT(*) AS total_authors FROM seed_authors
),
top_level_comments AS (
    SELECT
        c.id,
        c.post_id,
        c.user_id AS parent_user_id,
        c.created_at,
        row_number() OVER (PARTITION BY c.post_id ORDER BY c.id)::INT AS comment_rank
    FROM comments c
    JOIN posts p ON p.id = c.post_id
    JOIN users u ON u.id = p.user_id
    WHERE u.username LIKE 'seed_user_%'
      AND c.parent_id IS NULL
      AND c.content LIKE 'Top-level seed comment %'
)
SELECT
    tlc.post_id,
    sa.id,
    tlc.id,
    format(
        'Reply seed comment %s on comment %s by %s. Useful for nested threads, moderation actions, and read-state testing.',
        slot.reply_slot,
        tlc.id,
        sa.username
    ),
    tlc.created_at + make_interval(mins => slot.reply_slot * 9 + (tlc.comment_rank % 12)),
    ((tlc.comment_rank + slot.reply_slot * 3) % 14)::BIGINT,
    ((tlc.comment_rank + slot.reply_slot) % 3)::BIGINT,
    false,
    CASE WHEN slot.reply_slot = 2 AND tlc.comment_rank % 5 = 0 THEN true ELSE false END
FROM top_level_comments tlc
CROSS JOIN generate_series(1, 2) AS slot(reply_slot)
CROSS JOIN author_pool ap
JOIN seed_authors sa
    ON sa.author_rank = ((tlc.comment_rank + slot.reply_slot + tlc.post_id::INT) % ap.total_authors) + 1
WHERE sa.id <> tlc.parent_user_id
  AND NOT EXISTS (
      SELECT 1
      FROM comments c
      WHERE c.post_id = tlc.post_id
        AND c.user_id = sa.id
        AND c.parent_id = tlc.id
        AND c.content = format(
            'Reply seed comment %s on comment %s by %s. Useful for nested threads, moderation actions, and read-state testing.',
            slot.reply_slot,
            tlc.id,
            sa.username
        )
  );

-- Synchronize derived counters so the seeded data behaves like production records.
UPDATE posts p
SET
    cmt_count = stats.comment_count,
    hot_score = (
        p.upvote_count * 1.8
        + stats.comment_count * 2.6
        + p.view_count / 18.0
        + p.share_count * 3.1
        - p.downvote_count * 1.5
    ),
    updated_at = GREATEST(p.updated_at, NOW())
FROM (
    SELECT c.post_id, COUNT(*)::BIGINT AS comment_count
    FROM comments c
    JOIN posts p2 ON p2.id = c.post_id
    JOIN users u ON u.id = p2.user_id
    WHERE u.username LIKE 'seed_user_%'
      AND p2.content LIKE 'Seed post #%'
    GROUP BY c.post_id
) AS stats
WHERE p.id = stats.post_id;
