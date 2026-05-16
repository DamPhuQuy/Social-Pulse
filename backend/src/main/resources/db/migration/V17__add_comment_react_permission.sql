INSERT INTO permissions (name, description) VALUES
    ('comment:react', 'React to comments')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'comment:react'
WHERE r.name IN ('USER', 'ADMIN')
ON CONFLICT DO NOTHING;
