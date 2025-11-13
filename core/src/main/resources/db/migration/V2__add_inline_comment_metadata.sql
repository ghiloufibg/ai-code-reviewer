-- V2__add_inline_comment_metadata.sql
-- Add inline comment tracking metadata to review_issues and review_notes

-- Add columns to review_issues
ALTER TABLE review_issues
  ADD COLUMN inline_comment_posted BOOLEAN DEFAULT FALSE NOT NULL,
  ADD COLUMN scm_comment_id VARCHAR(255),
  ADD COLUMN fallback_reason VARCHAR(255),
  ADD COLUMN position_metadata TEXT;

-- Add columns to review_notes
ALTER TABLE review_notes
  ADD COLUMN inline_comment_posted BOOLEAN DEFAULT FALSE NOT NULL,
  ADD COLUMN scm_comment_id VARCHAR(255),
  ADD COLUMN fallback_reason VARCHAR(255),
  ADD COLUMN position_metadata TEXT;

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_issues_inline_posted ON review_issues(inline_comment_posted);
CREATE INDEX IF NOT EXISTS idx_notes_inline_posted ON review_notes(inline_comment_posted);
CREATE INDEX IF NOT EXISTS idx_issues_scm_comment_id ON review_issues(scm_comment_id);
CREATE INDEX IF NOT EXISTS idx_notes_scm_comment_id ON review_notes(scm_comment_id);

-- Add comments for documentation
COMMENT ON COLUMN review_issues.inline_comment_posted IS 'Whether this issue was posted as an inline comment (true) or fallback note (false)';
COMMENT ON COLUMN review_issues.scm_comment_id IS 'SCM platform comment/discussion ID (e.g., GitLab discussion ID, GitHub review comment ID)';
COMMENT ON COLUMN review_issues.fallback_reason IS 'Reason why inline posting failed: INVALID_LINE, API_ERROR, FEATURE_DISABLED';
COMMENT ON COLUMN review_issues.position_metadata IS 'JSON metadata with schema: {"base_sha":"abc123","head_sha":"def456","start_sha":"ghi789","new_line":42,"old_line":40,"new_path":"src/Main.java","old_path":"src/Main.java","position_type":"text"}';

COMMENT ON COLUMN review_notes.inline_comment_posted IS 'Whether this note was posted as an inline comment (true) or fallback note (false)';
COMMENT ON COLUMN review_notes.scm_comment_id IS 'SCM platform comment/discussion ID (e.g., GitLab discussion ID, GitHub review comment ID)';
COMMENT ON COLUMN review_notes.fallback_reason IS 'Reason why inline posting failed: INVALID_LINE, API_ERROR, FEATURE_DISABLED';
COMMENT ON COLUMN review_notes.position_metadata IS 'JSON metadata with schema: {"base_sha":"abc123","head_sha":"def456","start_sha":"ghi789","new_line":15,"old_line":13,"new_path":"src/Utils.java","old_path":"src/Utils.java","position_type":"text"}';
