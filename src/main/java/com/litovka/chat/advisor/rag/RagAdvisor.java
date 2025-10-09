package com.litovka.chat.advisor.rag;

import com.litovka.chat.advisor.rag.reranker.StreamBM25Reranker;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.litovka.chat.constants.ProjectConstants.ENRICHED_QUESTION;

/**
 * RagAdvisor is an implementation of the BaseAdvisor interface designed to provide
 * dynamic context enrichment for user queries using Retrieval-Augmented Generation (RAG).
 * It integrates with a vector store for similarity-based document retrieval, reranks search
 * results, and builds enriched query prompts to augment responses.
 *
 * Built using the builder pattern, this class allows for fine-grained configuration
 * of parameters such as the number of top results (`topK`), similarity thresholds, and
 * search multipliers.
 *
 * Key functionalities include:
 * - Search request generation with user-specific filters to ensure personalized document access.
 * - Secure handling and sanitization of usernames for safe filtering of results.
 * - Dynamic adjustment of query contexts based on RAG search results.
 *
 * Features:
 * - RAG-based document retrieval and relevance scoring.
 * - Allows reranking using BM25 for higher accuracy in retrieval.
 * - Custom prompt template for shaping query contexts and questions.
 */
@Slf4j
@Builder
public class RagAdvisor implements BaseAdvisor {

    private final int topK;
    private final double similarityScoreThreshold;
    private final int searchMultiplier;

    @Builder.Default
    private static final PromptTemplate template = PromptTemplate.builder().template("""
            CONTEXT: {context}
            QUESTION: {question}
            """).build();


    private VectorStore vectorStore;

    public static RagAdvisorBuilder build(VectorStore vectorStore, int topK, double similarityScoreThreshold, int searchMultiplier) {
        return new RagAdvisorBuilder()
                .vectorStore(vectorStore)
                .topK(topK)
                .similarityScoreThreshold(similarityScoreThreshold)
                .searchMultiplier(searchMultiplier);
    }


    @Builder.Default
    private StreamBM25Reranker reranker = StreamBM25Reranker.builder().build();

    @Getter
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        log.debug("RagAdvisor processing request");
        String originalUserQuestion = chatClientRequest.prompt().getUserMessage().getText();
        String queryToRag = chatClientRequest.context().getOrDefault(ENRICHED_QUESTION, originalUserQuestion).toString();
        
        log.info("Processing RAG query with length: {}, enriched: {}", 
                originalUserQuestion.length(), !queryToRag.equals(originalUserQuestion));

        SearchRequest searchRequest = buildSearchRequest();
        if (searchRequest == null) {
            log.warn("No valid search request created, returning authentication required context");
            return chatClientRequest.mutate().context("CONTEXT", "Authentication required for document access").build();
        }

        List<Document> documents = performDocumentSearch(queryToRag, searchRequest);
        if (documents.isEmpty()) {
            log.warn("No documents found for query");
            return chatClientRequest.mutate().context("CONTEXT", "Can't find any related documents").build();
        }

        log.info("Found {} relevant documents for RAG context", documents.size());
        String finalUserPrompt = buildFinalPrompt(documents, queryToRag, originalUserQuestion);
        log.debug("Built final prompt with context length: {}", finalUserPrompt.length());
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentUserMessage(finalUserPrompt)).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    private SearchRequest buildSearchRequest() {
        String username = getCurrentUsername();
        if (username == null || username.isEmpty()) {
            log.warn("No authenticated user found, proceeding without filename filter");
            return null;
        }

        String sanitizedUsername = sanitizeUsername(username);
        return SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(similarityScoreThreshold)
                .filterExpression("source_filename == '" + sanitizedUsername + "'")
                .build();
    }

    private List<Document> performDocumentSearch(String queryToRag, SearchRequest searchRequest) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.from(searchRequest)
                .query(queryToRag).topK(topK * searchMultiplier).build());

        return reranker.rerank(documents, queryToRag, topK);
    }

    private String buildFinalPrompt(List<Document> documents, String queryToRag, String originalUserQuestion) {
        String llmContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        return template.render(Map.of("context", llmContext, "question", originalUserQuestion));
    }

    /**
     * Safely retrieves the current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            // Handle anonymous authentication
            if ("anonymousUser".equals(authentication.getName())) {
                return null;
            }
            return authentication.getName();
        } catch (Exception e) {
            log.error("Error retrieving authenticated user", e);
            return null;
        }
    }

    /**
     * IMPORTANT SECURITY NOTE: Sanitizes username to prevent filter injection attacks
     * This prevents malicious usernames like: ' OR 1=1 --
     */
    private String sanitizeUsername(String username) {
        if (username == null) {
            return "";
        }
        // Remove potentially dangerous characters for filter expressions
        // Allow only alphanumeric, underscore, hyphen, dot, and @
        String sanitized = username.replaceAll("[^a-zA-Z0-9_\\-\\.@]", "");

        // Limit length to prevent extremely long usernames
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
            log.warn("Username truncated for security: original length was {}", username.length());
        }

        return sanitized.toLowerCase();
    }


}
