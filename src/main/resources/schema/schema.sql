CREATE TABLE IF NOT EXISTS chat (
      id         BIGSERIAL PRIMARY KEY,
      title      VARCHAR(255),
      created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_entry (
      id         BIGSERIAL PRIMARY KEY,
      content    TEXT,
      created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
      role       VARCHAR(255) CHECK (role IN ('USER', 'ASSISTANT')),
      chat_id    BIGINT REFERENCES chat(id)
);

CREATE INDEX IF NOT EXISTS idx_chat_entry_chat_id
    ON chat_entry(chat_id);

CREATE TABLE IF NOT EXISTS loaded_document (
       id            SERIAL PRIMARY KEY,
       filename      VARCHAR(255) NOT NULL,
       content_hash  VARCHAR(64) NOT NULL,
       document_type VARCHAR(20) NOT NULL,
       chunk_count   INTEGER CHECK (chunk_count >= 0),
       created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

       CONSTRAINT unique_loaded_document UNIQUE (filename, content_hash)
);

CREATE INDEX IF NOT EXISTS idx_created_document_filename_type
    ON loaded_document(filename, document_type);