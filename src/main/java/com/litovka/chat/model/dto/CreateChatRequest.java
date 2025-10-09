package com.litovka.chat.model.dto;

/**
 * Represents a request for creating a new chat.
 *
 * This class is typically used as a data transfer object (DTO) to encapsulate
 * the details required to create a new chat resource. The primary information
 * conveyed by this class is the title of the chat.
 *
 * Purpose:
 * The `CreateChatRequest` record is used in contexts where a new chat
 * needs to be initialized, such as API endpoints for chat management.
 * Example usage includes passing an instance of this class to service
 * methods or controllers to create and store a new chat.
 *
 * Fields:
 * - `title`: The name or title of the chat to be created.
 *
 * Example Context:
 * Within the `ChatController` class, this object is received as input
 * to handle chat creation operations through the `/chat/new` endpoint.
 *
 * Constraints:
 * The `title` should not be null and is expected to be a concise name
 * that identifies the chat.
 */
public record CreateChatRequest(String title) {
}
