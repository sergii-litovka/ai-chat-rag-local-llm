package com.litovka.chat.repo;

import com.litovka.chat.model.LoadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link LoadedDocument} entities in the persistence layer.
 * Extends {@link JpaRepository} to provide basic CRUD operations as well as additional query capabilities.
 *
 * This interface allows interaction with the `LoadedDocument` database table and provides pre-implemented methods
 * such as save, findById, findAll, deleteById, etc. It also includes a custom query method for determining the
 * existence of a document based on its filename and content hash.
 *
 * Custom Method:
 * - {@link #existsByFilenameAndContentHash(String, String)}: Checks whether a document with the given filename
 *   and content hash already exists in the database.
 *
 * The primary key type for the {@link LoadedDocument} entity is {@code Long}.
 */
public interface DocumentRepository extends JpaRepository<LoadedDocument, Long> {
    boolean existsByFilenameAndContentHash(String filename, String contentHash);
}