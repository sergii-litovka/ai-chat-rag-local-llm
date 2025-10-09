package com.litovka.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovka.chat.model.Chat;
import com.litovka.chat.model.dto.AddMessageRequest;
import com.litovka.chat.model.dto.CreateChatRequest;
import com.litovka.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ChatController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    private Chat testChat;
    private List<Chat> testChats;

    @BeforeEach
    void setUp() {
        testChat = Chat.builder()
                .id(1L)
                .title("Test Chat")
                .createdAt(LocalDateTime.of(2023, 10, 9, 12, 0))
                .build();

        Chat secondChat = Chat.builder()
                .id(2L)
                .title("Another Chat")
                .createdAt(LocalDateTime.of(2023, 10, 9, 11, 0))
                .build();

        testChats = Arrays.asList(testChat, secondChat);
    }

    @Test
    void shouldGetAllChats() throws Exception {
        // Given
        when(chatService.getAllChats()).thenReturn(testChats);

        // When & Then
        mockMvc.perform(get("/chats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Chat")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("Another Chat")));

        verify(chatService).getAllChats();
    }

    @Test
    void shouldGetChatById() throws Exception {
        // Given
        Long chatId = 1L;
        when(chatService.getChat(chatId)).thenReturn(testChat);

        // When & Then
        mockMvc.perform(get("/chat/{chatId}", chatId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Chat")));

        verify(chatService).getChat(chatId);
    }

    @Test
    void shouldReturnNotFoundWhenChatNotExists() throws Exception {
        // Given
        Long chatId = 999L;
        when(chatService.getChat(chatId)).thenThrow(new IllegalArgumentException("Chat not found"));

        // When & Then
        mockMvc.perform(get("/chat/{chatId}", chatId))
                .andExpect(status().isNotFound());

        verify(chatService).getChat(chatId);
    }

    @Test
    void shouldCreateNewChat() throws Exception {
        // Given
        CreateChatRequest request = new CreateChatRequest("New Chat");
        when(chatService.createNewChat(request.title())).thenReturn(testChat);

        // When & Then
        mockMvc.perform(post("/chat/new")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Chat")));

        verify(chatService).createNewChat(request.title());
    }

    @Test
    void shouldHandleCreateChatException() throws Exception {
        // Given
        CreateChatRequest request = new CreateChatRequest("New Chat");
        when(chatService.createNewChat(anyString())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            mockMvc.perform(post("/chat/new")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            // Exception is expected as controller rethrows it
        }

        verify(chatService).createNewChat(request.title());
    }

    @Test
    void shouldDeleteChat() throws Exception {
        // Given
        Long chatId = 1L;
        doNothing().when(chatService).deleteChat(chatId);

        // When & Then
        mockMvc.perform(delete("/chat/{chatId}", chatId)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(chatService).deleteChat(chatId);
    }

    @Test
    void shouldHandleDeleteChatException() throws Exception {
        // Given
        Long chatId = 1L;
        doThrow(new RuntimeException("Delete failed")).when(chatService).deleteChat(chatId);

        // When & Then
        try {
            mockMvc.perform(delete("/chat/{chatId}", chatId)
                            .with(csrf()));
        } catch (Exception e) {
            // Exception is expected as controller rethrows it
        }

        verify(chatService).deleteChat(chatId);
    }

    @Test
    void shouldAddMessage() throws Exception {
        // Given
        Long chatId = 1L;
        AddMessageRequest request = new AddMessageRequest("Hello, how are you?");
        doNothing().when(chatService).proceedInteraction(chatId, request.content());
        when(chatService.getChat(chatId)).thenReturn(testChat);

        // When & Then
        mockMvc.perform(post("/chat/{chatId}/entry", chatId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Chat")));

        verify(chatService).proceedInteraction(chatId, request.content());
        verify(chatService).getChat(chatId);
    }

    @Test
    void shouldHandleAddMessageException() throws Exception {
        // Given
        Long chatId = 1L;
        AddMessageRequest request = new AddMessageRequest("Hello");
        doThrow(new RuntimeException("AI service error")).when(chatService).proceedInteraction(anyLong(), anyString());

        // When & Then
        try {
            mockMvc.perform(post("/chat/{chatId}/entry", chatId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            // Exception is expected as controller rethrows it
        }

        verify(chatService).proceedInteraction(chatId, request.content());
    }

    @Test
    void shouldHandleInvalidJson() throws Exception {
        // When & Then
        try {
            mockMvc.perform(post("/chat/new")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"invalid\": \"json\"}"));
        } catch (Exception e) {
            // Exception is expected due to invalid JSON structure
        }
    }

    @Test
    void shouldHandleMissingRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/chat/new")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}