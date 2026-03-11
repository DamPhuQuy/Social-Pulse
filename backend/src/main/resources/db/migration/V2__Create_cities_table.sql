-- Create cities table
CREATE TABLE cities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    country VARCHAR NOT NULL
);

-- Create indexes
CREATE INDEX idx_cities_name ON cities(name);
CREATE INDEX idx_cities_country ON cities(country);
