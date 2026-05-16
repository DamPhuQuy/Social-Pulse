DELETE FROM permissions
WHERE name IN (
    'post:delete:any',
    'comment:delete:any'
);
