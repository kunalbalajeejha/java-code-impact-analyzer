package com.impactanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Impact Analyzer library.
 *
 * In application-local.properties:
 *   impact.analyzer.enabled=true
 *   impact.analyzer.base-packages=com.yourcompany   # optional: filter call-stack frames
 *   impact.analyzer.max-reports-per-method=20        # optional: history depth
 */
@ConfigurationProperties(prefix = "impact.analyzer")
public class ImpactAnalyzerProperties {

    /** Master switch. Set true ONLY in local/dev profile. */
    private boolean enabled = false;

    /**
     * Comma-separated base packages used to filter call-stack frames
     * in the call tree — only frames matching these prefixes are shown.
     * Defaults to "com." to exclude JDK / framework internals.
     */
    private String basePackages = "com.";

    /** Max number of reports stored per method. Older reports are evicted. */
    private int maxReportsPerMethod = 20;

    // ── Getters / Setters ──────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBasePackages() { return basePackages; }
    public void setBasePackages(String basePackages) { this.basePackages = basePackages; }

    public int getMaxReportsPerMethod() { return maxReportsPerMethod; }
    public void setMaxReportsPerMethod(int maxReportsPerMethod) {
        this.maxReportsPerMethod = maxReportsPerMethod;
    }
}
