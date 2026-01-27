-- Create job_runs table to track scheduled job executions
CREATE TYPE job_status AS ENUM ('SUCCESS', 'FAILURE');

CREATE TABLE job_runs (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    start_datetime TIMESTAMP NOT NULL,
    finish_datetime TIMESTAMP NOT NULL,
    status job_status NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_runs_job_name ON job_runs(job_name);
CREATE INDEX idx_job_runs_status ON job_runs(status);
CREATE INDEX idx_job_runs_job_name_status_finish ON job_runs(job_name, status, finish_datetime DESC);
