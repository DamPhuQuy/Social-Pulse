-- Create topics table
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL
);
