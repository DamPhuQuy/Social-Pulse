-- ============================================================
-- V4: MASSIVE seed data for feed ranking stress test
-- Target: 500 users, 15000 posts, 60000+ reactions,
--         20000+ comments, 5000+ follows, 30000+ interactions
-- ============================================================

DO $$
BEGIN
IF ${seedDemoData} THEN

-- ============================================================
-- 500 USERS (seed_user_001 to seed_user_500)
-- ============================================================

INSERT INTO users (username, email, password_hash, status, verification, is_locked, failed_attempts, created_at, updated_at)
SELECT
    format('seed_user_%s', lpad(gs::text, 3, '0')),
    format('seed.user.%s@socialpulse.com', lpad(gs::text, 3, '0')),
    crypt('User@123', gen_salt('bf', 4)),
    'ACTIVE', 'VERIFIED', false, 0,
    NOW() - make_interval(days => (gs % 180) + 30),
    NOW() - make_interval(days => gs % 30)
FROM generate_series(6, 10) AS gs
ON CONFLICT DO NOTHING;

-- Profiles for new users
INSERT INTO profiles (user_id, display_name, bio, dob, gender, avatar_url, avatar_public_id, updated_at)
SELECT u.id, u.username,
       format('Active member since %s. Interests: %s.',
              to_char(u.created_at, 'Mon YYYY'),
              (ARRAY['tech','gaming','music','sports','food','travel','fashion','science','art','books'])[((u.id % 10) + 1)]),
       DATE '1990-01-01' + (u.id % 10000)::INT,
       CASE WHEN u.id % 3 = 0 THEN 'MALE' WHEN u.id % 3 = 1 THEN 'FEMALE' ELSE 'OTHER' END,
       NULL, NULL, NOW()
FROM users u
WHERE u.username LIKE 'seed_user_%'
ON CONFLICT (user_id) DO NOTHING;

-- Assign USER role to all new users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u JOIN roles r ON r.name = 'USER'
WHERE u.username LIKE 'seed_user_%'
ON CONFLICT DO NOTHING;

-- ============================================================
-- 15000 POSTS (30 per user × 500 users)
-- Spread across all topics, varied timestamps over 90 days
-- ============================================================

INSERT INTO posts (
    user_id, topic_id, content, privacy, type,
    upvote_count, downvote_count, cmt_count, view_count, share_count, hot_score,
    created_at, updated_at
)
SELECT
    author.id,
    (SELECT id FROM topics ORDER BY id OFFSET ((author.rank + slot) % 20) LIMIT 1),
    format(
        '%s %s %s #%s #%s',
        (ARRAY[
            'Just discovered something amazing about',
            'Hot take:',
            'Anyone else think that',
            'Unpopular opinion about',
            'Breaking news in',
            'My experience with',
            'Tips and tricks for',
            'Deep dive into',
            'Why I love',
            'The truth about',
            'Controversial take on',
            'Beginner guide to',
            'Advanced techniques in',
            'What nobody tells you about',
            'I cannot believe'
        ])[(slot % 15) + 1],
        (ARRAY[
            'machine learning and AI models',
            'the latest gaming releases this month',
            'modern web development frameworks',
            'cryptocurrency and blockchain tech',
            'remote work culture in 2024',
            'sustainable living practices',
            'mental health awareness',
            'space exploration missions',
            'electric vehicles and the future',
            'social media algorithms',
            'indie music scene growth',
            'competitive esports tournaments',
            'plant-based nutrition science',
            'minimalist lifestyle choices',
            'open source contributions',
            'startup funding strategies',
            'photography composition rules',
            'language learning methods',
            'fitness and workout routines',
            'travel hacking secrets'
        ])[((author.rank + slot * 3) % 20) + 1],
        (ARRAY[
            'This changed my perspective completely.',
            'What do you all think?',
            'Share your thoughts below!',
            'I spent weeks researching this.',
            'The results surprised me.',
            'Let me know if you agree.',
            'This is just the beginning.',
            'More updates coming soon.',
            'Been working on this for months.',
            'Finally ready to share this.'
        ])[((slot * 7 + author.rank) % 10) + 1],
        (ARRAY['tech','gaming','music','sports','food','travel','fashion','science','art','books','coding','design','health','finance','education'])[((author.rank + slot) % 15) + 1],
        (ARRAY['trending','viral','discussion','opinion','news','tips','guide','review','story','update'])[((slot + author.rank * 2) % 10) + 1]
    ),
    CASE WHEN (author.rank + slot) % 12 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END,
    'ORIGINAL',
    ((author.rank * 11 + slot * 7) % 500)::BIGINT,
    ((author.rank + slot) % 15)::BIGINT,
    0,
    (200 + ((author.rank * 37 + slot * 19) % 2000))::BIGINT,
    ((author.rank + slot) % 30)::BIGINT,
    0.0,
    NOW() - make_interval(days => (slot + author.rank) % 90, hours => (slot * 3 + author.rank) % 24, mins => (slot * 11) % 60),
    NOW() - make_interval(days => (slot + author.rank) % 90, hours => (slot * 3 + author.rank) % 24)
FROM (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank
    FROM users WHERE username LIKE 'seed_user_%'
) AS author
CROSS JOIN generate_series(1, 2) AS slot
WHERE NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id = author.id
    AND p.content LIKE format('%%#%s%%', (ARRAY['tech','gaming','music','sports','food','travel','fashion','science','art','books','coding','design','health','finance','education'])[((author.rank + slot) % 15) + 1])
    AND p.created_at = NOW() - make_interval(days => (slot + author.rank) % 90, hours => (slot * 3 + author.rank) % 24, mins => (slot * 11) % 60)
)
ON CONFLICT DO NOTHING;

-- ============================================================
-- FOLLOWS (~5000 edges, ~25% density among active subset)
-- ============================================================

INSERT INTO follows (follower_id, following_id, created_at)
SELECT f.id, t.id,
       NOW() - make_interval(days => (f.rank + t.rank) % 120)
FROM (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS f
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS t
WHERE f.id != t.id
  AND ((f.rank * 7 + t.rank * 13) % 50) < 1
ON CONFLICT DO NOTHING;

-- High-engagement users get more followers (top 50 users followed by many)
INSERT INTO follows (follower_id, following_id, created_at)
SELECT f.id, t.id,
       NOW() - make_interval(days => f.rank % 60)
FROM (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS f
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%' LIMIT 50
) AS t
WHERE f.id != t.id
  AND f.rank % 5 = 0
ON CONFLICT DO NOTHING;

-- ============================================================
-- POST REACTIONS (~60000 reactions)
-- ============================================================

INSERT INTO post_reactions (post_id, user_id, reaction_type, created_at)
SELECT p.id, reactor.id,
       CASE WHEN (p.post_rank + reactor.rank) % 8 = 0 THEN 'DOWNVOTE' ELSE 'UPVOTE' END,
       p.created_at + make_interval(hours => (reactor.rank * 2 + p.post_rank) % 168)
FROM (
    SELECT p.id, p.user_id, p.created_at, row_number() OVER (ORDER BY p.id)::INT AS post_rank
    FROM posts p
    WHERE p.privacy = 'PUBLIC' AND p.deleted_at IS NULL
) AS p
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank
    FROM users WHERE username LIKE 'seed_user_%'
) AS reactor
WHERE reactor.id != p.user_id
  AND (p.post_rank * 11 + reactor.rank * 3) % 120 < 1
ON CONFLICT (post_id, user_id) DO NOTHING;

-- ============================================================
-- COMMENTS (~20000 top-level + ~10000 replies)
-- ============================================================

INSERT INTO comments (post_id, user_id, parent_id, content, created_at, upvote_count, down_vote_count, deleted)
SELECT p.id, commenter.id, NULL,
       format('%s %s',
           (ARRAY[
               'Great post!', 'Totally agree with this.', 'Interesting perspective.',
               'I have a different take on this.', 'Thanks for sharing!',
               'This is exactly what I needed.', 'Can you elaborate more?',
               'Bookmarking this for later.', 'Shared with my team.',
               'This deserves more attention.', 'Well said!', 'Mind blown.',
               'I have been thinking the same thing.', 'Solid analysis.',
               'This is underrated content.', 'Following for more updates.',
               'The data backs this up.', 'Controversial but true.',
               'Adding to my reading list.', 'Quality content right here.'
           ])[((p.post_rank + commenter.rank) % 20) + 1],
           (ARRAY[
               'Would love to discuss further.',
               'What sources did you use?',
               'My experience was similar.',
               'Have you considered the opposite view?',
               'This aligns with recent research.',
               'I wrote about something similar last week.',
               'The community needs more of this.',
               'Saving this thread.',
               'Curious about the methodology.',
               'Keep up the great work!'
           ])[((commenter.rank * 3 + p.post_rank) % 10) + 1]
       ),
       p.created_at + make_interval(hours => (commenter.rank + p.post_rank) % 72, mins => (commenter.rank * 7) % 60),
       ((p.post_rank + commenter.rank) % 30)::BIGINT,
       ((p.post_rank + commenter.rank) % 5)::BIGINT,
       false
FROM (
    SELECT p.id, p.user_id, p.created_at, row_number() OVER (ORDER BY p.id)::INT AS post_rank
    FROM posts p WHERE p.privacy = 'PUBLIC' AND p.deleted_at IS NULL
) AS p
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank
    FROM users WHERE username LIKE 'seed_user_%'
) AS commenter
WHERE commenter.id != p.user_id
  AND (p.post_rank * 7 + commenter.rank * 11) % 350 < 1
ON CONFLICT DO NOTHING;

-- Replies to top-level comments
INSERT INTO comments (post_id, user_id, parent_id, content, created_at, upvote_count, down_vote_count, deleted)
SELECT c.post_id, replier.id, c.id,
       format('%s %s',
           (ARRAY[
               'Good point!', 'I disagree because', 'Exactly!',
               'To add to this,', 'Not sure about that.',
               'This is the way.', 'Fair enough.',
               'Let me push back on this.', 'Spot on.',
               'Interesting, but consider'
           ])[((c.crank + replier.rank) % 10) + 1],
           (ARRAY[
               'the evidence suggests otherwise.',
               'my experience confirms this.',
               'we need more data to be sure.',
               'this is a common misconception.',
               'the nuance matters here.'
           ])[((replier.rank + c.crank * 2) % 5) + 1]
       ),
       c.created_at + make_interval(hours => (replier.rank % 24) + 1),
       ((c.crank + replier.rank) % 15)::BIGINT,
       ((c.crank + replier.rank) % 3)::BIGINT,
       false
FROM (
    SELECT c.id, c.post_id, c.user_id, c.created_at, row_number() OVER (ORDER BY c.id)::INT AS crank
    FROM comments c WHERE c.parent_id IS NULL AND c.deleted = false
    LIMIT 5000
) AS c
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank
    FROM users WHERE username LIKE 'seed_user_%'
) AS replier
WHERE replier.id != c.user_id
  AND (c.crank * 13 + replier.rank * 7) % 250 < 1
ON CONFLICT DO NOTHING;

-- ============================================================
-- COMMENT REACTIONS (~15000)
-- ============================================================

INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
SELECT c.id, reactor.id,
       CASE WHEN (c.crank + reactor.rank) % 9 = 0 THEN 'DOWNVOTE' ELSE 'UPVOTE' END,
       c.created_at + make_interval(hours => (reactor.rank + c.crank) % 48)
FROM (
    SELECT id, user_id, created_at, row_number() OVER (ORDER BY id)::INT AS crank
    FROM comments WHERE deleted = false
    LIMIT 10000
) AS c
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank
    FROM users WHERE username LIKE 'seed_user_%'
    LIMIT 200
) AS reactor
WHERE reactor.id != c.user_id
  AND (c.crank * 7 + reactor.rank * 11) % 130 < 1
ON CONFLICT (comment_id, user_id) DO NOTHING;

-- ============================================================
-- USER INTERACTIONS (for AI affinity scoring, ~30000+)
-- ============================================================

INSERT INTO user_interactions (viewer_id, author_id, interaction_type, created_at)
SELECT pr.user_id, p.user_id, 'UPVOTE', pr.created_at
FROM post_reactions pr
JOIN posts p ON p.id = pr.post_id
WHERE pr.reaction_type = 'UPVOTE' AND pr.user_id != p.user_id
ON CONFLICT DO NOTHING;

INSERT INTO user_interactions (viewer_id, author_id, interaction_type, created_at)
SELECT c.user_id, p.user_id, 'COMMENT', c.created_at
FROM comments c
JOIN posts p ON p.id = c.post_id
WHERE c.user_id != p.user_id AND c.deleted = false
ON CONFLICT DO NOTHING;

-- ============================================================
-- BOOKMARKS (~2000)
-- ============================================================

INSERT INTO bookmarks (user_id, post_id, created_at)
SELECT u.id, p.id, p.created_at + INTERVAL '6 hours'
FROM (
    SELECT id, created_at, row_number() OVER (ORDER BY hot_score DESC)::INT AS rank
    FROM posts WHERE privacy = 'PUBLIC' AND deleted_at IS NULL
    LIMIT 500
) AS p
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank
    FROM users WHERE username LIKE 'seed_user_%'
) AS u
WHERE (p.rank * 3 + u.rank * 7) % 120 = 0
ON CONFLICT (user_id, post_id) DO NOTHING;

-- ============================================================
-- NOTIFICATIONS (~3000)
-- ============================================================

INSERT INTO notifications (recipient_id, actor_id, type, resource_type, resource_id, message, created_at)
SELECT p.user_id, pr.user_id, 'POST_REACTED', 'POST', p.id,
       (SELECT username FROM users WHERE id = pr.user_id) || ' upvoted your post',
       pr.created_at
FROM post_reactions pr
JOIN posts p ON p.id = pr.post_id
WHERE pr.reaction_type = 'UPVOTE' AND pr.user_id != p.user_id
  AND pr.id % 20 = 0
LIMIT 1500;

INSERT INTO notifications (recipient_id, actor_id, type, resource_type, resource_id, message, created_at)
SELECT p.user_id, c.user_id, 'COMMENTED_ON_POST', 'POST', p.id,
       (SELECT username FROM users WHERE id = c.user_id) || ' commented on your post',
       c.created_at
FROM comments c
JOIN posts p ON p.id = c.post_id
WHERE c.user_id != p.user_id AND c.parent_id IS NULL AND c.deleted = false
  AND c.id % 10 = 0
LIMIT 1000;

INSERT INTO notifications (recipient_id, actor_id, type, resource_type, resource_id, message, created_at)
SELECT f.following_id, f.follower_id, 'FOLLOWED_YOU', 'USER', f.follower_id,
       (SELECT username FROM users WHERE id = f.follower_id) || ' started following you',
       f.created_at
FROM follows f
WHERE f.id % 5 = 0
LIMIT 500;

-- ============================================================
-- SYNC COUNTERS
-- ============================================================

-- Sync post upvote/downvote counts
UPDATE posts p
SET upvote_count = COALESCE(stats.up, 0),
    downvote_count = COALESCE(stats.down, 0)
FROM (
    SELECT post_id,
           COUNT(*) FILTER (WHERE reaction_type = 'UPVOTE') AS up,
           COUNT(*) FILTER (WHERE reaction_type = 'DOWNVOTE') AS down
    FROM post_reactions GROUP BY post_id
) AS stats
WHERE p.id = stats.post_id;

-- Sync comment counts
UPDATE posts p
SET cmt_count = COALESCE(stats.cnt, 0)
FROM (
    SELECT post_id, COUNT(*)::BIGINT AS cnt
    FROM comments WHERE deleted = false
    GROUP BY post_id
) AS stats
WHERE p.id = stats.post_id;

-- Sync comment reaction counts
UPDATE comments c
SET upvote_count = COALESCE(stats.up, 0),
    down_vote_count = COALESCE(stats.down, 0)
FROM (
    SELECT comment_id,
           COUNT(*) FILTER (WHERE reaction_type = 'UPVOTE') AS up,
           COUNT(*) FILTER (WHERE reaction_type = 'DOWNVOTE') AS down
    FROM comment_reactions GROUP BY comment_id
) AS stats
WHERE c.id = stats.comment_id;

-- Recalculate hot_score
UPDATE posts
SET hot_score = (upvote_count * 1.8 + cmt_count * 2.6 + view_count / 18.0 + share_count * 3.1 - downvote_count * 1.5),
    updated_at = NOW()
WHERE deleted_at IS NULL;

-- ============================================================
-- SEED POST_TOPICS
-- ============================================================
INSERT INTO post_topics (post_id, topic_order, topic_slug)
SELECT
    p.id,
    0,
    CASE
        WHEN LOWER(COALESCE(p.content, '')) LIKE '%backend%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%api%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%react%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%frontend%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%tech%'
          THEN 'cong-nghe'
        WHEN LOWER(COALESCE(p.content, '')) LIKE '%onboarding%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%flow%'
          THEN 'giao-duc'
        WHEN LOWER(COALESCE(p.content, '')) LIKE '%topic bucket%'
          THEN 'tam-su'
        ELSE 'hoi-dap'
    END
FROM posts p
WHERE NOT EXISTS (
    SELECT 1
    FROM post_topics pt
    WHERE pt.post_id = p.id
);

END IF;
END $$;
