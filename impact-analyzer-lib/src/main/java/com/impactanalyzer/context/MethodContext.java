package com.impactanalyzer.context;

import com.impactanalyzer.model.ApiCallRecord;
import com.impactanalyzer.model.DbOpRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-invocation ThreadLocal accumulator.
 * Populated by SQL and HTTP interceptors during a @TrackImpact method call.
 *
 * NOTE: Does not propagate across @Async / reactive boundaries.
 * For async code, propagate context manually or via MDC.
 */
public class MethodContext {

    private static final ThreadLocal<MethodContext> CTX = new ThreadLocal<>();

    private final List<DbOpRecord>  dbOps    = new ArrayList<>();
    private final List<ApiCallRecord> apiCalls = new ArrayList<>();
    private final List<String>      callStack = new ArrayList<>();
    private int linesExecuted = 0;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    public static void init()   { CTX.set(new MethodContext()); }
    public static boolean isActive() { return CTX.get() != null; }
    public static MethodContext current() { return CTX.get(); }

    public static MethodContext collectAndClear() {
        MethodContext ctx = CTX.get();
        CTX.remove();
        return ctx;
    }

    // ── Recording ──────────────────────────────────────────────────────────

    public void recordDbOp(String operation, String table, String sql) {
        String frame = callerFrame();
        dbOps.add(new DbOpRecord(operation, table, sql, frame));
        callStack.add(String.format("[DB] %s %s  ← %s", operation, table, frame));
    }

    public void recordApiCall(String url, String method, long durationMs) {
        String frame = callerFrame();
        apiCalls.add(new ApiCallRecord(url, method, durationMs, frame));
        callStack.add(String.format("[API] %s %s ~%dms  ← %s", method, url, durationMs, frame));
    }

    public void setLinesExecuted(int n) { this.linesExecuted = n; }

    // ── Accessors ──────────────────────────────────────────────────────────

    public int getLinesExecuted()          { return linesExecuted; }
    public List<DbOpRecord>   getDbOps()   { return dbOps; }
    public List<ApiCallRecord> getApiCalls(){ return apiCalls; }
    public List<String>       getCallStack(){ return callStack; }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String callerFrame() {
        for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
            String cls = el.getClassName();
            if (cls.startsWith("com.")
                    && !cls.startsWith("com.impactanalyzer")
                    && !cls.contains("$$")
                    && !cls.contains("CGLIB")
                    && !cls.contains("SpringProxy")) {
                return el.getClassName() + "." + el.getMethodName() + ":" + el.getLineNumber();
            }
        }
        return "unknown";
    }
}
