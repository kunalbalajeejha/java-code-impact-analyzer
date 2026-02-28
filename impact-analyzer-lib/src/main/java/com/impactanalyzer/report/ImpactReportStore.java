package com.impactanalyzer.report;

import com.impactanalyzer.model.ImpactReport;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store. Keeps the latest N reports per method.
 * Replace with a persistent store (MongoDB / Postgres) for team-shared history.
 */
public class ImpactReportStore {

    private final int maxPerMethod;
    private final Map<String, Deque<ImpactReport>> store = new ConcurrentHashMap<>();

    public ImpactReportStore(int maxPerMethod) {
        this.maxPerMethod = maxPerMethod;
    }

    public void save(ImpactReport report) {
        store.compute(report.getMethodSignature(), (key, q) -> {
            if (q == null) q = new ArrayDeque<>();
            q.addFirst(report);
            while (q.size() > maxPerMethod) q.removeLast();
            return q;
        });
    }

    public ImpactReport latest(String sig) {
        Deque<ImpactReport> q = store.get(sig);
        return q == null || q.isEmpty() ? null : q.peekFirst();
    }

    public List<ImpactReport> history(String sig) {
        Deque<ImpactReport> q = store.get(sig);
        return q == null ? List.of() : List.copyOf(q);
    }

    public Set<String> allMethods() {
        return Collections.unmodifiableSet(store.keySet());
    }
}
