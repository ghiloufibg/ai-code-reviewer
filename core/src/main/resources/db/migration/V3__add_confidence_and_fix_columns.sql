-- V3__add_confidence_and_fix_columns.sql
-- Add confidence scoring and one-click fix application columns to review_issues

-- Add confidence scoring columns
ALTER TABLE review_issues
  ADD COLUMN confidence_score DECIMAL(3,2)
    CHECK (confidence_score >= 0.00 AND confidence_score <= 1.00),
  ADD COLUMN confidence_explanation TEXT;

-- Add fix application columns
ALTER TABLE review_issues
  ADD COLUMN suggested_fix TEXT,
  ADD COLUMN fix_diff TEXT,
  ADD COLUMN fix_applied BOOLEAN DEFAULT FALSE NOT NULL,
  ADD COLUMN applied_at TIMESTAMP,
  ADD COLUMN applied_commit_sha VARCHAR(40);

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_issues_confidence_score ON review_issues(confidence_score)
  WHERE confidence_score IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_issues_fix_applied ON review_issues(fix_applied, applied_at)
  WHERE fix_applied = TRUE;

CREATE INDEX IF NOT EXISTS idx_issues_commit_sha ON review_issues(applied_commit_sha)
  WHERE applied_commit_sha IS NOT NULL;

-- Add documentation comments
COMMENT ON COLUMN review_issues.confidence_score IS 'AI confidence level (0.0-1.0) indicating certainty about the issue. Higher scores indicate more reliable suggestions.';
COMMENT ON COLUMN review_issues.confidence_explanation IS 'Brief explanation of why the AI assigned this confidence score, considering pattern clarity, context completeness, and false positive risk.';
COMMENT ON COLUMN review_issues.suggested_fix IS 'AI-generated code snippet showing the corrected code that should replace the problematic section.';
COMMENT ON COLUMN review_issues.fix_diff IS 'Unified diff patch format showing exact changes to apply. Used for one-click fix application via SCM API.';
COMMENT ON COLUMN review_issues.fix_applied IS 'Whether this AI-suggested fix has been applied to the repository via one-click apply feature.';
COMMENT ON COLUMN review_issues.applied_at IS 'Timestamp when the fix was successfully applied to the repository.';
COMMENT ON COLUMN review_issues.applied_commit_sha IS 'Git commit SHA hash of the commit that applied this fix. Used for tracking and audit purposes.';
