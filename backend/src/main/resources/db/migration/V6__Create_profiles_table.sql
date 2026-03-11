-- Create profiles table
CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    avatar_url VARCHAR,
    dob DATE NOT NULL,
    gender VARCHAR,
    bio TEXT,
    city_id BIGINT,
    updated_at TIMESTAMP,

    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_profiles_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_city_id ON profiles(city_id);
