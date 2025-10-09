package com.litovka.chat.service;

import com.litovka.chat.model.LoadedDocument;
import com.litovka.chat.repo.DocumentRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Service responsible for loading, processing, and storing document data asynchronously.
 * This service uses a bounded thread pool for efficient and safe concurrent processing
 * and integrates with a vector store for storing document chunks and metadata for later retrieval.
 *
 * The documents are located based on a resource pattern and are processed into smaller chunks
 * with metadata enhancement, then stored in a repository and a vector database.
 *
 * Key responsibilities:
 * - Asynchronously load documents on application startup.
 * - Process documents by splitting their content into manageable chunks.
 * - Skip already processed documents by verifying file and content hash.
 * - Enhance document chunks with additional metadata.
 * - Persist document data in both a vector store and database.
 *
 * Thread safety and concurrency:
 * - Uses a bounded {@link ExecutorService} to limit concurrent processing.
 * - Avoids unbounded parallelism to prevent out-of-memory issues.
 *
 * Notes:
 * - The service ensures safe resource management (e.g., closing streams).
 * - Logs detailed processing information for debugging and monitoring.
 *
 * Implementation details:
 * - Documents are split based on token size using a configurable chunking strategy.
 * - Metadata is added to each chunk, including the source filename and content length.
 * - The vector store is used for fast retrieval and similarity searches.
 * - Metadata about loaded documents is stored in a relational database repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentLoaderService {

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;
    private final ResourcePatternResolver resolver;

    private static final int CHUNK_SIZE = 200;
    private static final int MAX_NUM_CHUNKS = 100;
    private static final String RESOURCE_PATTERN = "classpath:/knowledgebase/**/*.{pdf,doc,docx,ppt,pptx,txt,html}";

    // Bounded thread pool to prevent unbounded parallelism and OOM
    // Size can be tuned; using CPU count gives reasonable throughput without memory spikes.
    private final ExecutorService documentExecutor =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    @PreDestroy
    public void shutdown() {
        documentExecutor.shutdown();
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public CompletableFuture<Void> loadDocumentsAsync() {
        try {
            List<Resource> resources = Arrays.stream(
                    resolver.getResources(RESOURCE_PATTERN)
            ).toList();

            List<CompletableFuture<Void>> futures = new ArrayList<>(resources.size());

            for (Resource resource : resources) {
                String contentHash = calcContentHash(resource);

                // Skip already processed documents
                if (documentRepository.existsByFilenameAndContentHash(resource.getFilename(), contentHash)) {
                    continue;
                }

                Pair<Resource, String> pair = Pair.of(resource, contentHash);

                // Submit each document to a bounded executor, not to the common pool
                futures.add(CompletableFuture.runAsync(() -> processDocument(pair), documentExecutor));
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            return all.whenComplete((v, t) -> {
                if (t == null) {
                    log.info("All {} documents loaded successfully", futures.size());
                } else {
                    log.error("One or more documents failed to load", t);
                }
            });
        } catch (Exception e) {
            log.error("Failed to load documents", e);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    // Synchronous document processing logic; concurrency is controlled by the executor above
    private void processDocument(Pair<Resource, String> pair) {
        Resource resource = pair.getFirst();
        String contentHash = pair.getSecond();

        try {
            List<Document> chunks = readAndSplitDocument(resource);

            if (chunks.isEmpty()) {
                log.warn("No chunks generated for document: {}", resource.getFilename());
                return;
            }

            enhanceChunksWithMetadata(chunks, resource.getFilename());
            persistDocumentData(chunks, resource, contentHash);

        } catch (Exception e) {
            log.error("Failed to process document: {}", resource.getFilename(), e);
        }
    }

    private List<Document> readAndSplitDocument(Resource resource) {
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        List<Document> documents = tikaReader.get();

        logDocumentProcessingInfo(resource.getFilename(), documents);

        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMaxNumChunks(MAX_NUM_CHUNKS)
                .build();

        List<Document> chunks = textSplitter.apply(documents);
        log.info("After splitting: {} original documents -> {} chunks", documents.size(), chunks.size());

        return chunks;
    }

    private void enhanceChunksWithMetadata(List<Document> chunks, String filename) {
        String filenameWithoutExtension = getFilenameWithoutExtension(filename);

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("source_filename", filenameWithoutExtension);

            String chunkContent = chunk.getText();
            int chunkLength = chunkContent != null ? chunkContent.length() : 0;
            log.info("Chunk {}: Length = {} characters", i, chunkLength);
        }
    }

    private void logDocumentProcessingInfo(String filename, List<Document> documents) {
        log.info("Processing document: {} with {} documents", filename, documents.size());

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getText();
            int contentLength = content != null ? content.length() : 0;
            log.info("Document {}: Content length = {} characters", i, contentLength);
        }
    }

    private void persistDocumentData(List<Document> chunks, Resource resource, String contentHash) {
        // Store in vector database
        vectorStore.accept(chunks);

        // Save metadata
        String documentType = getFileExtension(resource.getFilename());
        LoadedDocument loadedDocument = LoadedDocument.builder()
                .documentType(documentType)
                .chunkCount(chunks.size())
                .filename(resource.getFilename())
                .contentHash(contentHash)
                .build();

        documentRepository.save(loadedDocument);

        log.debug("Processed {} document: {} ({} chunks)",
                documentType, resource.getFilename(), chunks.size());
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getFilenameWithoutExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return filename;
        }
        return filename.substring(0, filename.lastIndexOf(".")).toLowerCase();
    }

    @SneakyThrows
    private String calcContentHash(Resource resource) {
        // Ensure stream is closed to prevent resource leaks
        try (InputStream is = resource.getInputStream()) {
            return DigestUtils.md5DigestAsHex(is);
        }
    }
}

