package com.litovka.chat.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Getter
public enum Role {
    USER("user", UserMessage::new),
    ASSISTANT("assistant", AssistantMessage::new),
    SYSTEM("system", SystemMessage::new);

    private final String role;
    private final Function<String, Message> messageFactory;

    // Pre-computed lookup map for O(1) performance
    private static final Map<String, Role> ROLE_LOOKUP =
            Stream.of(values()).collect(Collectors.toMap(Role::getRole, Function.identity()));

    public static Role getRole(String roleName) {
        log.debug("Looking up role for name: {}", roleName);
        Role role = ROLE_LOOKUP.get(roleName);
        if (role == null) {
            log.error("Unknown role requested: {}", roleName);
            throw new IllegalArgumentException("Unknown role: " + roleName);
        }
        log.debug("Found role: {}", role);
        return role;
    }

    public Message getMessage(String prompt) {
        log.debug("Creating message for role: {} with prompt length: {}", this.role, prompt != null ? prompt.length() : 0);
        return messageFactory.apply(prompt);
    }
}

