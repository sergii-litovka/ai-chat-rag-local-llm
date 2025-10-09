# API Documentation

This document provides detailed information about the AI Chat Application REST API endpoints.

## Base URL

All API endpoints are relative to: `http://localhost:8080`

## Content Type

All requests and responses use `application/json` content type unless specified otherwise.

## Error Handling

The API returns standard HTTP status codes:
- `200 OK`: Request successful
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

Error responses include detailed messages for debugging.

## Authentication

Currently, the API allows cross-origin requests (`@CrossOrigin(origins = "*")`) and does not require authentication. This should be secured in production environments.

---

## Chat Management Endpoints

### Get All Chats

Retrieves all chat sessions ordered by creation date (newest first).

**Endpoint:** `GET /chats`

**Response:**
```json
[
  {
    "id": 1,
    "title": "My First Chat",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T11:45:00Z",
    "entries": [
      {
        "id": 1,
        "content": "Hello, how are you?",
        "role": "USER",
        "timestamp": "2024-01-15T10:30:00Z"
      },
      {
        "id": 2,
        "content": "Hello! I'm doing well, thank you for asking. How can I help you today?",
        "role": "ASSISTANT",
        "timestamp": "2024-01-15T10:30:15Z"
      }
    ]
  }
]
```

**Example Request:**
```bash
curl -X GET http://localhost:8080/chats
```

---

### Get Specific Chat

Retrieves details of a specific chat including all messages.

**Endpoint:** `GET /chat/{chatId}`

**Parameters:**
- `chatId` (path parameter): The ID of the chat to retrieve

**Response:**
```json
{
  "id": 1,
  "title": "My First Chat",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T11:45:00Z",
  "entries": [
    {
      "id": 1,
      "content": "Hello, how are you?",
      "role": "USER",
      "timestamp": "2024-01-15T10:30:00Z"
    },
    {
      "id": 2,
      "content": "Hello! I'm doing well, thank you for asking. How can I help you today?",
      "role": "ASSISTANT",
      "timestamp": "2024-01-15T10:30:15Z"
    }
  ]
}
```

**Error Responses:**
- `404 Not Found`: Chat with specified ID does not exist

**Example Request:**
```bash
curl -X GET http://localhost:8080/chat/1
```

---

### Create New Chat

Creates a new chat session with the specified title.

**Endpoint:** `POST /chat/new`

**Request Body:**
```json
{
  "title": "Chat about Spring AI"
}
```

**Response:**
```json
{
  "id": 2,
  "title": "Chat about Spring AI",
  "createdAt": "2024-01-15T12:00:00Z",
  "updatedAt": "2024-01-15T12:00:00Z",
  "entries": []
}
```

**Example Request:**
```bash
curl -X POST http://localhost:8080/chat/new \
  -H "Content-Type: application/json" \
  -d '{"title": "Chat about Spring AI"}'
```

---

### Delete Chat

Deletes a chat session and all its associated messages.

**Endpoint:** `DELETE /chat/{chatId}`

**Parameters:**
- `chatId` (path parameter): The ID of the chat to delete

**Response:** `200 OK` with empty body

**Example Request:**
```bash
curl -X DELETE http://localhost:8080/chat/1
```

---

### Add Message to Chat

Adds a user message to a chat and triggers AI response generation.

**Endpoint:** `POST /chat/{chatId}/entry`

**Parameters:**
- `chatId` (path parameter): The ID of the chat to add the message to

**Request Body:**
```json
{
  "content": "What is Spring AI and how does it work?"
}
```

**Response:**
Returns the updated chat with the new user message and AI response:
```json
{
  "id": 1,
  "title": "My First Chat",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T12:15:00Z",
  "entries": [
    {
      "id": 1,
      "content": "Hello, how are you?",
      "role": "USER",
      "timestamp": "2024-01-15T10:30:00Z"
    },
    {
      "id": 2,
      "content": "Hello! I'm doing well, thank you for asking. How can I help you today?",
      "role": "ASSISTANT",
      "timestamp": "2024-01-15T10:30:15Z"
    },
    {
      "id": 3,
      "content": "What is Spring AI and how does it work?",
      "role": "USER",
      "timestamp": "2024-01-15T12:15:00Z"
    },
    {
      "id": 4,
      "content": "Spring AI is a framework that provides Spring-friendly APIs and abstractions for developing AI applications. It offers portable APIs across various AI providers and supports features like chat models, embeddings, vector stores, and function calling...",
      "role": "ASSISTANT",
      "timestamp": "2024-01-15T12:15:30Z"
    }
  ]
}
```

**Processing Flow:**
1. User message is added to the chat
2. AI advisors process the request:
   - Query expansion for better context
   - Memory retrieval from chat history
   - RAG document retrieval for relevant context
   - Response generation using the configured LLM
3. AI response is added to the chat
4. Updated chat is returned

**Example Request:**
```bash
curl -X POST http://localhost:8080/chat/1/entry \
  -H "Content-Type: application/json" \
  -d '{"content": "What is Spring AI and how does it work?"}'
```

---

## Data Models

### Chat
```json
{
  "id": "Long - Unique identifier",
  "title": "String - Chat title",
  "createdAt": "LocalDateTime - Creation timestamp",
  "updatedAt": "LocalDateTime - Last update timestamp",
  "entries": "List<ChatEntry> - List of messages in the chat"
}
```

### ChatEntry
```json
{
  "id": "Long - Unique identifier",
  "content": "String - Message content",
  "role": "Role - USER or ASSISTANT",
  "timestamp": "LocalDateTime - Message timestamp"
}
```

### CreateChatRequest
```json
{
  "title": "String - Title for the new chat"
}
```

### AddMessageRequest
```json
{
  "content": "String - User message content"
}
```

---

## AI Processing Pipeline

When a message is added to a chat, the following processing pipeline is executed:

### 1. Simple Logger (Order 0)
Logs the incoming request for debugging and monitoring.

### 2. Query Expansion (Order 1)
- Uses a separate LLM call to expand the user query
- Improves context understanding and retrieval accuracy
- Configured with specific temperature and parameters for consistent expansion

### 3. Chat Memory (Order 2)
- Retrieves conversation history from PostgreSQL
- Limits the number of messages based on configuration
- Provides context for coherent conversation flow

### 4. Simple Logger (Order 3)
Logs the request after memory injection.

### 5. RAG Advisor (Order 4)
- Performs semantic search using pgvector
- Retrieves relevant documents from the knowledge base
- Applies BM25 reranking for improved relevance
- Injects context into the prompt

### 6. Final Logger (Order 5)
Logs the final request before sending to the LLM.

---

## Rate Limiting and Performance

### Considerations
- Each message triggers multiple LLM calls (expansion + main response)
- Vector search operations on the knowledge base
- Database operations for chat history storage

### Recommendations
- Implement rate limiting for production use
- Consider caching for frequently accessed documents
- Monitor token usage and costs
- Optimize vector search parameters for your use case

---

## Error Scenarios

### Common Error Cases

1. **Chat Not Found (404)**
   ```json
   {
     "error": "Chat not found with ID: 123"
   }
   ```

2. **Invalid Request Body (400)**
   ```json
   {
     "error": "Invalid request format"
   }
   ```

3. **LLM Service Unavailable (500)**
   ```json
   {
     "error": "Failed to process AI request"
   }
   ```

4. **Database Connection Error (500)**
   ```json
   {
     "error": "Database connection failed"
   }
   ```

### Troubleshooting Tips

1. **Slow Response Times**
   - Check Ollama service status
   - Verify database connection
   - Monitor vector search performance

2. **Empty Responses**
   - Check LLM model availability
   - Verify configuration parameters
   - Review system prompts

3. **Memory Issues**
   - Adjust message history limits
   - Check PostgreSQL performance
   - Monitor heap usage

---

## Integration Examples

### JavaScript/Frontend Integration

```javascript
// Get all chats
async function getAllChats() {
  const response = await fetch('/chats');
  return await response.json();
}

// Create new chat
async function createChat(title) {
  const response = await fetch('/chat/new', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ title })
  });
  return await response.json();
}

// Send message
async function sendMessage(chatId, content) {
  const response = await fetch(`/chat/${chatId}/entry`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ content })
  });
  return await response.json();
}
```

### Python Integration

```python
import requests

class ChatClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
    
    def get_all_chats(self):
        response = requests.get(f"{self.base_url}/chats")
        return response.json()
    
    def create_chat(self, title):
        response = requests.post(
            f"{self.base_url}/chat/new",
            json={"title": title}
        )
        return response.json()
    
    def send_message(self, chat_id, content):
        response = requests.post(
            f"{self.base_url}/chat/{chat_id}/entry",
            json={"content": content}
        )
        return response.json()
```

This API documentation provides comprehensive information for integrating with the AI Chat Application.