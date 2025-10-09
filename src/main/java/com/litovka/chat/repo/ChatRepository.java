package com.litovka.chat.repo;

import com.litovka.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link Chat} entities in the persistence layer.
 * Extends {@link JpaRepository} to provide CRUD operations and additional query capabilities.
 *
 * This repository allows interaction with the `Chat` database table and provides
 * pre-implemented methods such as save, findAll, findById, deleteById, etc.
 * Custom query methods can be declared here as needed.
 *
 * The primary key type for the {@link Chat} entity is {@code Long}.
 */
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // Additional custom query methods can be defined here if needed
}
