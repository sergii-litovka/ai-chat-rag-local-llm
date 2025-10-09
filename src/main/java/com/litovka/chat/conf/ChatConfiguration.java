package com.litovka.chat.conf;

import com.litovka.chat.advisor.expansion.ExpansionQueryAdvisor;
import com.litovka.chat.advisor.rag.RagAdvisor;
import com.litovka.chat.repo.ChatRepository;
import com.litovka.chat.service.PostgresChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuration class for the AI Chat Application.
 * 
 * <p>This class configures the Spring AI components including:
 * <ul>
 *   <li>Ollama API connection and chat model configuration</li>
 *   <li>ChatClient with a sophisticated advisor chain for AI processing</li>
 *   <li>Custom advisors for query expansion, RAG, and memory management</li>
 *   <li>PostgreSQL-based chat memory implementation</li>
 * </ul>
 * 
 * <p>The advisor chain processes requests in the following order:
 * <ol>
 *   <li>SimpleLoggerAdvisor (Order 0) - Request logging</li>
 *   <li>ExpansionQueryAdvisor (Order 1) - Query expansion for better context</li>
 *   <li>MessageChatMemoryAdvisor (Order 2) - Conversation history injection</li>
 *   <li>SimpleLoggerAdvisor (Order 3) - Intermediate logging</li>
 *   <li>RagAdvisor (Order 4) - Document retrieval and context augmentation</li>
 *   <li>SimpleLoggerAdvisor (Order 5) - Final logging</li>
 * </ol>
 * 
 * @author AI Chat Application
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatConfiguration {

    @Value("classpath:/prompts/system.st")
    private Resource SYSTEM_PROMPT;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModelName;

    @Value("${spring.ai.ollama.chat.temperature}")
    private double chatTemperature;
    
    @Value("${spring.ai.ollama.chat.top-k}")
    private int chatTopK;
    
    @Value("${spring.ai.ollama.chat.top-p}")
    private double chatTopP;
    
    @Value("${spring.ai.ollama.chat.repeat-penalty}")
    private double chatRepeatPenalty;
    
    @Value("${spring.ai.ollama.expansion.temperature}")
    private double expansionTemperature;
    
    @Value("${spring.ai.ollama.expansion.top-k}")
    private int expansionTopK;
    
    @Value("${spring.ai.ollama.expansion.top-p}")
    private double expansionTopP;
    
    @Value("${spring.ai.ollama.expansion.repeat-penalty}")
    private double expansionRepeatPenalty;
    
    @Value("${spring.ai.rag.top-k}")
    private int ragTopK;
    
    @Value("${spring.ai.rag.similarity-score-threshold}")
    private double ragSimilarityScoreThreshold;
    
    @Value("${spring.ai.rag.search-multiplier}")
    private int ragSearchMultiplier;
    
    @Value("${spring.ai.chat.memory.max-messages}")
    private int maxMessagesFromHistory;

    private final ChatRepository chatRepository;
    //private final VectorStore vectorStore;

    @Bean
    public OllamaApi ollamaApi() {
        log.info("Creating OllamaApi with base URL: {}", ollamaBaseUrl);
        OllamaApi api = OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
        log.info("OllamaApi created successfully");
        return api;
    }

    @Bean
    public ChatModel chatModel() {
        log.info("Creating ChatModel with model: {}, temperature: {}, topK: {}, topP: {}, repeatPenalty: {}", 
                chatModelName, chatTemperature, chatTopK, chatTopP, chatRepeatPenalty);
        ChatModel model = OllamaChatModel.builder()
                .ollamaApi(ollamaApi())
                .defaultOptions(OllamaChatOptions.builder()
                        .model(chatModelName)
                        .temperature(chatTemperature)
                        .topK(chatTopK)
                        .topP(chatTopP)
                        .repeatPenalty(chatRepeatPenalty)
                        .build())
                .build();
        log.info("ChatModel created successfully");
        return model;
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        log.info("Creating ChatClient with RAG configuration - topK: {}, similarity threshold: {}, search multiplier: {}", 
                ragTopK, ragSimilarityScoreThreshold, ragSearchMultiplier);
        log.info("Chat memory configuration - max messages: {}", maxMessagesFromHistory);
        
        ChatClient client = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(chatTemperature)
                        .topP(chatTopP)
                        .topK(chatTopK)
                        .repeatPenalty(chatRepeatPenalty)
                        .build())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(0),
                        ExpansionQueryAdvisor.builder(chatModel(),
                                expansionTemperature,
                                expansionTopK,
                                expansionTopP,
                                expansionRepeatPenalty)
                                .order(1)
                                .build(),
                        getHistoryAdvisor(2),
                        new SimpleLoggerAdvisor(3),
                        RagAdvisor.build(vectorStore,
                                ragTopK, 
                                ragSimilarityScoreThreshold, 
                                ragSearchMultiplier).order(4).build(),
                        new SimpleLoggerAdvisor(5))
                .build();
        
        log.info("ChatClient created successfully with 5 advisors configured");
        return client;
    }

    private Advisor getHistoryAdvisor(int order) {
        log.debug("Creating history advisor with order: {}", order);
        return MessageChatMemoryAdvisor.builder(getChatMemory())
                .order(order)
                .build();
    }

    private ChatMemory getChatMemory() {
        log.debug("Creating PostgresChatMemory with max messages: {}", maxMessagesFromHistory);
        return PostgresChatMemory.builder()
                .maxMessages(maxMessagesFromHistory)
                .chatMemoryRepository(chatRepository)
                .build();
    }
}
