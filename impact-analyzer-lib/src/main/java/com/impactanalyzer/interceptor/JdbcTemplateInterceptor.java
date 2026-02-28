package com.impactanalyzer.interceptor;

import com.impactanalyzer.context.MethodContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * AOP interceptor for Spring's JdbcTemplate / NamedParameterJdbcTemplate.
 *
 * Covers apps that use JdbcTemplate directly (not JPA).
 * JPA apps are covered by SqlStatementInterceptor (Hibernate StatementInspector).
 *
 * NOTE: No @Component here — registered as a @Bean in ImpactAnalyzerAutoConfiguration
 * so it is ONLY created when impact.analyzer.enabled=true.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class JdbcTemplateInterceptor {

    private final SqlStatementInterceptor sqlInterceptor;

    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.query*(String, ..))" +
            " || execution(* org.springframework.jdbc.core.JdbcTemplate.update(String, ..))" +
            " || execution(* org.springframework.jdbc.core.JdbcTemplate.execute(String, ..))" +
            " || execution(* org.springframework.jdbc.core.JdbcTemplate.batchUpdate(String, ..))" +
            " || execution(* org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.query*(String, ..))" +
            " || execution(* org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.update(String, ..))")
    public Object interceptJdbc(ProceedingJoinPoint pjp) throws Throwable {
        if (!MethodContext.isActive()) return pjp.proceed();

        Object[] args = pjp.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof String sql) {
            sqlInterceptor.record(sql);
        }
        return pjp.proceed();
    }
}
