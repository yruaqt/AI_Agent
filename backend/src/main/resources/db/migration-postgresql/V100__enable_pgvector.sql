CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE knowledge_chunk
    ADD COLUMN IF NOT EXISTS embedding_vector vector(1024);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_hnsw
    ON knowledge_chunk USING hnsw (embedding_vector vector_cosine_ops);
