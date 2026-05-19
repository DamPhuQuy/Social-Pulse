-- ============================================================
-- V4: Massive seed data for feed ranking
-- Adds: follows, post reactions, user interactions, bookmarks
-- ============================================================

-- ============================================================
-- FOLLOWS (each user follows 8-15 others = ~500 follow edges)
-- ============================================================

INSERT INTO follows (follower_id, following_id, created_at)
SELECT follower.id, following.id,
       NOW() - make_interval(days => (follower.rank + following.rank) % 90)
FROM (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS follower
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS following
WHERE follower.id != following.id
  AND (follower.rank * 7 + following.rank * 13) % 4 < 1  -- ~25% density
ON CONFLICT DO NOTHING;

-- Named users follow some seed users
INSERT INTO follows (follower_id, following_id, created_at)
SELECT u.id, su.id, NOW() - make_interval(days => su.rank % 30)
FROM users u
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS su
WHERE u.username IN ('alice', 'bob', 'charlie')
  AND su.rank <= 15
ON CONFLICT DO NOTHING;

-- Seed users follow named users
INSERT INTO follows (follower_id, following_id, created_at)
SELECT su.id, u.id, NOW() - make_interval(days => su.rank % 45)
FROM (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS su
CROSS JOIN users u
WHERE u.username IN ('alice', 'bob', 'charlie')
  AND su.rank % 3 = 0
ON CONFLICT DO NOTHING;

-- ============================================================
-- POST REACTIONS (~3000 reactions across posts)
-- ============================================================

INSERT INTO post_reactions (post_id, user_id, reaction_type, created_at)
SELECT p.id, reactor.id,
       CASE WHEN (p.post_rank + reactor.rank) % 7 = 0 THEN 'DOWNVOTE' ELSE 'UPVOTE' END,
       p.created_at + make_interval(hours => (reactor.rank * 3 + p.post_rank) % 72)
FROM (
    SELECT p.id, p.user_id, p.created_at, row_number() OVER (ORDER BY p.id)::INT AS post_rank
    FROM posts p
    JOIN users u ON u.id = p.user_id
    WHERE u.username LIKE 'seed_user_%' AND p.privacy = 'PUBLIC' AND p.deleted_at IS NULL
) AS p
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS reactor
WHERE reactor.id != p.user_id
  AND (p.post_rank * 11 + reactor.rank * 3) % 15 < 1  -- ~7% of all possible pairs
ON CONFLICT (post_id, user_id) DO NOTHING;

-- Named users react to seed posts
INSERT INTO post_reactions (post_id, user_id, reaction_type, created_at)
SELECT p.id, u.id, 'UPVOTE', p.created_at + INTERVAL '2 hours'
FROM (
    SELECT id, user_id, created_at, row_number() OVER (ORDER BY hot_score DESC) AS rank
    FROM posts WHERE privacy = 'PUBLIC' AND deleted_at IS NULL
) AS p
CROSS JOIN users u
WHERE u.username IN ('alice', 'bob', 'charlie')
  AND p.rank <= 50
  AND u.id != p.user_id
ON CONFLICT (post_id, user_id) DO NOTHING;

-- Sync upvote/downvote counts
UPDATE posts p
SET upvote_count = COALESCE(stats.up, 0),
    downvote_count = COALESCE(stats.down, 0),
    updated_at = NOW()
FROM (
    SELECT post_id,
           COUNT(*) FILTER (WHERE reaction_type = 'UPVOTE') AS up,
           COUNT(*) FILTER (WHERE reaction_type = 'DOWNVOTE') AS down
    FROM post_reactions
    GROUP BY post_id
) AS stats
WHERE p.id = stats.post_id;

-- Recalculate hot_score after reaction sync
UPDATE posts
SET hot_score = (upvote_count * 1.8 + cmt_count * 2.6 + view_count / 18.0 + share_count * 3.1 - downvote_count * 1.5)
WHERE deleted_at IS NULL;

-- ============================================================
-- USER INTERACTIONS (for AI feed ranking affinity)
-- ~2000 interactions: UPVOTE, COMMENT types
-- ============================================================

INSERT INTO user_interactions (viewer_id, author_id, interaction_type, created_at)
SELECT pr.user_id, p.user_id, 'UPVOTE',
       pr.created_at
FROM post_reactions pr
JOIN posts p ON p.id = pr.post_id
WHERE pr.reaction_type = 'UPVOTE'
  AND pr.user_id != p.user_id
ON CONFLICT DO NOTHING;

INSERT INTO user_interactions (viewer_id, author_id, interaction_type, created_at)
SELECT c.user_id, p.user_id, 'COMMENT', c.created_at
FROM comments c
JOIN posts p ON p.id = c.post_id
WHERE c.user_id != p.user_id AND c.deleted = false
ON CONFLICT DO NOTHING;

-- ============================================================
-- COMMENT REACTIONS (~1500 reactions)
-- ============================================================

INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
SELECT c.id, reactor.id,
       CASE WHEN (c.comment_rank + reactor.rank) % 9 = 0 THEN 'DOWNVOTE' ELSE 'UPVOTE' END,
       c.created_at + make_interval(hours => (reactor.rank + c.comment_rank) % 48)
FROM (
    SELECT c.id, c.user_id, c.created_at, row_number() OVER (ORDER BY c.id)::INT AS comment_rank
    FROM comments c WHERE c.deleted = false
) AS c
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS reactor
WHERE reactor.id != c.user_id
  AND (c.comment_rank * 7 + reactor.rank * 11) % 40 < 1  -- ~2.5%
ON CONFLICT (comment_id, user_id) DO NOTHING;

-- Sync comment reaction counts
UPDATE comments c
SET upvote_count = COALESCE(stats.up, 0),
    down_vote_count = COALESCE(stats.down, 0)
FROM (
    SELECT comment_id,
           COUNT(*) FILTER (WHERE reaction_type = 'UPVOTE') AS up,
           COUNT(*) FILTER (WHERE reaction_type = 'DOWNVOTE') AS down
    FROM comment_reactions
    GROUP BY comment_id
) AS stats
WHERE c.id = stats.comment_id;

-- ============================================================
-- BOOKMARKS (~200 bookmarks)
-- ============================================================

INSERT INTO bookmarks (user_id, post_id, created_at)
SELECT u.id, p.id, p.created_at + INTERVAL '1 day'
FROM (
    SELECT id, created_at, row_number() OVER (ORDER BY hot_score DESC)::INT AS rank
    FROM posts WHERE privacy = 'PUBLIC' AND deleted_at IS NULL
) AS p
CROSS JOIN (
    SELECT id, row_number() OVER (ORDER BY id)::INT AS rank FROM users WHERE username LIKE 'seed_user_%'
) AS u
WHERE (p.rank * 3 + u.rank * 7) % 60 = 0  -- sparse
ON CONFLICT (user_id, post_id) DO NOTHING;

-- ============================================================
-- NOTIFICATIONS (from reactions and follows)
-- ============================================================

INSERT INTO notifications (recipient_id, actor_id, type, resource_type, resource_id, message, created_at)
SELECT p.user_id, pr.user_id, 'POST_REACTED', 'POST', p.id,
       (SELECT username FROM users WHERE id = pr.user_id) || ' upvoted your post',
       pr.created_at
FROM post_reactions pr
JOIN posts p ON p.id = pr.post_id
WHERE pr.reaction_type = 'UPVOTE'
  AND pr.user_id != p.user_id
  AND (pr.id % 5 = 0)  -- only 20% to avoid flooding
LIMIT 300;

INSERT INTO notifications (recipient_id, actor_id, type, resource_type, resource_id, message, created_at)
SELECT f.following_id, f.follower_id, 'FOLLOWED_YOU', 'USER', f.follower_id,
       (SELECT username FROM users WHERE id = f.follower_id) || ' started following you',
       f.created_at
FROM follows f
WHERE (f.id % 3 = 0)  -- only 33%
LIMIT 200;
