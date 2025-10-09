package com.litovka.chat.repo;

import com.litovka.chat.model.Chat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.litovka.chat.constants.ProjectConstants.CREATED_AT_FIELD;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ChatRepository chatRepository;

    private Chat testChat1;
    private Chat testChat2;

    @BeforeEach
    void setUp() {
        chatRepository.deleteAll();
        
        testChat1 = Chat.builder()
                .title("First Chat")
                .createdAt(LocalDateTime.of(2023, 10, 9, 11, 0))
                .build();

        testChat2 = Chat.builder()
                .title("Second Chat")
                .createdAt(LocalDateTime.of(2023, 10, 9, 12, 0))
                .build();
    }

    @Test
    void shouldSaveAndRetrieveChat() {
        // When
        Chat savedChat = chatRepository.save(testChat1);
        Optional<Chat> retrievedChat = chatRepository.findById(savedChat.getId());

        // Then
        assertThat(retrievedChat).isPresent();
        assertThat(retrievedChat.get().getTitle()).isEqualTo("First Chat");
        assertThat(retrievedChat.get().getId()).isNotNull();
        assertThat(retrievedChat.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindAllChatsOrderedByCreatedAtDesc() {
        // Given
        Chat saved1 = chatRepository.save(testChat1);
        Chat saved2 = chatRepository.save(testChat2);

        // When
        List<Chat> chats = chatRepository.findAll(Sort.by(Sort.Direction.DESC, CREATED_AT_FIELD));

        // Then
        assertThat(chats).hasSize(2);
        // Second chat should come first as it was created later (12:00 vs 11:00)
        assertThat(chats.get(0).getTitle()).isEqualTo("Second Chat");
        assertThat(chats.get(1).getTitle()).isEqualTo("First Chat");
    }

    @Test
    void shouldDeleteChatById() {
        // Given
        Chat savedChat = chatRepository.save(testChat1);
        Long chatId = savedChat.getId();

        // When
        chatRepository.deleteById(chatId);

        // Then
        Optional<Chat> deletedChat = chatRepository.findById(chatId);
        assertThat(deletedChat).isNotPresent();
    }

    @Test
    void shouldReturnEmptyWhenChatNotFound() {
        // Given
        Long nonExistentId = 999L;

        // When
        Optional<Chat> chat = chatRepository.findById(nonExistentId);

        // Then
        assertThat(chat).isNotPresent();
    }

    @Test
    void shouldUpdateChatTitle() {
        // Given
        Chat savedChat = chatRepository.save(testChat1);
        String newTitle = "Updated Chat Title";

        // When
        savedChat.setTitle(newTitle);
        Chat updatedChat = chatRepository.save(savedChat);

        // Then
        assertThat(updatedChat.getTitle()).isEqualTo(newTitle);
        assertThat(updatedChat.getId()).isEqualTo(savedChat.getId());
    }

    @Test
    void shouldCountChats() {
        // Given
        chatRepository.save(testChat1);
        chatRepository.save(testChat2);

        // When
        long count = chatRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldHandleEmptyRepository() {
        // When
        List<Chat> chats = chatRepository.findAll();
        long count = chatRepository.count();

        // Then
        assertThat(chats).isEmpty();
        assertThat(count).isZero();
    }

    @Test
    void shouldMaintainDataIntegrityWithNullValues() {
        // Given
        Chat chatWithNulls = Chat.builder()
                .title(null)  // Testing null title handling
                .build();

        // When
        Chat savedChat = chatRepository.save(chatWithNulls);

        // Then
        assertThat(savedChat.getId()).isNotNull();
        assertThat(savedChat.getTitle()).isNull();
        assertThat(savedChat.getCreatedAt()).isNotNull(); // Should be auto-generated
    }
}