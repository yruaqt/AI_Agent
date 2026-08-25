CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_content_trgm
    ON knowledge_chunk USING gin (content gin_trgm_ops);
