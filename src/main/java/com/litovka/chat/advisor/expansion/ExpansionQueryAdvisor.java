package com.litovka.chat.advisor.expansion;

import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.litovka.chat.constants.ProjectConstants.ENRICHED_QUESTION;
import static com.litovka.chat.constants.ProjectConstants.EXPANSION_RATIO;
import static com.litovka.chat.constants.ProjectConstants.EXPANSION_TEMPLATE_PATH;
import static com.litovka.chat.constants.ProjectConstants.ORIGINAL_QUESTION;

/**
 * The ExpansionQueryAdvisor is responsible for augmenting a user-provided query
 * by enriching it with additional context or information using a specified expansion prompt.
 * This advisor interacts with a ChatClient instance to generate refined queries,
 * which are then passed along in the advising chain.
 * It implements the {@link BaseAdvisor} interface and primarily functions in two stages,
 * namely before and after the query interaction.
 *
 * Key Functionalities:
 * - Uses a customizable expansion template to generate enriched questions from user queries.
 * - Computes an enrichment ratio to measure the degree of expansion of the original query.
 * - Handles ChatClient interactions for processing enriched queries.
 * - Allows integration into a chain of advisors for advanced query handling.
 *
 * Features:
 * - Customizable configuration through the builder pattern.
 * - Compatibility with a variety of chat models and configuration options such as temperature, topK, topP, and repeat penalty.
 * - Encapsulation of the query expansion logic to isolate this behavior from other parts of the system.
 *
 * The class relies on an external context, such as a configuration-specific expansion template,
 * which is loaded during its operation. It offers robust query processing with minimal intrusion into other system components.
 */
@Builder
public class ExpansionQueryAdvisor implements BaseAdvisor {

    private ChatClient chatClient;

    public static ExpansionQueryAdvisorBuilder builder(ChatModel chatModel, 
                                                        double temperature, 
                                                        int topK, 
                                                        double topP, 
                                                        double repeatPenalty) {
        return new ExpansionQueryAdvisorBuilder().chatClient(ChatClient.builder(chatModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(temperature)
                        .topK(topK)
                        .topP(topP)
                        .repeatPenalty(repeatPenalty)
                        .build())
                .build());
    }

    @Getter
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        PromptTemplate template = loadExpansionTemplate();
        String userQuestion = chatClientRequest.prompt().getUserMessage().getText();
        String enrichedQuestion = generateEnrichedQuestion(template, userQuestion);
        double ratio = enrichedQuestion.length() / (double) userQuestion.length();

        return chatClientRequest.mutate()
                .context(ORIGINAL_QUESTION, userQuestion)
                .context(ENRICHED_QUESTION, enrichedQuestion)
                .context(EXPANSION_RATIO, ratio)
                .build();
    }

    private PromptTemplate loadExpansionTemplate() {
        try {
            Resource expansionTemplate = new ClassPathResource(EXPANSION_TEMPLATE_PATH);
            return PromptTemplate.builder()
                    .template(expansionTemplate.getContentAsString(StandardCharsets.UTF_8))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load expansion template", e);
        }
    }

    private String generateEnrichedQuestion(PromptTemplate template, String userQuestion) {
        return chatClient
                .prompt()
                .user(template.render(Map.of("question", userQuestion)))
                .call()
                .content();
    }


    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

}
