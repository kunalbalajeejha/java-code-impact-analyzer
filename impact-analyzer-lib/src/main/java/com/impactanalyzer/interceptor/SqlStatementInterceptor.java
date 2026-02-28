package com.impactanalyzer.interceptor;

import com.impactanalyzer.context.MethodContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Hibernate StatementInspector that fires for every SQL Hibernate executes.
 *
 * Registered via HibernatePropertiesCustomizer in ImpactAnalyzerAutoConfiguration.
 * Only records to MethodContext when a @TrackImpact call is active on this thread.
 *
 * Also used by JdbcTemplateInterceptor for non-JPA SQL.
 */
@Slf4j
public class SqlStatementInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        if (sql != null && MethodContext.isActive()) {
            extractAndRecord(sql);
        }
        return sql;   // MUST return the sql string (may be modified if needed)
    }

    /** Called directly by JdbcTemplateInterceptor for non-JPA queries. */
    public void record(String sql) {
        if (sql != null && MethodContext.isActive()) {
            extractAndRecord(sql);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void extractAndRecord(String sql) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql.trim());
            String op;
            String table;

            if (stmt instanceof Select) {
                op    = "SELECT";
                table = tableAfterFrom(sql);
            } else if (stmt instanceof Insert i) {
                op    = "INSERT";
                table = i.getTable().getName().toUpperCase();
            } else if (stmt instanceof Update u) {
                op    = "UPDATE";
                table = u.getTable().getName().toUpperCase();
            } else if (stmt instanceof Delete d) {
                op    = "DELETE";
                table = d.getTable().getName().toUpperCase();
            } else {
                return;
            }

            MethodContext.current().recordDbOp(op, table, abbrev(sql, 200));

        } catch (Exception e) {
            log.trace("[ImpactAnalyzer] Could not parse SQL: {}", e.getMessage());
        }
    }

    private String tableAfterFrom(String sql) {
        String upper = sql.toUpperCase();
        int idx = upper.indexOf(" FROM ");
        if (idx == -1) return "UNKNOWN";
        String rest = sql.substring(idx + 6).trim();
        return rest.split("[ ,\n\r\t(]")[0].toUpperCase();
    }

    private String abbrev(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
