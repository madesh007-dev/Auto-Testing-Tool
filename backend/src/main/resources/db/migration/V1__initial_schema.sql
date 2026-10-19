-- V1__initial_schema.sql
-- AutoTest AI Baseline Schema

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS testing_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    base_url VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_projects_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    test_type VARCHAR(20) NOT NULL,
    configuration JSON NOT NULL,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_cases_project FOREIGN KEY (project_id) REFERENCES testing_projects (id) ON DELETE CASCADE,
    INDEX idx_test_cases_project (project_id),
    INDEX idx_test_cases_type (test_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS test_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    total_tests INT NOT NULL DEFAULT 0,
    passed_tests INT NOT NULL DEFAULT 0,
    failed_tests INT NOT NULL DEFAULT 0,
    skipped_tests INT NOT NULL DEFAULT 0,
    triggered_by_id BIGINT NOT NULL,
    CONSTRAINT fk_test_runs_project FOREIGN KEY (project_id) REFERENCES testing_projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_runs_triggered_by FOREIGN KEY (triggered_by_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_test_runs_project (project_id),
    INDEX idx_test_runs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id BIGINT NOT NULL,
    test_case_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    error_message TEXT,
    execution_log MEDIUMTEXT,
    screenshot_path VARCHAR(500),
    CONSTRAINT fk_test_results_run FOREIGN KEY (test_run_id) REFERENCES test_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_results_case FOREIGN KEY (test_case_id) REFERENCES test_cases (id) ON DELETE CASCADE,
    INDEX idx_test_results_run (test_run_id),
    INDEX idx_test_results_case (test_case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
