ALTER TABLE test_runs
    ADD COLUMN error_tests INT NOT NULL DEFAULT 0 AFTER failed_tests;
