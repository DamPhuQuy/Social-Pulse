-- Create otp_codes table
CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    type VARCHAR,
    expires_at TIMESTAMP,
    is_used BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_otp_codes_email FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_otp_codes_email ON otp_codes(email);
CREATE INDEX idx_otp_codes_expires_at ON otp_codes(expires_at);
