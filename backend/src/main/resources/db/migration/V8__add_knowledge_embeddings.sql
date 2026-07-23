ALTER TABLE knowledge_chunk ADD COLUMN embedding_provider VARCHAR(32);
ALTER TABLE knowledge_chunk ADD COLUMN embedding_dimension INT;
ALTER TABLE knowledge_chunk ADD COLUMN embedding_data TEXT;
ALTER TABLE knowledge_chunk ADD COLUMN indexed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_knowledge_chunk_embedding_provider
    ON knowledge_chunk (embedding_provider);

ALTER TABLE chat_message ADD COLUMN citations_json TEXT;
