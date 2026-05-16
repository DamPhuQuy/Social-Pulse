CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Seed a small sample social graph for local development.
INSERT INTO users (
    username,
    email,
    password_hash,
    status,
    role,
    verification,
    is_locked,
    failed_attempts,
    created_at,
    updated_at
)
SELECT
    s.username,
    s.email,
    crypt(s.raw_password, gen_salt('bf', 12)),
    'ACTIVE',
    'USER',
    'VERIFIED',
    false,
    0,
    NOW(),
    NOW()
FROM (
    VALUES
        ('alice', 'alice@socialpulse.com', 'User@123'),
        ('bob', 'bob@socialpulse.com', 'User@123'),
        ('charlie', 'charlie@socialpulse.com', 'User@123')
) AS s(username, email, raw_password)
WHERE NOT EXISTS (
    SELECT 1
    FROM users u
    WHERE u.email = s.email
);

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
    p.bio,
    p.dob,
    p.gender,
    p.avatar_url,
    NULL,
    NOW()
FROM users u
JOIN (
    VALUES
        ('alice', 'Tech enthusiast and community builder', DATE '1999-04-12', 'FEMALE', NULL),
        ('bob', 'Backend developer who loves clean APIs', DATE '1997-08-21', 'MALE', NULL),
        ('charlie', 'Frontend engineer and UX explorer', DATE '2000-01-03', 'MALE', NULL)
) AS p(username, bio, dob, gender, avatar_url)
    ON p.username = u.username
WHERE NOT EXISTS (
    SELECT 1
    FROM profiles pr
    WHERE pr.user_id = u.id
);

INSERT INTO posts (
    user_id,
    content,
    image_url,
    image_public_id,
    privacy,
    created_at,
    updated_at
)
SELECT
    u.id,
    sp.content,
    sp.image_url,
    NULL,
    sp.privacy,
    NOW(),
    NOW()
FROM users u
JOIN (
    VALUES
        ('alice', 'Excited to join Social Pulse. Building in public starts today.', NULL, 'PUBLIC'),
        ('bob', 'Just shipped a small backend optimization. Response time looks better now.', NULL, 'PUBLIC'),
        ('charlie', 'Working on a new onboarding flow. Feedback is welcome.', NULL, 'PUBLIC'),
        ('alice', 'Keeping this update private while testing a draft feature.', NULL, 'PRIVATE')
) AS sp(username, content, image_url, privacy)
    ON sp.username = u.username
WHERE NOT EXISTS (
    SELECT 1
    FROM posts p
    WHERE p.user_id = u.id
      AND p.content = sp.content
);

INSERT INTO comments (
    post_id,
    user_id,
    parent_id,
    content,
    created_at,
    upvote_count,
    down_vote_count,
    deleted
)
SELECT
    p.id,
    u.id,
    NULL,
    sc.content,
    NOW(),
    0,
    0,
    false
FROM posts p
JOIN (
    VALUES
        ('Excited to join Social Pulse. Building in public starts today.', 'bob', 'Welcome Alice, great to see your progress updates.'),
        ('Just shipped a small backend optimization. Response time looks better now.', 'charlie', 'Nice improvement. Did you profile DB queries too?'),
        ('Working on a new onboarding flow. Feedback is welcome.', 'alice', 'I can help test this flow once it is deployed.')
) AS sc(post_content, username, content)
    ON sc.post_content = p.content
JOIN users u
    ON u.username = sc.username
WHERE NOT EXISTS (
    SELECT 1
    FROM comments c
    WHERE c.post_id = p.id
      AND c.user_id = u.id
      AND c.content = sc.content
);

INSERT INTO comments (
    post_id,
    user_id,
    parent_id,
    content,
    created_at,
    upvote_count,
    down_vote_count,
    deleted
)
SELECT
    p.id,
    u.id,
    parent_comment.id,
    sr.reply_content,
    NOW(),
    0,
    0,
    false
FROM posts p
JOIN (
    VALUES
        (
            'Just shipped a small backend optimization. Response time looks better now.',
            'charlie',
            'Nice improvement. Did you profile DB queries too?',
            'bob',
            'Yes, added indexes and removed one expensive join. Big difference.'
        ),
        (
            'Working on a new onboarding flow. Feedback is welcome.',
            'alice',
            'I can help test this flow once it is deployed.',
            'charlie',
            'Awesome, I will share a test link after tonight deployment.'
        )
) AS sr(post_content, parent_author, parent_content, reply_author, reply_content)
    ON sr.post_content = p.content
JOIN users parent_user
    ON parent_user.username = sr.parent_author
JOIN comments parent_comment
    ON parent_comment.post_id = p.id
   AND parent_comment.user_id = parent_user.id
   AND parent_comment.parent_id IS NULL
   AND parent_comment.content = sr.parent_content
JOIN users u
    ON u.username = sr.reply_author
WHERE NOT EXISTS (
    SELECT 1
    FROM comments c
    WHERE c.post_id = p.id
      AND c.user_id = u.id
      AND c.parent_id = parent_comment.id
      AND c.content = sr.reply_content
);
