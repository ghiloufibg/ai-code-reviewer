-- V6__drop_context_audit_tables.sql
-- Remove context audit tables for MVP simplification

-- Drop tables in reverse dependency order to avoid constraint violations
DROP TABLE IF EXISTS context_reason_distribution CASCADE;
DROP TABLE IF EXISTS context_matches CASCADE;
DROP TABLE IF EXISTS context_strategy_executions CASCADE;
DROP TABLE IF EXISTS context_retrieval_sessions CASCADE;
