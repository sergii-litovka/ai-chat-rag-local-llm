package com.litovka.chat.service;

import com.litovka.chat.model.Chat;
import com.litovka.chat.repo.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.litovka.chat.constants.ProjectConstants.CHAT_NOT_FOUND_MESSAGE;
import static com.litovka.chat.constants.ProjectConstants.CREATED_AT_FIELD;

/**
 * Service class for managing chat operations. This class provides methods for
 * creating, retrieving, and deleting chat entries, as well as handling chat interactions
 * with external systems.
 *
 * It interacts with the underlying data repository and an external chat client
 * to perform the required business logic. All methods are transactional to ensure
 * data consistency and thread safety.
 *
 * Key responsibilities include:
 * - Retrieving all chats from the repository, sorted by the creation date in descending order.
 * - Creating a new chat with a specified title and persisting it to the repository.
 * - Fetching a single chat by its unique identifier.
 * - Deleting a chat by its identifier from the repository.
 * - Handling interactions for a specific chat by communicating with an external chat client.
 *
 * Uses {@link ChatRepository} for persistence and {@link ChatClient} for external interactions.
 * Logs operations at different levels such as debug and info to provide a detailed trace of executions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepo;
    private final ChatClient chatClient;

    @Transactional(readOnly = true)
    public List<Chat> getAllChats() {
        log.debug("Retrieving all chats");
        List<Chat> chats = chatRepo.findAll(Sort.by(Sort.Direction.DESC, CREATED_AT_FIELD));
        log.info("Retrieved {} chats", chats.size());
        return chats;
    }

    @Transactional
    public Chat createNewChat(String title) {
        log.info("Creating new chat with title: {}", title);
        Chat chat = Chat.builder().title(title).build();
        Chat savedChat = chatRepo.save(chat);
        log.info("Created new chat with ID: {}", savedChat.getId());
        return savedChat;
    }

    @Transactional(readOnly = true)
    public Chat getChat(Long chatId) {
        log.debug("Retrieving chat with ID: {}", chatId);
        return getChatById(chatId);
    }

    @Transactional
    public void deleteChat(Long chatId) {
        log.info("Deleting chat with ID: {}", chatId);
        chatRepo.deleteById(chatId);
        log.info("Successfully deleted chat with ID: {}", chatId);
    }

    @Transactional
    public void proceedInteraction(Long chatId, String prompt) {
        log.info("Processing interaction for chat ID: {} with prompt length: {}", chatId, prompt != null ? prompt.length() : 0);
        try {
            chatClient.prompt()
                    .user(prompt)
                    .advisors(a -> a.param("chat_memory_conversation_id", String.valueOf(chatId)))
                    .call()
                    .content();
            log.debug("Successfully processed interaction for chat ID: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to process interaction for chat ID: {}", chatId, e);
            throw e;
        }
    }

    private Chat getChatById(Long chatId) {
        log.debug("Looking up chat by ID: {}", chatId);
        return chatRepo.findById(chatId)
                .orElseThrow(() -> {
                    log.error("Chat not found with ID: {}", chatId);
                    return new IllegalArgumentException(CHAT_NOT_FOUND_MESSAGE + chatId);
                });
    }

}
