INSERT INTO permissions (name, description) VALUES
    ('report:manage', 'Manage reports and moderation queue')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'report:manage'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
