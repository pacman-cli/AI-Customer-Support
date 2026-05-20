package com.pacman.customersupport.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for document chunk processing operations used by
 * VectorStoreService, including knowledge base stats generation
 * and document listing.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class DocumentProcessingBenchmark {

    private List<Map<String, String>> documentChunks;

    @Param({"10", "100", "500"})
    private int chunkCount;

    @Setup
    public void setup() {
        documentChunks = new ArrayList<>();
        String[] sources = {
            "return_policy.txt", "shipping_policy.txt", "support_hours.txt",
            "product_catalog.txt", "payment_methods.txt", "account_management.txt",
            "warranty_support.txt"
        };
        for (int i = 0; i < chunkCount; i++) {
            Map<String, String> chunk = new HashMap<>();
            chunk.put("content",
                "Document chunk " + i + ": Our return policy allows returns within 30 days of purchase. " +
                "Item must be unused and in original packaging. " +
                "Refunds are processed within 5-7 business days. " +
                "Please contact support for any questions about returns or exchanges."
            );
            chunk.put("sourceDocument", sources[i % sources.length]);
            chunk.put("embedding", "");
            documentChunks.add(chunk);
        }
    }

    @Benchmark
    public String buildDocumentList() {
        StringBuilder result = new StringBuilder();
        result.append(String.format("Found %d documents in database:\n\n", documentChunks.size()));

        for (int i = 0; i < documentChunks.size(); i++) {
            Map<String, String> chunk = documentChunks.get(i);
            String content = chunk.get("content");
            result.append(String.format("%d. Source: %s\n", i + 1, chunk.get("sourceDocument")));
            result.append(String.format("   Content preview: %s...\n",
                content.length() > 100 ? content.substring(0, 100) : content));
            result.append("\n");
        }

        return result.toString();
    }

    @Benchmark
    public String buildKnowledgeBaseStats() {
        long dbCount = documentChunks.size();
        int searchResults = Math.min(5, documentChunks.size());

        return String.format("Knowledge base status:\n" +
            "- Vector store: Found %d relevant documents\n" +
            "- Database: Contains %d document chunks\n" +
            "- Status: Ready for queries",
            searchResults, dbCount);
    }

    @Benchmark
    public void duplicateDetection(Blackhole bh) {
        String targetContent = documentChunks.get(0).get("content");
        String targetSource = documentChunks.get(0).get("sourceDocument");

        boolean exists = documentChunks.stream()
                .anyMatch(chunk ->
                    chunk.get("content").equals(targetContent) &&
                    chunk.get("sourceDocument").equals(targetSource)
                );
        bh.consume(exists);
    }

    @Benchmark
    public void contentSubstringPreview(Blackhole bh) {
        for (Map<String, String> chunk : documentChunks) {
            String content = chunk.get("content");
            String preview = content.length() > 100 ? content.substring(0, 100) : content;
            bh.consume(preview);
        }
    }
}
