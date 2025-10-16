# Configuration Documentation

This document provides detailed information about configuring the AI Chat Application, including all available settings, their purposes, and recommended values for different environments.

## Configuration Files

### Primary Configuration
- **Location**: `src/main/resources/application.properties`
- **Format**: Spring Boot properties format
- **Environment Overrides**: Can be overridden with environment variables or profiles

### Additional Configuration Files
- **System Prompt**: `src/main/resources/prompts/system.st`
- **Query Expansion Prompt**: `src/main/resources/prompts/expansion.st`
- **Database Schema**: `src/main/resources/schema/schema.sql`
- **Docker Configuration**: `docker-compose.yaml`

## Configuration Categories

### 1. Application Settings

#### Basic Application Configuration
```properties
# Application name and identification
spring.application.name=ai-chat

# Enable circular reference resolution for Spring AI components
spring.main.allow-circular-references=true
```

**Purpose**: Basic Spring Boot application settings and framework compatibility.

### 2. Ollama LLM Configuration

#### Connection Settings
```properties
# Ollama service base URL
spring.ai.ollama.base-url=http://localhost:11434

# LLM model to use for chat interactions
spring.ai.ollama.chat.model=gemma3:4b-it-q4_K_M
```

**Configuration Options**:
- **base-url**: URL of the Ollama service
  - **Default**: `http://localhost:11434`
  - **Production**: Should point to your Ollama deployment
  - **Docker**: Use service name when containerized

- **model**: LLM model identifier
  - **Default**: `gemma3:4b-it-q4_K_M`
  - **Alternatives**: `llama2`, `mistral`, `codellama`, etc.
  - **Requirements**: Model must be available in Ollama

#### Chat Model Parameters
```properties
# Controls randomness in responses (0.0 = deterministic, 1.0 = very random)
spring.ai.ollama.chat.temperature=0.3

# Number of highest probability vocabulary tokens to keep for nucleus sampling
spring.ai.ollama.chat.top-k=20

# Cumulative probability threshold for nucleus sampling
spring.ai.ollama.chat.top-p=0.7

# Penalty for repeating tokens (1.0 = no penalty, >1.0 = penalty)
spring.ai.ollama.chat.repeat-penalty=1.1
```

**Parameter Guidelines**:
- **temperature**: 
  - `0.0-0.3`: More focused and deterministic responses
  - `0.3-0.7`: Balanced creativity and coherence
  - `0.7-1.0`: More creative but potentially less coherent
- **top-k**: 
  - `1-10`: Very focused vocabulary
  - `10-50`: Balanced vocabulary selection
  - `50+`: Broader vocabulary usage
- **top-p**:
  - `0.1-0.5`: Conservative word choice
  - `0.5-0.9`: Balanced word selection
  - `0.9-1.0`: More diverse word usage
- **repeat-penalty**:
  - `1.0`: No repetition penalty
  - `1.1-1.3`: Moderate penalty (recommended)
  - `1.3+`: Strong penalty (may affect coherence)

#### Query Expansion Configuration
```properties
# Expansion model parameters (more deterministic for consistent expansion)
spring.ai.ollama.expansion.temperature=0.0
spring.ai.ollama.expansion.top-k=1
spring.ai.ollama.expansion.top-p=0.1
spring.ai.ollama.expansion.repeat-penalty=1.0
```

**Purpose**: Query expansion uses more deterministic settings to ensure consistent and focused query enhancement.

### 3. RAG (Retrieval Augmented Generation) Configuration

```properties
# Number of documents to retrieve from vector store
spring.ai.rag.top-k=5

# Minimum similarity score threshold for document inclusion
spring.ai.rag.similarity-score-threshold=0.55

# Multiplier for initial document retrieval before reranking
spring.ai.rag.search-multiplier=2
```

**RAG Parameter Guidelines**:
- **top-k**: 
  - `3-5`: Good balance for most applications
  - `1-3`: When context window is limited
  - `5-10`: For comprehensive knowledge retrieval
- **similarity-score-threshold**:
  - `0.7-1.0`: Very strict relevance (may miss relevant docs)
  - `0.5-0.7`: Balanced relevance filtering
  - `0.3-0.5`: Loose filtering (may include less relevant docs)
- **search-multiplier**:
  - `1`: No oversampling for reranking
  - `2-3`: Moderate oversampling (recommended)
  - `3+`: High oversampling (better quality, slower performance)

### 4. Chat Memory Configuration

```properties
# Maximum number of messages to keep in conversation history
spring.ai.chat.memory.max-messages=8
```

**Memory Guidelines**:
- **max-messages**:
  - `4-8`: Good for most conversations, reasonable context size
  - `8-16`: Better context retention, larger token usage
  - `16+`: Extensive context, may hit token limits

### 5. Database Configuration

#### PostgreSQL Connection
```properties
# Database connection URL with schema specification
spring.datasource.url=jdbc:postgresql://localhost:5432/vectordb?currentSchema=public
spring.datasource.username=postgres
spring.datasource.password=postgres
```

#### JPA/Hibernate Settings
```properties
# Automatically create/update database schema
spring.jpa.hibernate.ddl-auto=update

# Show SQL queries in logs (disable in production)
spring.jpa.show-sql=true

# Format SQL queries for better readability
spring.jpa.properties.hibernate.format_sql=true
```

**Database Configuration Options**:
- **ddl-auto values**:
  - `none`: No schema management
  - `validate`: Validate schema without changes
  - `update`: Update schema as needed (recommended for development)
  - `create`: Create schema on startup (destroys existing data)
  - `create-drop`: Create on startup, drop on shutdown

### 6. Vector Store Configuration

```properties
# Enable automatic vector store schema creation
spring.ai.vectorstore.pgvector.initialize-schema=true

# Vector store table name
spring.ai.vectorstore.pgvector.table-name=vector_store

# Index type for vector similarity search
spring.ai.vectorstore.pgvector.index-type=hnsw

# Distance metric for vector comparisons
spring.ai.vectorstore.pgvector.distance-type=cosine_distance
```

**Vector Store Options**:
- **index-type**:
  - `hnsw`: Hierarchical Navigable Small World (fast, approximate)
  - `ivfflat`: Inverted File Flat (slower, more accurate)
- **distance-type**:
  - `cosine_distance`: Good for semantic similarity
  - `l2_distance`: Euclidean distance
  - `inner_product`: Dot product similarity

### 7. Logging Configuration

```properties
# Enable debug logging for Spring AI advisors
logging.level.org.springframework.ai.chat.client.advisor=DEBUG
```

**Logging Levels**:
- `ERROR`: Only error messages
- `WARN`: Warnings and errors
- `INFO`: General information, warnings, and errors
- `DEBUG`: Detailed debugging information
- `TRACE`: Very detailed trace information

## Environment-Specific Configurations

### Development Environment
```properties
# Development settings
spring.ai.ollama.base-url=http://localhost:11434
spring.datasource.url=jdbc:postgresql://localhost:5432/vectordb
spring.jpa.show-sql=true
logging.level.org.springframework.ai.chat.client.advisor=DEBUG

# More aggressive settings for development
spring.ai.ollama.chat.temperature=0.5
spring.ai.rag.similarity-score-threshold=0.5
```

### Production Environment
```properties
# Production settings
spring.ai.ollama.base-url=http://ollama-service:11434
spring.datasource.url=jdbc:postgresql://postgres-service:5432/vectordb
spring.jpa.show-sql=false
logging.level.org.springframework.ai.chat.client.advisor=INFO

# More conservative settings for production
spring.ai.ollama.chat.temperature=0.3
spring.ai.rag.similarity-score-threshold=0.6
spring.jpa.hibernate.ddl-auto=validate
```

### Testing Environment
```properties
# Testing settings
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.ai.ollama.chat.temperature=0.1
spring.ai.rag.top-k=3
```

## Advanced Configuration

### Custom Prompts

#### System Prompt (`prompts/system.st`)
Location: `src/main/resources/prompts/system.st`

This template defines the AI assistant's behavior and personality:
```
You are a helpful AI assistant with access to relevant context information.
Use the provided context to answer questions accurately and helpfully.
If you don't know something, say so rather than guessing.
```

#### Expansion Prompt (`prompts/expansion.st`)
Location: `src/main/resources/prompts/expansion.st`

This template guides query expansion:
```
Expand the following query to improve document retrieval:
Original query: {query}
Expanded query:
```

### JVM Configuration

#### Memory Settings
```bash
# For development
export JAVA_OPTS="-Xmx2g -Xms1g"

# For production
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"
```

#### Garbage Collection
```bash
# G1GC for better performance with large heaps
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
```

### Docker Configuration

#### Docker Compose Environment Variables
```yaml
environment:
  - SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vectordb
  - SPRING_AI_OLLAMA_CHAT_TEMPERATURE=0.3
  - LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_AI_CHAT_CLIENT_ADVISOR=INFO
```

## Configuration Validation

### Required Settings Checklist
- [ ] Ollama service URL is accessible
- [ ] Database connection parameters are correct
- [ ] LLM model is available in Ollama
- [ ] Vector store table name doesn't conflict
- [ ] Memory limits are appropriate for available RAM

### Performance Tuning Checklist
- [ ] Temperature settings appropriate for use case
- [ ] RAG parameters tuned for document collection
- [ ] Memory limits configured for context requirements
- [ ] Database connection pool sized correctly
- [ ] Logging levels appropriate for environment

### Security Configuration Checklist
- [ ] Database credentials secured (not in plain text)
- [ ] API endpoints protected if needed
- [ ] CORS configuration restrictive for production
- [ ] Input validation enabled
- [ ] Error messages don't leak sensitive information

## Troubleshooting Configuration Issues

### Common Configuration Problems

1. **Ollama Connection Issues**
   ```
   Error: Failed to connect to Ollama service
   Solution: Verify spring.ai.ollama.base-url is correct and service is running
   ```

2. **Database Connection Failures**
   ```
   Error: Connection refused to PostgreSQL
   Solution: Check datasource.url, username, password, and PostgreSQL service
   ```

3. **Model Not Found**
   ```
   Error: Model not found in Ollama
   Solution: Pull model with 'ollama pull model-name' or update configuration
   ```

4. **Vector Store Initialization Errors**
   ```
   Error: pgvector extension not found
   Solution: Install pgvector extension in PostgreSQL
   ```

5. **Memory Issues**
   ```
   Error: OutOfMemoryError
   Solution: Reduce max-messages or increase JVM heap size
   ```

### Configuration Testing

#### Connection Testing
```bash
# Test Ollama connection
curl http://localhost:11434/api/tags

# Test database connection
psql -h localhost -p 5432 -U postgres -d vectordb -c "SELECT version();"
```

#### Configuration Validation Script
```bash
#!/bin/bash
echo "Testing AI Chat Application Configuration..."

# Test Ollama
if curl -f http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✓ Ollama service is accessible"
else
    echo "✗ Ollama service is not accessible"
fi

# Test PostgreSQL
if pg_isready -h localhost -p 5432; then
    echo "✓ PostgreSQL is running"
else
    echo "✗ PostgreSQL is not accessible"
fi

# Test pgvector extension
if psql -h localhost -U postgres -d vectordb -c "SELECT * FROM pg_extension WHERE extname='vector';" | grep -q vector; then
    echo "✓ pgvector extension is installed"
else
    echo "✗ pgvector extension is not installed"
fi
```

This configuration documentation provides comprehensive guidance for setting up and tuning the AI Chat Application for different environments and use cases.


### SSE and SecurityContext
- The chat streaming endpoint uses Server-Sent Events at `GET /chat/{chatId}/stream`.
- Authentication is required for this endpoint (same as the rest of the app). Because it is a GET request, CSRF protection does not block it.
- To ensure the authenticated user is visible to downstream reactive callbacks (e.g., RAG document filtering), the application configures Spring Security to use `MODE_INHERITABLETHREADLOCAL` for `SecurityContextHolder` at startup. This allows SecurityContext to propagate into async/streaming threads.
- Frontend uses same-origin EventSource with cookies. No CORS headers are required; the controller does not enable `@CrossOrigin("*")`.
- If a user is not authenticated, the RAG enrichment is skipped, and the original prompt is sent without inserting any placeholder context.
