package com.impactanalyzer.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
public class ImpactReport {

    private String methodSignature;
    private String label;
    private Instant capturedAt;
    private long executionTimeMs;
    private int linesExecuted;

    private List<DbOpRecord>    dbOps;
    private List<TableSummary>  tablesRead;
    private List<TableSummary>  tablesWritten;
    private List<ApiCallRecord> apiCalls;
    private List<String>        callStack;

    // ── Factory ────────────────────────────────────────────────────────────

    public static ImpactReport from(String signature, String label,
                                    int lines, long execMs,
                                    List<DbOpRecord> dbOps,
                                    List<ApiCallRecord> apiCalls,
                                    List<String> callStack) {

        Map<String, List<DbOpRecord>> byOp = dbOps.stream()
                .collect(Collectors.groupingBy(DbOpRecord::getOperation));

        List<TableSummary> reads = summarize(byOp.getOrDefault("SELECT", List.of()));
        List<TableSummary> writes = summarize(
                Stream.of(
                    byOp.getOrDefault("INSERT", List.of()),
                    byOp.getOrDefault("UPDATE", List.of()),
                    byOp.getOrDefault("DELETE", List.of())
                ).flatMap(List::stream).collect(Collectors.toList())
        );

        return ImpactReport.builder()
                .methodSignature(signature)
                .label(label == null || label.isBlank() ? signature : label)
                .capturedAt(Instant.now())
                .executionTimeMs(execMs)
                .linesExecuted(lines)
                .dbOps(dbOps)
                .tablesRead(reads)
                .tablesWritten(writes)
                .apiCalls(apiCalls)
                .callStack(callStack)
                .build();
    }

    private static List<TableSummary> summarize(List<DbOpRecord> ops) {
        return ops.stream()
                .collect(Collectors.groupingBy(DbOpRecord::getTable))
                .entrySet().stream()
                .map(e -> new TableSummary(
                        e.getKey(),
                        e.getValue().get(0).getOperation(),
                        e.getValue().size()))
                .collect(Collectors.toList());
    }

    // ── Inner types ────────────────────────────────────────────────────────

    @Data
    @lombok.AllArgsConstructor
    public static class TableSummary {
        private String table;
        private String operation;
        private int    count;
    }
}
