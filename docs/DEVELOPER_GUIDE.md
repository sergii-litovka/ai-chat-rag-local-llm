# Developer Guide

This guide provides comprehensive information for developers who want to extend, modify, or contribute to the AI Chat Application.

## Table of Contents
1. [Development Setup](#development-setup)
2. [Project Structure](#project-structure)
3. [Key Concepts](#key-concepts)
4. [Extension Points](#extension-points)
5. [Custom Implementation Examples](#custom-implementation-examples)
6. [Testing Guidelines](#testing-guidelines)
7. [Performance Optimization](#performance-optimization)
8. [Deployment Considerations](#deployment-considerations)

## Development Setup

### Prerequisites
- **Java 17+**: Required for Spring Boot 3.x
- **Docker & Docker Compose**: For infrastructure services
- **Ollama**: Local LLM server
- **IDE**: IntelliJ IDEA (recommended) or VS Code
- **PostgreSQL Client**: For database management

### Local Development Environment

1. **Clone and Setup**:
   ```bash
   git clone <repository-url>
   cd ai-chat
   ./gradlew build
   ```

2. **Start Infrastructure**:
   ```bash
   docker-compose up -d postgres
   ollama serve
   ollama pull gemma3:4b-it-q4_K_M
   ```

3. **Run Application**:
   ```bash
   ./gradlew bootRun
   ```

4. **Development Configuration**:
   Create `application-dev.properties`:
   ```properties
   # Development-specific settings
   spring.ai.ollama.chat.temperature=0.5
   spring.jpa.show-sql=true
   logging.level.org.springframework.ai=DEBUG
   logging.level.com.litovka.chat=DEBUG
   ```

### IDE Configuration

#### IntelliJ IDEA
1. **Lombok Plugin**: Enable annotation processing
2. **Database Plugin**: Configure PostgreSQL connection
3. **Spring Boot Plugin**: For application management
4. **Code Style**: Use project-specific formatting

#### VS Code Extensions
- Spring Boot Extension Pack
- Java Extension Pack
- Lombok Annotations Support
- PostgreSQL Client

## Project Structure

### Package Organization
```
src/main/java/com/litovka/chat/
├── advisor/           # Custom AI advisors
│   ├── expansion/     # Query expansion logic
│   └── rag/           # RAG implementation
├── conf/              # Spring configuration
├── controller/        # REST endpoints
├── model/             # Data models and DTOs
├── repo/              # JPA repositories
└── service/           # Business logic
```

### Key Design Patterns

#### 1. Configuration Pattern
All AI components are configured through Spring Beans with external properties:
```java
@Configuration
@RequiredArgsConstructor
public class ChatConfiguration {
    // Centralized configuration for AI components
}
```

#### 2. Advisor Chain Pattern
Processing pipeline using ordered advisors:
```java
.defaultAdvisors(
    new SimpleLoggerAdvisor(0),      // Logging
    expansionAdvisor(1),             // Query expansion
    memoryAdvisor(2),                // Memory injection
    ragAdvisor(4)                    // Document retrieval
)
```

#### 3. Repository Pattern
Data access through Spring Data JPA:
```java
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    // Custom query methods
}
```

## Key Concepts

### 1. Advisor Chain
Advisors modify requests/responses in a pipeline:

```java
public interface Advisor {
    AdvisorResponse advise(AdvisorRequest request);
    String getName();
    int getOrder();
}
```

**Key Points**:
- Lower order numbers execute first
- Each advisor can modify the request
- Chain execution is synchronous
- Advisors can access previous modifications

### 2. Chat Memory
Conversation history management:

```java
public interface ChatMemory {
    void add(String conversationId, List<Message> messages);
    List<Message> get(String conversationId, int lastN);
    void clear(String conversationId);
}
```

**Implementation Details**:
- PostgreSQL storage for persistence
- Sliding window for memory limits
- Efficient retrieval with pagination

### 3. RAG (Retrieval Augmented Generation)
Document-based context enhancement:

```java
// Vector search → BM25 reranking → context injection
VectorStore → StreamBM25Reranker → PromptTemplate
```

## Extension Points

### 1. Custom Advisors

#### Creating a Custom Advisor
```java
@Component
public class CustomAdvisor implements Advisor {
    
    @Override
    public AdvisorResponse advise(AdvisorRequest request) {
        // Custom processing logic
        ChatOptions modifiedOptions = modifyOptions(request.getChatOptions());
        UserMessage modifiedMessage = processMessage(request.getUserMessage());
        
        return AdvisorResponse.builder()
            .chatOptions(modifiedOptions)
            .userMessage(modifiedMessage)
            .build();
    }
    
    @Override
    public String getName() {
        return "CustomAdvisor";
    }
    
    @Override
    public int getOrder() {
        return 10; // Order in the chain
    }
}
```

#### Registering the Advisor
```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder, CustomAdvisor customAdvisor) {
    return builder
        .defaultAdvisors(
            // ... existing advisors
            customAdvisor
        )
        .build();
}
```

### 2. Custom Memory Strategies

#### Implementing Custom Memory
```java
@Service
public class RediseChatMemory implements ChatMemory {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = "chat:" + conversationId;
        redisTemplate.opsForList().rightPushAll(key, messages.toArray());
        // Set expiration
        redisTemplate.expire(key, Duration.ofDays(7));
    }
    
    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = "chat:" + conversationId;
        return redisTemplate.opsForList()
            .range(key, -lastN, -1)
            .stream()
            .map(Message.class::cast)
            .collect(Collectors.toList());
    }
}
```

### 3. Custom Document Loaders

#### Implementing Document Loader
```java
@Service
public class CustomDocumentLoader implements DocumentLoader {
    
    public List<Document> loadFromAPI(String apiEndpoint) {
        // Fetch documents from external API
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(apiEndpoint, ApiResponse.class);
        
        return response.getBody().getData().stream()
            .map(this::convertToDocument)
            .collect(Collectors.toList());
    }
    
    private Document convertToDocument(ApiDocument apiDoc) {
        return new Document(
            apiDoc.getContent(),
            Map.of(
                "source", apiDoc.getSource(),
                "type", "api",
                "timestamp", apiDoc.getCreatedAt()
            )
        );
    }
}
```

## Custom Implementation Examples

### 1. Sentiment Analysis Advisor

```java
@Component
@RequiredArgsConstructor
public class SentimentAnalysisAdvisor implements Advisor {
    
    private final SentimentAnalysisService sentimentService;
    
    @Override
    public AdvisorResponse advise(AdvisorRequest request) {
        UserMessage userMessage = request.getUserMessage();
        String content = userMessage.getContent();
        
        // Analyze sentiment
        SentimentResult sentiment = sentimentService.analyze(content);
        
        // Add sentiment to message metadata
        Map<String, Object> metadata = new HashMap<>(userMessage.getMetadata());
        metadata.put("sentiment", sentiment.getLabel());
        metadata.put("confidence", sentiment.getConfidence());
        
        UserMessage enrichedMessage = new UserMessage(content, metadata);
        
        return AdvisorResponse.builder()
            .userMessage(enrichedMessage)
            .build();
    }
    
    @Override
    public int getOrder() {
        return 1; // Early in the chain
    }
}
```

### 2. Rate Limiting Advisor

```java
@Component
public class RateLimitingAdvisor implements Advisor {
    
    private final RedisRateLimiter rateLimiter;
    
    @Override
    public AdvisorResponse advise(AdvisorRequest request) {
        String userId = extractUserId(request);
        
        if (!rateLimiter.isAllowed(userId, 10, Duration.ofMinutes(1))) {
            throw new RateLimitExceededException("Too many requests");
        }
        
        return AdvisorResponse.builder().build(); // Pass through
    }
    
    @Override
    public int getOrder() {
        return -1; // Very early in the chain
    }
}
```

### 3. Content Filtering Advisor

```java
@Component
public class ContentFilterAdvisor implements Advisor {
    
    private final List<String> prohibitedWords;
    private final Pattern sensitivePattern;
    
    @Override
    public AdvisorResponse advise(AdvisorRequest request) {
        String content = request.getUserMessage().getContent();
        
        // Check for prohibited content
        if (containsProhibitedContent(content)) {
            throw new ContentViolationException("Message contains prohibited content");
        }
        
        // Sanitize sensitive information
        String sanitized = sanitizeContent(content);
        UserMessage sanitizedMessage = new UserMessage(sanitized);
        
        return AdvisorResponse.builder()
            .userMessage(sanitizedMessage)
            .build();
    }
}
```

### 4. Custom Vector Store

```java
@Configuration
public class CustomVectorStoreConfig {
    
    @Bean
    @ConditionalOnProperty(name = "vector.store.type", havingValue = "custom")
    public VectorStore customVectorStore() {
        return new CustomVectorStore();
    }
}

public class CustomVectorStore implements VectorStore {
    
    private final ElasticsearchClient client;
    
    @Override
    public void add(List<Document> documents) {
        List<VectorDocument> vectorDocs = documents.stream()
            .map(this::convertToVectorDocument)
            .collect(Collectors.toList());
            
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (VectorDocument doc : vectorDocs) {
            bulkBuilder.operations(op -> op
                .index(idx -> idx
                    .index("vectors")
                    .document(doc)
                )
            );
        }
        
        client.bulk(bulkBuilder.build());
    }
    
    @Override
    public List<Document> similaritySearch(String query, int k) {
        // Implement similarity search using Elasticsearch
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index("vectors")
            .query(q -> q
                .scriptScore(ss -> ss
                    .query(Query.of(mq -> mq.matchAll(ma -> ma)))
                    .script(Script.of(sc -> sc
                        .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                        .params("query_vector", JsonValue.of(embedQuery(query)))
                    ))
                )
            )
            .size(k)
        );
        
        return executeSearch(searchRequest);
    }
}
```

## Testing Guidelines

### 1. Unit Testing

#### Testing Services
```java
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    
    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private ChatClient chatClient;
    
    @InjectMocks
    private ChatService chatService;
    
    @Test
    void shouldCreateNewChat() {
        // Given
        String title = "Test Chat";
        Chat expectedChat = Chat.builder().title(title).build();
        when(chatRepository.save(any(Chat.class))).thenReturn(expectedChat);
        
        // When
        Chat result = chatService.createNewChat(title);
        
        // Then
        assertThat(result.getTitle()).isEqualTo(title);
        verify(chatRepository).save(any(Chat.class));
    }
}
```

#### Testing Advisors
```java
@Test
void shouldExpandQuery() {
    // Given
    AdvisorRequest request = AdvisorRequest.builder()
        .userMessage(new UserMessage("test query"))
        .build();
    
    // When
    AdvisorResponse response = expansionQueryAdvisor.advise(request);
    
    // Then
    assertThat(response.getUserMessage().getContent())
        .contains("expanded")
        .contains("test query");
}
```

### 2. Integration Testing

#### Database Integration
```java
@SpringBootTest
@Testcontainers
class ChatRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private ChatRepository chatRepository;
    
    @Test
    void shouldPersistChat() {
        // Given
        Chat chat = Chat.builder().title("Integration Test").build();
        
        // When
        Chat saved = chatRepository.save(chat);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
```

#### AI Integration Testing
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.ollama.base-url=http://localhost:11434"
})
class ChatClientIntegrationTest {
    
    @Autowired
    private ChatClient chatClient;
    
    @Test
    @EnabledIf("isOllamaAvailable")
    void shouldGenerateResponse() {
        // Given
        String prompt = "Hello, how are you?";
        
        // When
        String response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();
        
        // Then
        assertThat(response).isNotBlank();
    }
    
    private boolean isOllamaAvailable() {
        try {
            URL url = new URL("http://localhost:11434/api/tags");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 3. Performance Testing

```java
@Test
void shouldHandleConcurrentRequests() throws InterruptedException {
    int numberOfThreads = 10;
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    
    for (int i = 0; i < numberOfThreads; i++) {
        final int threadId = i;
        executor.submit(() -> {
            try {
                Chat chat = chatService.createNewChat("Concurrent Test " + threadId);
                assertThat(chat.getId()).isNotNull();
            } finally {
                latch.countDown();
            }
        });
    }
    
    boolean finished = latch.await(30, TimeUnit.SECONDS);
    assertThat(finished).isTrue();
}
```

## Performance Optimization

### 1. Database Optimization

#### Connection Pooling
```properties
# HikariCP configuration
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
```

#### Query Optimization
```java
// Use projections for large result sets
public interface ChatSummary {
    Long getId();
    String getTitle();
    LocalDateTime getCreatedAt();
}

@Query("SELECT c.id as id, c.title as title, c.createdAt as createdAt FROM Chat c")
List<ChatSummary> findAllSummaries();
```

### 2. Caching Strategies

#### Redis Integration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();
            
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

@Service
public class CachedChatService {
    
    @Cacheable(value = "chats", key = "#chatId")
    public Chat getChat(Long chatId) {
        return chatRepository.findById(chatId).orElse(null);
    }
    
    @CacheEvict(value = "chats", key = "#chat.id")
    public Chat updateChat(Chat chat) {
        return chatRepository.save(chat);
    }
}
```

### 3. Async Processing

```java
@Service
public class AsyncDocumentService {
    
    @Async("documentProcessingExecutor")
    public CompletableFuture<Void> processDocumentsAsync(List<Document> documents) {
        // Process documents in background
        vectorStore.add(documents);
        return CompletableFuture.completedFuture(null);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("documentProcessingExecutor")
    public TaskExecutor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-processing-");
        executor.initialize();
        return executor;
    }
}
```

## Deployment Considerations

### 1. Container Optimization

#### Multi-stage Dockerfile
```dockerfile
# Build stage
FROM gradle:8-jdk17 AS build
COPY . /app
WORKDIR /app
RUN ./gradlew build -x test

# Runtime stage
FROM openjdk:17-jre-slim
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

#### Docker Compose for Production
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
    depends_on:
      - postgres
      - redis
    
  postgres:
    image: pgvector/pgvector:pg15
    environment:
      POSTGRES_DB: vectordb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
```

### 2. Monitoring and Observability

#### Actuator Configuration
```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true
```

#### Custom Health Indicators
```java
@Component
public class OllamaHealthIndicator implements HealthIndicator {
    
    private final OllamaApi ollamaApi;
    
    @Override
    public Health health() {
        try {
            ollamaApi.listModels();
            return Health.up()
                .withDetail("ollama", "Available")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("ollama", "Unavailable")
                .withException(e)
                .build();
        }
    }
}
```

### 3. Security Best Practices

#### Production Security Configuration
```java
@Configuration
@EnableWebSecurity
public class ProductionSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Configure CSRF properly
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .build();
    }
}
```

This developer guide provides comprehensive information for extending and maintaining the AI Chat Application. For additional support, refer to the other documentation files or create an issue in the repository.