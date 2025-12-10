-- V7__drop_suggested_fix_columns.sql
-- Remove suggested fix feature columns from review_issues
-- The suggested_fix feature is being removed as LLMs cannot reliably generate valid Base64-encoded diffs

-- Drop indexes first (if they exist)
DROP INDEX IF EXISTS idx_issues_fix_applied;
DROP INDEX IF EXISTS idx_issues_commit_sha;

-- Drop columns
ALTER TABLE review_issues
  DROP COLUMN IF EXISTS suggested_fix,
  DROP COLUMN IF EXISTS fix_diff,
  DROP COLUMN IF EXISTS fix_applied,
  DROP COLUMN IF EXISTS applied_at,
  DROP COLUMN IF EXISTS applied_commit_sha;
