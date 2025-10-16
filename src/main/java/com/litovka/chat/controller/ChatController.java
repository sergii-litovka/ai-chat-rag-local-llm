package com.litovka.chat.controller;

import com.litovka.chat.model.Chat;
import com.litovka.chat.model.dto.AddMessageRequest;
import com.litovka.chat.model.dto.CreateChatRequest;
import com.litovka.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * The ChatController class is a REST controller that provides endpoints for managing
 * and interacting with chat resources. It acts as a layer between the client and the
 * ChatService, handling HTTP requests and responses. It includes endpoints for
 * retrieving chat details, creating new chats, deleting chats, and adding messages
 * to chats.
 *
 * Annotations:
 * - `@RestController` marks this class as a Spring MVC controller, providing REST endpoints.
 * - `@Slf4j` adds logging support.
 * - `@RequiredArgsConstructor` automatically generates a constructor for final fields.
 * - `@CrossOrigin(origins = "*")` allows cross-origin requests to this controller.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chats")
    public ResponseEntity<List<Chat>> getAllChats() {
        log.debug("GET /chats - Retrieving all chats");
        List<Chat> chats = chatService.getAllChats();
        log.info("GET /chats - Returning {} chats", chats.size());
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<Chat> getChat(@PathVariable Long chatId) {
        log.debug("GET /chat/{} - Retrieving chat", chatId);
        try {
            Chat chat = chatService.getChat(chatId);
            if (chat == null) {
                log.warn("GET /chat/{} - Chat not found", chatId);
                return ResponseEntity.notFound().build();
            }
            log.info("GET /chat/{} - Chat found and returned", chatId);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            log.error("GET /chat/{} - Error retrieving chat", chatId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/chat/new")
    public ResponseEntity<Chat> createNewChat(@RequestBody CreateChatRequest request) {
        log.info("POST /chat/new - Creating new chat with title: {}", request.title());
        try {
            Chat chat = chatService.createNewChat(request.title());
            log.info("POST /chat/new - Successfully created chat with ID: {}", chat.getId());
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            log.error("POST /chat/new - Error creating chat with title: {}", request.title(), e);
            throw e;
        }
    }

    @DeleteMapping("/chat/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        log.info("DELETE /chat/{} - Deleting chat", chatId);
        try {
            chatService.deleteChat(chatId);
            log.info("DELETE /chat/{} - Successfully deleted chat", chatId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("DELETE /chat/{} - Error deleting chat", chatId, e);
            throw e;
        }
    }

    @PostMapping("/chat/{chatId}/entry")
    public ResponseEntity<Chat> addMessage(@PathVariable Long chatId, @RequestBody AddMessageRequest request) {
        log.info("POST /chat/{}/entry - Adding message with content length: {}", chatId, request.content() != null ? request.content().length() : 0);
        try {
            chatService.proceedInteraction(chatId, request.content());
            Chat updatedChat = chatService.getChat(chatId);
            log.info("POST /chat/{}/entry - Successfully added message", chatId);
            return ResponseEntity.ok(updatedChat);
        } catch (Exception e) {
            log.error("POST /chat/{}/entry - Error adding message", chatId, e);
            throw e;
        }
    }

    // Streaming endpoint using SSE
    @GetMapping(value = "/chat/{chatId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable Long chatId, @RequestParam(name = "q") String prompt) {
        log.info("GET /chat/{}/stream - Starting SSE stream", chatId);
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        try {
            chatService.processInteraction(chatId, prompt, emitter);
        } catch (Exception e) {
            handleStreamError(chatId, emitter, e);
        }
        return emitter;
    }

    private void handleStreamError(Long chatId, SseEmitter emitter, Exception e) {
        log.error("GET /chat/{}/stream - Error starting stream", chatId, e);
        try {
            emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
        } catch (Exception sendException) {
            log.warn("Failed to send error event to SSE emitter for chat {}", chatId, sendException);
        }
        emitter.completeWithError(e);
    }

}
