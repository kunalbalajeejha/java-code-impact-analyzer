package com.impactanalyzer.controller;

import com.impactanalyzer.model.ImpactReport;
import com.impactanalyzer.report.ImpactReportStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST API exposing impact reports.
 * Only registered when impact.analyzer.enabled=true.
 *
 * Endpoints:
 *   GET /api/impact                          — all tracked method signatures
 *   GET /api/impact/{signature}              — latest report
 *   GET /api/impact/{signature}/history      — last N reports
 */
@RestController
@RequestMapping("/api/impact")
@CrossOrigin
@RequiredArgsConstructor
public class ImpactReportController {

    private final ImpactReportStore store;

    @GetMapping
    public Set<String> allMethods() {
        return store.allMethods();
    }

    @GetMapping("/{signature}")
    public ResponseEntity<ImpactReport> latest(@PathVariable String signature) {
        ImpactReport r = store.latest(signature);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    @GetMapping("/{signature}/history")
    public List<ImpactReport> history(@PathVariable String signature) {
        return store.history(signature);
    }
}
