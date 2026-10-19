ALTER TABLE test_runs
    ADD COLUMN duration_ms BIGINT NOT NULL DEFAULT 0 AFTER completed_at;
