package com.pacman.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Benchmarks for prompt construction and text processing operations used in
 * the customer support system. These benchmarks cover the prompt-building and
 * knowledge base formatting logic found in AICustomerSupportService and
 * VectorStoreService.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class PromptConstructionBenchmark {

    private static final String PROMPT_TEMPLATE = """
            You are a helpful customer support assistant. Use the following context to answer the question.
            If you don't know the answer, say so politely.

            Context:
            %s

            Question: %s

            Please provide a helpful and friendly response:""";

    @Param({"3", "10", "25"})
    private int contextDocumentCount;

    private List<String> contextDocuments;
    private String userQuestion;

    @Setup(Level.Trial)
    public void setUp() {
        contextDocuments = new ArrayList<>();
        for (int i = 0; i < contextDocumentCount; i++) {
            contextDocuments.add(
                    "Document %d: This is a support document containing information about our %s. "
                            .formatted(i, i % 4 == 0 ? "return policy" : i % 4 == 1 ? "shipping options"
                                    : i % 4 == 2 ? "warranty coverage" : "payment methods")
                            + "Customers can expect comprehensive coverage and detailed explanations "
                            + "for all inquiries related to this topic. Our team is committed to "
                            + "providing the best possible support experience."
            );
        }
        userQuestion = "What is your return policy and how long do I have to return an item?";
    }

    /**
     * Benchmarks full prompt construction including context joining and
     * template formatting, mirroring generateResponse() in
     * AICustomerSupportService.
     */
    @Benchmark
    public String buildFullPrompt() {
        String context = contextDocuments.stream()
                .collect(Collectors.joining("\n---\n"));
        return PROMPT_TEMPLATE.formatted(context, userQuestion);
    }

    /**
     * Benchmarks knowledge base statistics formatting, mirroring
     * getKnowledgeBaseStats() in VectorStoreService.
     */
    @Benchmark
    public String formatKnowledgeBaseStats() {
        int vectorStoreCount = contextDocuments.size();
        long databaseCount = contextDocuments.size() * 2L;

        if (vectorStoreCount == 0) {
            return "Knowledge base is empty. Please load documents first using /load-documents or /load-extended-kb";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Knowledge Base Status:\n");
        sb.append("- Documents in vector store: ").append(vectorStoreCount).append("\n");
        sb.append("- Documents in database: ").append(databaseCount).append("\n");
        sb.append("- Sample topics found: ");

        List<String> topics = contextDocuments.stream()
                .map(doc -> {
                    if (doc.contains("return policy")) return "Returns";
                    if (doc.contains("shipping")) return "Shipping";
                    if (doc.contains("warranty")) return "Warranty";
                    if (doc.contains("payment")) return "Payments";
                    return "General";
                })
                .distinct()
                .collect(Collectors.toList());

        sb.append(String.join(", ", topics));
        return sb.toString();
    }

    /**
     * Benchmarks text truncation for document previews, mirroring the preview
     * logic in getDatabaseDocumentsList().
     */
    @Benchmark
    public void truncateDocumentPreviews(Blackhole bh) {
        for (String doc : contextDocuments) {
            String preview = doc.length() > 100
                    ? doc.substring(0, 100) + "..."
                    : doc;
            bh.consume(preview);
        }
    }

    /**
     * Benchmarks metadata extraction and JSON construction for document
     * chunks, mirroring the metadata handling in VectorStoreService.
     */
    @Benchmark
    public void buildDocumentMetadata(Blackhole bh) {
        for (int i = 0; i < contextDocuments.size(); i++) {
            String doc = contextDocuments.get(i);
            String category = doc.contains("return") ? "returns"
                    : doc.contains("shipping") ? "shipping"
                    : doc.contains("warranty") ? "warranty"
                    : "general";

            String metadata = "{\"source\": \"knowledge-base\", \"category\": \"%s\", \"index\": %d, \"length\": %d}"
                    .formatted(category, i, doc.length());
            bh.consume(metadata);
        }
    }
}
