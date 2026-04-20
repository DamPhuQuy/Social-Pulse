-- ============================================================
-- V6: Tạo bảng reports
-- Tương ứng với entity Report.java
-- ============================================================

CREATE TABLE reports (
    id          BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT      NOT NULL,
    target_type VARCHAR(50) NOT NULL, -- POST | COMMENT | USER
    target_id   BIGINT      NOT NULL,
    reason      TEXT        NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING | RESOLVED | REJECTED
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_report_reporter
        FOREIGN KEY (reporter_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_report_reporter   ON reports(reporter_id);
CREATE INDEX idx_report_target     ON reports(target_type, target_id);
CREATE INDEX idx_report_status     ON reports(status);
CREATE INDEX idx_report_created    ON reports(created_at);