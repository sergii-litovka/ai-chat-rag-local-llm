package com.litovka.chat.model.dto;

/**
 * This class represents a request object for adding a message to a chat.
 * It encapsulates the content of the message to be added to the chat.
 *
 * Fields:
 * - `content` represents the actual text of the message.
 *
 * Usage:
 * This request object is typically sent to endpoints that handle appending new
 * messages to a chat's history.
 *
 * Notes:
 * - The message content can be associated with different roles in the chat
 *   context, such as "system", "user", or "assistant."
 */
public record AddMessageRequest(String content) {
    // The 'role' can be "system", "user", or "assistant"
    // The 'content' is the message text
}
