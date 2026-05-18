-- Add missing discovery permissions for search history feature
INSERT INTO permissions (name, description) VALUES
    ('discovery:write', 'Write and save search history'),
    ('discovery:delete', 'Delete search history')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

-- Grant permissions to USER and ADMIN roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'discovery:write',
    'discovery:delete'
)
WHERE r.name IN ('USER', 'ADMIN')
ON CONFLICT DO NOTHING;
