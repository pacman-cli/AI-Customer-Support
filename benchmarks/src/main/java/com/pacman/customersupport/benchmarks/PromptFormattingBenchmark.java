package com.pacman.customersupport.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Benchmarks for prompt formatting and context assembly operations
 * used by AICustomerSupportService.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PromptFormattingBenchmark {

    private List<String> documentContents;
    private String question;

    @Param({"3", "10", "50"})
    private int documentCount;

    @Setup
    public void setup() {
        documentContents = new ArrayList<>();
        for (int i = 0; i < documentCount; i++) {
            documentContents.add(
                "Document " + i + ": Our return policy allows returns within 30 days of purchase. " +
                "Item must be unused and in original packaging. " +
                "Refunds are processed within 5-7 business days."
            );
        }
        question = "What is your return policy?";
    }

    @Benchmark
    public String buildPromptWithStreamJoining() {
        String context = documentContents.stream()
                .collect(Collectors.joining("\n---\n"));

        return """
                You are a friendly customer support agent.
                    Use the following context to answer the question.
                    If you don't know, say "I don't know based on provided info."

                    CONTEXT:
                    %s

                    QUESTION:
                    %s

                    ANSWER:
                    """.formatted(context, question);
    }

    @Benchmark
    public String buildPromptWithStringBuilder() {
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < documentContents.size(); i++) {
            if (i > 0) {
                contextBuilder.append("\n---\n");
            }
            contextBuilder.append(documentContents.get(i));
        }

        return """
                You are a friendly customer support agent.
                    Use the following context to answer the question.
                    If you don't know, say "I don't know based on provided info."

                    CONTEXT:
                    %s

                    QUESTION:
                    %s

                    ANSWER:
                    """.formatted(contextBuilder.toString(), question);
    }

    @Benchmark
    public void contextJoiningWithStreams(Blackhole bh) {
        String context = documentContents.stream()
                .collect(Collectors.joining("\n---\n"));
        bh.consume(context);
    }

    @Benchmark
    public void contextJoiningWithStringBuilder(Blackhole bh) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documentContents.size(); i++) {
            if (i > 0) {
                sb.append("\n---\n");
            }
            sb.append(documentContents.get(i));
        }
        bh.consume(sb.toString());
    }
}
