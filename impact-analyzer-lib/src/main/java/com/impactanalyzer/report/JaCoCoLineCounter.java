package com.impactanalyzer.report;

import lombok.extern.slf4j.Slf4j;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Counts source lines executed via the JaCoCo in-process RT agent.
 *
 * The agent API (org.jacoco.agent.rt.*) lives inside jacocoagent.jar — NOT
 * a Maven artifact. We access it via reflection so the project compiles
 * without it on the classpath.
 *
 * To enable:
 *   -javaagent:/path/to/jacocoagent.jar=output=none,dumponexit=false
 *
 * Without the agent the counter gracefully returns 0; everything else works.
 */
@Slf4j
public class JaCoCoLineCounter {

    private Object agentInstance;
    private Method getExecutionDataMethod;

    public JaCoCoLineCounter() {
        try {
            Class<?> rt    = Class.forName("org.jacoco.agent.rt.RT");
            Object   agent = rt.getMethod("getAgent").invoke(null);

            Class<?> iAgent = Class.forName("org.jacoco.agent.rt.IAgent");
            Method   method = iAgent.getMethod("getExecutionData", boolean.class);

            this.agentInstance        = agent;
            this.getExecutionDataMethod = method;
            log.info("[ImpactAnalyzer] JaCoCo RT agent attached — line counting enabled.");
        } catch (Exception e) {
            log.warn("[ImpactAnalyzer] JaCoCo agent not attached — line counts will be 0. " +
                     "Start JVM with: -javaagent:jacocoagent.jar=output=none,dumponexit=false");
        }
    }

    /** Reset execution counters — call before the method under observation. */
    public void reset() {
        if (agentInstance == null) return;
        try { getExecutionDataMethod.invoke(agentInstance, true); }
        catch (Exception ignored) { }
    }

    /**
     * Dump execution data and return covered line count for rootClass.
     * Call after the method under observation completes.
     */
    public int countLinesExecuted(Class<?> rootClass) {
        if (agentInstance == null) return 0;
        try {
            byte[] raw = (byte[]) getExecutionDataMethod.invoke(agentInstance, false);

            ExecFileLoader loader = new ExecFileLoader();
            loader.load(new ByteArrayInputStream(raw));
            ExecutionDataStore dataStore = loader.getExecutionDataStore();

            CoverageBuilder builder  = new CoverageBuilder();
            Analyzer         analyzer = new Analyzer(dataStore, builder);

            String resource = rootClass.getName().replace('.', '/') + ".class";
            try (InputStream is = rootClass.getClassLoader().getResourceAsStream(resource)) {
                if (is != null) analyzer.analyzeClass(is, resource);
            }

            int total = 0;
            for (IClassCoverage cc : builder.getClasses()) {
                total += cc.getLineCounter().getCoveredCount();
            }
            return total;
        } catch (Exception e) {
            log.debug("[ImpactAnalyzer] Line count failed: {}", e.getMessage());
            return 0;
        }
    }
}
