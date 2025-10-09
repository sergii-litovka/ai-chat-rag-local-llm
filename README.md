# AI Chat Application

##  Overview

A sophisticated AI-powered chat application built with Spring Boot and Spring AI, featuring Retrieval Augmented Generation (RAG), conversation memory, and advanced query processing.

## 🚀 Features

- **AI-Powered Conversations**: Integration with Ollama LLM models
- **Retrieval Augmented Generation (RAG)**: Enhanced responses using document knowledge base
- **Conversation Memory**: Persistent chat history with PostgreSQL storage
- **Query Expansion**: Intelligent query enhancement for better context understanding
- **Vector Search**: Semantic search using pgvector with HNSW indexing
- **Real-time Chat**: Web-based chat interface
- **Multiple Chat Sessions**: Support for multiple concurrent conversations
- **Document Loading**: Knowledge base management system

## 🏗️ Architecture

The application follows a layered architecture with the following key components:

### Core Components
- **ChatController**: REST API endpoints for chat operations
- **ChatService**: Business logic for chat management and AI interactions
- **ChatConfiguration**: Spring AI configuration with multiple advisors
- **PostgresChatMemory**: Custom chat memory implementation using PostgreSQL

### AI Pipeline
The application uses a sophisticated advisor chain:
1. **SimpleLoggerAdvisor**: Request/response logging
2. **ExpansionQueryAdvisor**: Query expansion for better context
3. **MessageChatMemoryAdvisor**: Conversation history management
4. **RagAdvisor**: Document retrieval and context injection
5. **Final Logging**: Response logging

### Data Models
- **Chat**: Main chat entity with title and timestamps
- **ChatEntry**: Individual messages within a chat
- **LoadedDocument**: Knowledge base documents for RAG

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.x
- **AI Integration**: Spring AI with Ollama
- **Database**: PostgreSQL with pgvector extension
- **Vector Store**: pgvector for semantic search
- **Frontend**: HTML, CSS, JavaScript
- **Build Tool**: Gradle
- **Containerization**: Docker Compose

## 📋 Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Ollama server running locally
- PostgreSQL with pgvector extension

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ai-chat
```

### 2. Start Infrastructure
```bash
# Start PostgreSQL with pgvector
docker-compose up -d

# Verify Ollama is running
curl http://localhost:11434/api/tags
```

### 3. Configure Ollama Model
```bash
# Pull the required model (or change in application.properties)
ollama pull gemma3:4b-it-q4_K_M
```

### 4. Run the Application
```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

## ⚙️ Configuration

### Application Properties

Key configuration options in `src/main/resources/application.properties`:

#### Ollama Configuration
```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=gemma3:4b-it-q4_K_M
spring.ai.ollama.chat.temperature=0.3
spring.ai.ollama.chat.top-k=20
spring.ai.ollama.chat.top-p=0.7
spring.ai.ollama.chat.repeat-penalty=1.1
```

#### RAG Configuration
```properties
spring.ai.rag.top-k=5
spring.ai.rag.similarity-score-threshold=0.55
spring.ai.rag.search-multiplier=2
```

#### Database Configuration
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/vectordb?currentSchema=public
spring.ai.vectorstore.pgvector.table-name=vector_store
spring.ai.vectorstore.pgvector.index-type=hnsw
spring.ai.vectorstore.pgvector.distance-type=cosine_distance
```

## 📚 API Documentation

### Chat Management Endpoints

#### Get All Chats
```http
GET /chats
```
Returns a list of all chat sessions ordered by creation date (newest first).

#### Get Specific Chat
```http
GET /chat/{chatId}
```
Returns details of a specific chat including all messages.

#### Create New Chat
```http
POST /chat/new
Content-Type: application/json

{
  "title": "Chat Title"
}
```

#### Delete Chat
```http
DELETE /chat/{chatId}
```

#### Add Message to Chat
```http
POST /chat/{chatId}/entry
Content-Type: application/json

{
  "content": "User message content"
}
```

## 🧠 AI Features

### Query Expansion
The system automatically expands user queries to improve context understanding and retrieval accuracy.

### RAG (Retrieval Augmented Generation)
- Documents are stored as vectors in PostgreSQL using pgvector
- Semantic search finds relevant context for each query
- BM25 reranking improves result quality
- Configurable similarity thresholds and result counts

### Memory Management
- Conversation history is stored in PostgreSQL
- Configurable message limits for context window management
- Automatic cleanup of old conversations

### Multi-Language Support
- Automatic language detection using Apache Tika
- Language-specific stop word generation
- Optimized processing for different languages

## 🔧 Development

### Project Structure
```
src/main/java/com/litovka/chat/
├── advisor/               # AI advisors for query processing
│   ├── expansion/        # Query expansion logic
│   └── rag/             # RAG implementation with reranking
├── conf/                # Spring configuration
├── controller/          # REST controllers
├── model/              # Data models and DTOs
├── repo/               # JPA repositories
└── service/            # Business logic services
```

### Adding New Features

#### Custom Advisors
Extend the advisor chain by implementing Spring AI's `Advisor` interface and adding it to `ChatConfiguration`.

#### Document Loading
Use `DocumentLoaderService` to add new documents to the knowledge base.

#### Custom Memory Strategies
Implement custom chat memory by extending the `ChatMemory` interface.

## 🐳 Docker Deployment

The project includes Docker Compose configuration for easy deployment:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🔍 Monitoring and Logging

The application provides comprehensive logging:
- Request/response logging for all chat interactions
- AI advisor execution logging
- Database operation logging
- Error tracking with stack traces

Configure logging levels in `application.properties`:
```properties
logging.level.org.springframework.ai.chat.client.advisor=DEBUG
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Troubleshooting

### Common Issues

1. **Ollama Connection Issues**
   - Ensure Ollama is running: `ollama serve`
   - Verify model is available: `ollama list`

2. **Database Connection**
   - Check PostgreSQL is running
   - Verify pgvector extension is installed

3. **Memory Issues**
   - Adjust JVM heap size for large document processing
   - Configure appropriate message limits

### Support

For issues and questions, please create an issue in the GitHub repository.
