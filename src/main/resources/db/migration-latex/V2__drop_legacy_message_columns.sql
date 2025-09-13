-- Remove legacy columns leftover from V7-era schema to align with current entities
-- Safe, idempotent drops for development/staging

ALTER TABLE latex_ai_chat_messages DROP COLUMN IF EXISTS message;
ALTER TABLE latex_ai_chat_messages DROP COLUMN IF EXISTS sender;
ALTER TABLE latex_ai_chat_messages DROP COLUMN IF EXISTS "timestamp";