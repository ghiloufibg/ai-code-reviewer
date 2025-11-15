-- V4__add_raw_llm_response_column.sql
-- Add raw LLM JSON response storage for debugging, auditing, and analysis

-- Add raw_llm_response column to reviews table
ALTER TABLE reviews
  ADD COLUMN raw_llm_response TEXT;

-- Add documentation comment
COMMENT ON COLUMN reviews.raw_llm_response IS 'Raw JSON response from the LLM before parsing. Stored for debugging, auditing, and LLM response analysis purposes. Contains the original, unmodified response including any formatting, escaping issues, or malformed JSON that was sanitized during processing.';
