package com.litovka.chat.service;

import com.litovka.chat.model.Chat;
import com.litovka.chat.model.ChatEntry;
import com.litovka.chat.repo.ChatRepository;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Implementation of the {@link ChatMemory} interface that provides chat memory
 * storage and retrieval functionality backed by a PostgreSQL database. This class
 * relies on an instance of {@link ChatRepository} for database interactions.
 *
 * The primary responsibilities of this class include:
 * - Adding messages to an existing conversation in the database.
 * - Retrieving messages from a conversation, limiting the number of messages
 *   returned based on the configured maximum message limit.
 * - Clearing a conversation (not implemented in the current version).
 *
 * Key Features:
 * - Logs actions (debug, info, and error levels) to provide operational insights
 *   and facilitate debugging.
 * - Ensures only the most recent messages are retrieved, skipping older entries
 *   if the total exceeds the maximum message limit.
 * - Uses entity models like {@link Chat} and {@link ChatEntry} to interact with
 *   the database efficiently and consistently.
 *
 * Thread Safety:
 * This class is thread-safe provided the underlying {@link ChatRepository} is thread-safe.
 * Operations are wrapped in try-catch blocks to log and handle unexpected failures.
 *
 * Known Limitations:
 * - The `clear` method is not implemented and logs a warning when invoked.
 * - Assumes the conversation IDs provided are valid and parsable as long integers.
 *
 * Usage Considerations:
 * This class is designed to work in environments where a persistence context is
 * configured and available. Database-related exceptions will propagate if any
 * database operation fails.
 *
 * Dependencies:
 * - {@link ChatRepository} for database interactions.
 * - Logging via SLF4J for operational visibility.
 *
 * Properties:
 * - `chatMemoryRepository`: Repository instance for managing `Chat` entities in
 *   the persistence layer.
 * - `maxMessages`: Maximum number of messages to retain in memory for a
 *   conversation during retrieval operations.
 */
@Slf4j
@Builder
public class PostgresChatMemory implements ChatMemory {

    private final ChatRepository chatMemoryRepository;
    private int maxMessages;

    @Override
    public void add(String conversationId, List<Message> messages) {
        log.debug("Adding {} messages to conversation {}", messages.size(), conversationId);
        try {
            Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
            for (Message message : messages) {
                chat.addChatEntry(ChatEntry.toChatEntry(message));
            }
            chatMemoryRepository.save(chat);
            log.info("Successfully added {} messages to conversation {}", messages.size(), conversationId);
        } catch (Exception e) {
            log.error("Failed to add messages to conversation {}", conversationId, e);
            throw e;
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        log.debug("Retrieving messages for conversation {}", conversationId);
        try {
            Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
            long messagesToSkip = Math.max(0, chat.getHistory().size() - maxMessages);
            List<Message> messages = chat.getHistory().stream()
                    .skip(messagesToSkip)
                    .map(ChatEntry::toMessage)
                    .toList();
            log.info("Retrieved {} messages for conversation {} (skipped {} older messages)", 
                    messages.size(), conversationId, messagesToSkip);
            return messages;
        } catch (Exception e) {
            log.error("Failed to retrieve messages for conversation {}", conversationId, e);
            throw e;
        }
    }

    @Override
    public void clear(String conversationId) {
        log.warn("Clear operation called for conversation {} but not implemented", conversationId);
        // Not implemented
    }

}
