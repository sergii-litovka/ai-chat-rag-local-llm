# Database Schema Documentation

This document provides comprehensive information about the AI Chat Application database schema, including table structures, relationships, indexes, and design decisions.

## Overview

The AI Chat Application uses PostgreSQL as its primary database with the pgvector extension for vector storage and semantic search capabilities. The schema is designed to support:

- **Chat Management**: Persistent conversation storage
- **Message History**: Individual message tracking with roles
- **Document Management**: Knowledge base document metadata
- **Vector Storage**: Semantic embeddings for RAG (managed by Spring AI)

## Database Architecture

```
┌─────────────┐    ┌─────────────────┐    ┌──────────────────┐
│    chat     │───▶│   chat_entry    │    │ loaded_document  │
│             │    │                 │    │                  │
│ - id (PK)   │    │ - id (PK)       │    │ - id (PK)        │
│ - title     │    │ - content       │    │ - filename       │
│ - created_at│    │ - role          │    │ - content_hash   │
└─────────────┘    │ - chat_id (FK)  │    │ - document_type  │
                   │ - created_at    │    │ - chunk_count    │
                   └─────────────────┘    │ - created_at     │
                                          └──────────────────┘

                   ┌─────────────────────────┐
                   │     vector_store        │
                   │   (Spring AI managed)   │
                   │                         │
                   │ - id                    │
                   │ - content               │
                   │ - metadata              │
                   │ - embedding (vector)    │
                   └─────────────────────────┘
```

## Table Definitions

### 1. chat

**Purpose**: Stores conversation metadata and serves as the parent entity for chat messages.

```sql
CREATE TABLE IF NOT EXISTS chat (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255),
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
);
```

**Columns**:
- `id` (BIGSERIAL, PRIMARY KEY): Unique identifier for each chat session
- `title` (VARCHAR(255), NULLABLE): User-defined title for the conversation
- `created_at` (TIMESTAMP(6), DEFAULT CURRENT_TIMESTAMP): Chat creation timestamp

**Design Decisions**:
- **BIGSERIAL**: Allows for virtually unlimited chat sessions
- **Nullable Title**: Users can create chats without specifying a title
- **Microsecond Precision**: TIMESTAMP(6) provides precise timing for ordering

**Usage Patterns**:
- Retrieved in descending order by `created_at` for chat listing
- Joined with `chat_entry` to fetch complete conversations
- Deleted cascades to remove all associated messages

### 2. chat_entry

**Purpose**: Stores individual messages within conversations, supporting both user inputs and AI responses.

```sql
CREATE TABLE IF NOT EXISTS chat_entry (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    role       VARCHAR(255) CHECK (role IN ('USER', 'ASSISTANT')),
    chat_id    BIGINT REFERENCES chat(id)
);
```

**Columns**:
- `id` (BIGSERIAL, PRIMARY KEY): Unique identifier for each message
- `content` (TEXT, NULLABLE): Message content (supports large text)
- `created_at` (TIMESTAMP(6), DEFAULT CURRENT_TIMESTAMP): Message timestamp
- `role` (VARCHAR(255), CHECK CONSTRAINT): Message sender type
- `chat_id` (BIGINT, FOREIGN KEY): Reference to parent chat

**Constraints**:
- `CHECK (role IN ('USER', 'ASSISTANT'))`: Ensures only valid roles
- `REFERENCES chat(id)`: Foreign key relationship with cascading behavior

**Design Decisions**:
- **TEXT Type**: Supports messages of any reasonable length
- **Role Enumeration**: Explicit constraint prevents invalid role values
- **Temporal Ordering**: Messages ordered by `created_at` within conversations
- **Foreign Key**: Maintains referential integrity with chat table

**Usage Patterns**:
- Retrieved with chat_id filtering for conversation reconstruction
- Ordered by `created_at` for chronological message display
- Limited by memory configuration for context window management

### 3. loaded_document

**Purpose**: Tracks knowledge base documents that have been processed and ingested into the vector store.

```sql
CREATE TABLE IF NOT EXISTS loaded_document (
    id            SERIAL PRIMARY KEY,
    filename      VARCHAR(255) NOT NULL,
    content_hash  VARCHAR(64) NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    chunk_count   INTEGER CHECK (chunk_count >= 0),
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_loaded_document UNIQUE (filename, content_hash)
);
```

**Columns**:
- `id` (SERIAL, PRIMARY KEY): Unique identifier for each loaded document
- `filename` (VARCHAR(255), NOT NULL): Original document filename
- `content_hash` (VARCHAR(64), NOT NULL): SHA-256 hash of document content
- `document_type` (VARCHAR(20), NOT NULL): Document format (PDF, TXT, etc.)
- `chunk_count` (INTEGER, CHECK ≥ 0): Number of chunks created from document
- `created_at` (TIMESTAMP(6), NOT NULL): Document processing timestamp

**Constraints**:
- `UNIQUE (filename, content_hash)`: Prevents duplicate document processing
- `CHECK (chunk_count >= 0)`: Ensures valid chunk count
- `NOT NULL` on critical fields: Ensures data completeness

**Design Decisions**:
- **Content Hash**: Enables change detection for document updates
- **Composite Unique Key**: Allows same filename with different content
- **Chunk Count Tracking**: Monitors document processing effectiveness
- **Document Type**: Supports different file format handling

**Usage Patterns**:
- Checked before document processing to prevent duplicates
- Queried for document management and status reporting
- Updated when documents are reprocessed or removed

### 4. vector_store

**Purpose**: Stores document embeddings for semantic search (managed by Spring AI pgvector integration).

```sql
-- Automatically created by Spring AI pgvector integration
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding vector(1536)  -- Dimension depends on embedding model
);
```

**Note**: This table is automatically managed by Spring AI and may have variations based on configuration.

**Columns**:
- `id` (UUID, PRIMARY KEY): Unique identifier for each vector entry
- `content` (TEXT): Original text content of the document chunk
- `metadata` (JSON): Additional information about the document chunk
- `embedding` (VECTOR): High-dimensional vector representation

**Features**:
- **HNSW Index**: Fast approximate nearest neighbor search
- **Cosine Distance**: Semantic similarity measurement
- **JSON Metadata**: Flexible document attribute storage

## Indexes

### Performance Indexes

```sql
-- Index for chat_entry lookups by chat_id
CREATE INDEX IF NOT EXISTS idx_chat_entry_chat_id
    ON chat_entry(chat_id);

-- Index for loaded_document lookups by filename and type
CREATE INDEX IF NOT EXISTS idx_created_document_filename_type
    ON loaded_document(filename, document_type);
```

**Index Usage**:
- `idx_chat_entry_chat_id`: Optimizes conversation retrieval queries
- `idx_created_document_filename_type`: Speeds up document existence checks

### Vector Indexes

The pgvector extension automatically creates indexes for vector similarity search:

```sql
-- HNSW index for fast approximate search (created by pgvector)
CREATE INDEX ON vector_store USING hnsw (embedding vector_cosine_ops);
```

## Relationships

### Entity Relationships

1. **chat → chat_entry**: One-to-Many
   - One chat can have multiple messages
   - Foreign key ensures referential integrity
   - Cascade delete removes messages when chat is deleted

2. **loaded_document**: Independent
   - No direct foreign key relationships
   - Related to vector_store through metadata correlation
   - Manages knowledge base document lifecycle

3. **vector_store**: Semi-Independent
   - Managed by Spring AI framework
   - Correlated with loaded_document through metadata
   - Contains actual searchable content for RAG

### Referential Integrity

```sql
-- Foreign key constraints
ALTER TABLE chat_entry 
ADD CONSTRAINT fk_chat_entry_chat 
FOREIGN KEY (chat_id) REFERENCES chat(id) 
ON DELETE CASCADE;
```

**Cascade Behavior**:
- Deleting a chat removes all associated chat_entry records
- Preserves data consistency during chat cleanup
- Prevents orphaned message records

## Data Types and Constraints

### Timestamp Precision

All timestamp fields use `TIMESTAMP(6)` for microsecond precision:
- Enables precise message ordering
- Supports high-frequency operations
- Maintains chronological accuracy

### Text Storage

Different text storage strategies:
- `VARCHAR(255)`: Limited length fields (titles, filenames)
- `TEXT`: Unlimited length content (messages, documents)
- `JSON`: Structured metadata storage

### Check Constraints

```sql
-- Role validation
CHECK (role IN ('USER', 'ASSISTANT'))

-- Non-negative chunk count
CHECK (chunk_count >= 0)
```

**Benefits**:
- Data validation at database level
- Prevents invalid data insertion
- Maintains data quality standards

## Performance Considerations

### Query Optimization

1. **Chat Retrieval**:
   ```sql
   -- Optimized with created_at index
   SELECT * FROM chat ORDER BY created_at DESC LIMIT 10;
   ```

2. **Message History**:
   ```sql
   -- Optimized with chat_id index
   SELECT * FROM chat_entry 
   WHERE chat_id = ? 
   ORDER BY created_at ASC;
   ```

3. **Document Existence**:
   ```sql
   -- Optimized with composite index
   SELECT id FROM loaded_document 
   WHERE filename = ? AND content_hash = ?;
   ```

### Memory Management

**Chat Memory Optimization**:
- Sliding window approach limits message retrieval
- Configurable message limits prevent memory overflow
- Efficient pagination for large conversations

**Vector Storage**:
- HNSW indexes provide fast approximate search
- Configurable similarity thresholds filter results
- Batch processing for document ingestion

## Maintenance and Monitoring

### Regular Maintenance Tasks

1. **Index Maintenance**:
   ```sql
   -- Rebuild indexes periodically
   REINDEX TABLE chat_entry;
   REINDEX TABLE loaded_document;
   ```

2. **Statistics Updates**:
   ```sql
   -- Update table statistics for query optimization
   ANALYZE chat;
   ANALYZE chat_entry;
   ANALYZE loaded_document;
   ```

3. **Cleanup Operations**:
   ```sql
   -- Remove old chats (example: older than 90 days)
   DELETE FROM chat 
   WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
   ```

### Monitoring Queries

1. **Storage Usage**:
   ```sql
   SELECT 
       schemaname,
       tablename,
       attname,
       avg_width,
       n_distinct,
       correlation
   FROM pg_stats 
   WHERE schemaname = 'public';
   ```

2. **Index Usage**:
   ```sql
   SELECT 
       indexrelname,
       idx_tup_read,
       idx_tup_fetch
   FROM pg_stat_user_indexes 
   WHERE schemaname = 'public';
   ```

## Migration and Versioning

### Schema Evolution

The application uses Hibernate's `ddl-auto=update` for development:
- Automatically creates new tables and columns
- Does not drop existing structures
- Requires manual intervention for complex changes

### Production Migration Strategy

For production environments, use explicit migration scripts:

```sql
-- Example migration script
-- Version 1.1.0: Add updated_at to chat table

ALTER TABLE chat ADD COLUMN updated_at TIMESTAMP(6);
UPDATE chat SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE chat ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- Create trigger for automatic updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_chat_updated_at 
    BEFORE UPDATE ON chat 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
```

## Backup and Recovery

### Backup Strategy

1. **Full Database Backup**:
   ```bash
   pg_dump -h localhost -U postgres -d vectordb > backup.sql
   ```

2. **Table-Specific Backup**:
   ```bash
   pg_dump -h localhost -U postgres -d vectordb -t chat -t chat_entry > chat_backup.sql
   ```

3. **Vector Data Backup**:
   ```bash
   pg_dump -h localhost -U postgres -d vectordb -t vector_store > vectors_backup.sql
   ```

### Recovery Procedures

1. **Full Restore**:
   ```bash
   psql -h localhost -U postgres -d vectordb_new < backup.sql
   ```

2. **Selective Restore**:
   ```bash
   psql -h localhost -U postgres -d vectordb < chat_backup.sql
   ```

This database schema documentation provides comprehensive information for developers, administrators, and operators working with the AI Chat Application's data layer.