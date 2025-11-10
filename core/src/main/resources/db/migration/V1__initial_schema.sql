-- V1__initial_schema.sql
-- Initial database schema for AI Code Reviewer

CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY,
    repository_id VARCHAR(255) NOT NULL,
    change_request_id VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    llm_provider VARCHAR(50),
    llm_model VARCHAR(100),
    summary TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT,
    CONSTRAINT uk_review_unique UNIQUE (repository_id, change_request_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_reviews_status ON reviews(status);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_provider ON reviews(provider);

CREATE TABLE IF NOT EXISTS review_issues (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    start_line INTEGER NOT NULL,
    severity VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    suggestion TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_issues_review_id ON review_issues(review_id);
CREATE INDEX IF NOT EXISTS idx_issues_severity ON review_issues(severity);
CREATE INDEX IF NOT EXISTS idx_issues_file_path ON review_issues(file_path);

CREATE TABLE IF NOT EXISTS review_notes (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    line_number INTEGER NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notes_review_id ON review_notes(review_id);
CREATE INDEX IF NOT EXISTS idx_notes_file_path ON review_notes(file_path);
