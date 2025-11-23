-- V5__add_context_audit_tables.sql
-- Add context retrieval auditing and debugging tables

-- Session tracking table
CREATE TABLE context_retrieval_sessions (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    total_execution_time_ms BIGINT NOT NULL,
    strategies_executed INTEGER NOT NULL,
    strategies_succeeded INTEGER NOT NULL,
    strategies_failed INTEGER NOT NULL,
    diff_file_count INTEGER NOT NULL,
    diff_line_count INTEGER NOT NULL,
    total_matches INTEGER NOT NULL,
    high_confidence_matches INTEGER NOT NULL,
    context_enabled BOOLEAN NOT NULL,
    skipped_due_to_size BOOLEAN NOT NULL,
    prompt_text TEXT NOT NULL,
    prompt_size_bytes INTEGER NOT NULL
);

CREATE INDEX idx_context_sessions_review_id ON context_retrieval_sessions(review_id);
CREATE INDEX idx_context_sessions_created_at ON context_retrieval_sessions(created_at DESC);
CREATE INDEX idx_context_sessions_total_matches ON context_retrieval_sessions(total_matches);

COMMENT ON TABLE context_retrieval_sessions IS 'Tracks each context retrieval execution session for debugging and auditing';
COMMENT ON COLUMN context_retrieval_sessions.prompt_text IS 'Complete prompt text sent to LLM including context section';
COMMENT ON COLUMN context_retrieval_sessions.skipped_due_to_size IS 'Whether context retrieval was skipped due to large diff size';

-- Strategy execution tracking
CREATE TABLE context_strategy_executions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES context_retrieval_sessions(id) ON DELETE CASCADE,
    strategy_name VARCHAR(100) NOT NULL,
    execution_order INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    total_candidates INTEGER,
    matches_found INTEGER,
    high_confidence_count INTEGER,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_strategy_executions_session_id ON context_strategy_executions(session_id);
CREATE INDEX idx_strategy_executions_strategy_name ON context_strategy_executions(strategy_name);
CREATE INDEX idx_strategy_executions_status ON context_strategy_executions(status);

COMMENT ON TABLE context_strategy_executions IS 'Tracks individual strategy executions within context retrieval sessions';
COMMENT ON COLUMN context_strategy_executions.status IS 'SUCCESS, TIMEOUT, or ERROR';

-- Context matches storage
CREATE TABLE context_matches (
    id UUID PRIMARY KEY,
    strategy_execution_id UUID NOT NULL REFERENCES context_strategy_executions(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    match_reason VARCHAR(50) NOT NULL,
    confidence NUMERIC(3,2) NOT NULL CHECK (confidence >= 0.0 AND confidence <= 1.0),
    evidence TEXT NOT NULL,
    is_high_confidence BOOLEAN NOT NULL,
    included_in_prompt BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_context_matches_strategy_execution_id ON context_matches(strategy_execution_id);
CREATE INDEX idx_context_matches_file_path ON context_matches(file_path);
CREATE INDEX idx_context_matches_match_reason ON context_matches(match_reason);
CREATE INDEX idx_context_matches_confidence ON context_matches(confidence DESC);
CREATE INDEX idx_context_matches_high_confidence ON context_matches(is_high_confidence) WHERE is_high_confidence = true;

COMMENT ON TABLE context_matches IS 'Individual file matches discovered by context retrieval strategies';
COMMENT ON COLUMN context_matches.included_in_prompt IS 'Whether this match was included in the final prompt sent to LLM';

-- Reason distribution aggregation
CREATE TABLE context_reason_distribution (
    id UUID PRIMARY KEY,
    strategy_execution_id UUID NOT NULL REFERENCES context_strategy_executions(id) ON DELETE CASCADE,
    match_reason VARCHAR(50) NOT NULL,
    count INTEGER NOT NULL CHECK (count >= 0),
    UNIQUE (strategy_execution_id, match_reason)
);

CREATE INDEX idx_reason_distribution_strategy_id ON context_reason_distribution(strategy_execution_id);
CREATE INDEX idx_reason_distribution_reason ON context_reason_distribution(match_reason);

COMMENT ON TABLE context_reason_distribution IS 'Aggregated statistics of match reasons per strategy execution';
