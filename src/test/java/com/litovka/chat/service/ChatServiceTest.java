package com.litovka.chat.service;

import com.litovka.chat.model.Chat;
import com.litovka.chat.repo.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.litovka.chat.constants.ProjectConstants.CHAT_NOT_FOUND_MESSAGE;
import static com.litovka.chat.constants.ProjectConstants.CREATED_AT_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatClient chatClient;


    @InjectMocks
    private ChatService chatService;

    private Chat testChat;

    @BeforeEach
    void setUp() {
        testChat = Chat.builder()
                .id(1L)
                .title("Test Chat")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldGetAllChats() {
        // Given
        List<Chat> expectedChats = Arrays.asList(testChat);
        when(chatRepository.findAll(Sort.by(Sort.Direction.DESC, CREATED_AT_FIELD)))
                .thenReturn(expectedChats);

        // When
        List<Chat> result = chatService.getAllChats();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Chat");
        verify(chatRepository).findAll(Sort.by(Sort.Direction.DESC, CREATED_AT_FIELD));
    }

    @Test
    void shouldCreateNewChat() {
        // Given
        String title = "New Chat";
        Chat expectedChat = Chat.builder().id(2L).title(title).build();
        when(chatRepository.save(any(Chat.class))).thenReturn(expectedChat);

        // When
        Chat result = chatService.createNewChat(title);

        // Then
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getId()).isEqualTo(2L);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void shouldGetChatById() {
        // Given
        Long chatId = 1L;
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(testChat));

        // When
        Chat result = chatService.getChat(chatId);

        // Then
        assertThat(result).isEqualTo(testChat);
        verify(chatRepository).findById(chatId);
    }

    @Test
    void shouldThrowExceptionWhenChatNotFound() {
        // Given
        Long chatId = 999L;
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getChat(chatId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(CHAT_NOT_FOUND_MESSAGE + chatId);
        verify(chatRepository).findById(chatId);
    }

    @Test
    void shouldDeleteChat() {
        // Given
        Long chatId = 1L;

        // When
        chatService.deleteChat(chatId);

        // Then
        verify(chatRepository).deleteById(chatId);
    }

    // Note: ChatClient interaction testing would require complex mocking of fluent interface
    // For now, we focus on the core business logic that can be reliably tested
    // Integration tests will cover the full ChatClient interaction flow
}