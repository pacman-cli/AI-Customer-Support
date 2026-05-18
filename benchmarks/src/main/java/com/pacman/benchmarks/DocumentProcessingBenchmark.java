package com.pacman.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Benchmarks for document processing operations used in the customer support
 * system. These benchmarks cover the core logic found in VectorStoreService
 * and AICustomerSupportService, including document creation, duplicate
 * detection, and content formatting.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class DocumentProcessingBenchmark {

    /**
     * Simulates a document chunk as stored in the database.
     */
    static class DocumentChunk {
        private final Long id;
        private final String content;
        private final String sourceDocument;
        private final String metadataJson;

        DocumentChunk(Long id, String content, String sourceDocument, String metadataJson) {
            this.id = id;
            this.content = content;
            this.sourceDocument = sourceDocument;
            this.metadataJson = metadataJson;
        }

        public Long getId() { return id; }
        public String getContent() { return content; }
        public String getSourceDocument() { return sourceDocument; }
        public String getMetadataJson() { return metadataJson; }
    }

    @Param({"10", "100", "500"})
    private int documentCount;

    private List<DocumentChunk> existingChunks;
    private List<Map<String, String>> documentsToProcess;
    private String searchContent;

    @Setup(Level.Trial)
    public void setUp() {
        existingChunks = new ArrayList<>();
        documentsToProcess = new ArrayList<>();

        for (int i = 0; i < documentCount; i++) {
            String content = "Document content for chunk %d. This contains information about %s policy. "
                    .formatted(i, i % 3 == 0 ? "return" : i % 3 == 1 ? "shipping" : "warranty")
                    + "Additional details and context are provided here to simulate realistic document sizes. "
                    + "The customer support system processes these chunks for similarity search.";
            String source = "knowledge-base-" + (i % 5);
            String metadata = "{\"source\": \"%s\", \"category\": \"support\", \"index\": %d}".formatted(source, i);

            existingChunks.add(new DocumentChunk((long) i, content, source, metadata));

            Map<String, String> doc = new HashMap<>();
            doc.put("content", content);
            doc.put("source", source);
            doc.put("metadata", metadata);
            documentsToProcess.add(doc);
        }

        // Content to search for — exists at roughly the midpoint
        int midpoint = documentCount / 2;
        searchContent = existingChunks.get(midpoint).getContent();
    }

    /**
     * Benchmarks duplicate detection using stream-based search, mirroring the
     * documentExists() method in VectorStoreService.
     */
    @Benchmark
    public boolean duplicateDetection() {
        String targetContent = searchContent;
        String targetSource = "knowledge-base-2";
        return existingChunks.stream()
                .anyMatch(chunk ->
                        chunk.getContent().equals(targetContent)
                                && chunk.getSourceDocument().equals(targetSource));
    }

    /**
     * Benchmarks building a formatted document list with content previews,
     * mirroring getDatabaseDocumentsList() in VectorStoreService.
     */
    @Benchmark
    public String formatDocumentList() {
        if (existingChunks.isEmpty()) {
            return "No documents found in the database.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Database Documents (").append(existingChunks.size()).append(" total):\n\n");

        for (int i = 0; i < existingChunks.size(); i++) {
            DocumentChunk chunk = existingChunks.get(i);
            String preview = chunk.getContent().length() > 100
                    ? chunk.getContent().substring(0, 100) + "..."
                    : chunk.getContent();
            sb.append(i + 1).append(". [").append(chunk.getSourceDocument()).append("] ")
                    .append(preview).append("\n");
        }

        return sb.toString();
    }

    /**
     * Benchmarks saving documents to the database with duplicate checking,
     * mirroring saveDocumentsToDatabase() in VectorStoreService.
     */
    @Benchmark
    public void saveWithDuplicateCheck(Blackhole bh) {
        List<DocumentChunk> saved = new ArrayList<>();
        for (Map<String, String> doc : documentsToProcess) {
            String content = doc.get("content");
            String source = doc.get("source");

            boolean exists = saved.stream()
                    .anyMatch(chunk ->
                            chunk.getContent().equals(content)
                                    && chunk.getSourceDocument().equals(source));

            if (!exists) {
                saved.add(new DocumentChunk(
                        (long) saved.size(),
                        content,
                        source,
                        doc.get("metadata")
                ));
            }
        }
        bh.consume(saved);
    }

    /**
     * Benchmarks extracting content from document chunks and joining them,
     * mirroring the context-building logic in AICustomerSupportService.
     */
    @Benchmark
    public String buildContext() {
        return existingChunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n---\n"));
    }
}
