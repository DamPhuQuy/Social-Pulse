INSERT INTO permissions (name, description) VALUES
    ('topic:manage', 'Create, update, delete topics'),
    ('admin:access', 'Access admin endpoints')
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('topic:manage', 'admin:access')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
