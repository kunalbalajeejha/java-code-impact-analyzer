package com.impactanalyzer.config;

import com.impactanalyzer.aspect.MethodImpactAspect;
import com.impactanalyzer.controller.ImpactReportController;
import com.impactanalyzer.interceptor.HttpClientInterceptor;
import com.impactanalyzer.interceptor.JdbcTemplateInterceptor;
import com.impactanalyzer.interceptor.SqlStatementInterceptor;
import com.impactanalyzer.report.ImpactReportStore;
import com.impactanalyzer.report.JaCoCoLineCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                  Impact Analyzer Auto-Configuration                      │
 * │                                                                         │
 * │  Activated ONLY when:  impact.analyzer.enabled=true                     │
 * │                                                                         │
 * │  Set this in application-local.properties (never in prod/staging).      │
 * │                                                                         │
 * │  What gets wired:                                                       │
 * │    • MethodImpactAspect     — wraps @TrackImpact methods                │
 * │    • SqlStatementInterceptor — intercepts Hibernate SQL                 │
 * │    • JdbcTemplateInterceptor — intercepts JdbcTemplate SQL              │
 * │    • HttpClientInterceptor  — intercepts RestTemplate calls             │
 * │    • ImpactReportController — serves reports at /api/impact             │
 * │                                                                         │
 * │  Hibernate wiring:                                                      │
 * │    The SqlStatementInterceptor is registered as a Hibernate             │
 * │    StatementInspector via HibernatePropertiesCustomizer.                │
 * │    This bean is only created when Hibernate is on the classpath         │
 * │    (@ConditionalOnClass(StatementInspector.class)).                     │
 * │                                                                         │
 * │  Extension points (add to your app's @Configuration when needed):      │
 * │                                                                         │
 * │  // WebClient:                                                          │
 * │  @Bean ExchangeFilterFunction impactWebClientFilter(                    │
 * │         SqlStatementInterceptor si) {                                   │
 * │      return (req, next) -> {                                            │
 * │          long t = System.currentTimeMillis();                           │
 * │          return next.exchange(req).doOnNext(r ->                        │
 * │              MethodContext.current().recordApiCall(                     │
 * │                  req.url().toString(), req.method().name(),             │
 * │                  System.currentTimeMillis() - t));                      │
 * │      };                                                                 │
 * │  }                                                                      │
 * │                                                                         │
 * │  // Feign:                                                              │
 * │  @Bean RequestInterceptor impactFeignInterceptor() {                    │
 * │      return t -> MethodContext.current()                                │
 * │          .recordApiCall(t.url(), t.method(), 0);                       │
 * │  }                                                                      │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "impact.analyzer",
        name   = "enabled",
        havingValue = "true",
        matchIfMissing = false      // ← OFF by default in all environments
)
@EnableConfigurationProperties(ImpactAnalyzerProperties.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Slf4j
public class ImpactAnalyzerAutoConfiguration {

    public ImpactAnalyzerAutoConfiguration() {
        log.warn("╔══════════════════════════════════════════════════════════╗");
        log.warn("║  Impact Analyzer is ACTIVE — for local development only  ║");
        log.warn("║  Dashboard → GET /api/impact                             ║");
        log.warn("╚══════════════════════════════════════════════════════════╝");
    }

    @Bean
    @ConditionalOnMissingBean
    public ImpactReportStore impactReportStore(ImpactAnalyzerProperties props) {
        return new ImpactReportStore(props.getMaxReportsPerMethod());
    }

    @Bean
    @ConditionalOnMissingBean
    public JaCoCoLineCounter jaCoCoLineCounter() {
        return new JaCoCoLineCounter();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlStatementInterceptor sqlStatementInterceptor() {
        return new SqlStatementInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplateInterceptor jdbcTemplateInterceptor(SqlStatementInterceptor sqlInterceptor) {
        return new JdbcTemplateInterceptor(sqlInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpClientInterceptor httpClientInterceptor() {
        return new HttpClientInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodImpactAspect methodImpactAspect(ImpactReportStore store,
                                                  JaCoCoLineCounter lineCounter) {
        return new MethodImpactAspect(store, lineCounter);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImpactReportController impactReportController(ImpactReportStore store) {
        return new ImpactReportController(store);
    }

    /**
     * Registers SqlStatementInterceptor as a Hibernate StatementInspector.
     * Only wired when Hibernate is present (i.e. spring-boot-starter-data-jpa is used).
     */
    @Bean
    @ConditionalOnClass(name = "org.hibernate.resource.jdbc.spi.StatementInspector")
    public org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
    hibernateImpactCustomizer(SqlStatementInterceptor sqlInterceptor) {
        return props -> props.put(
                "hibernate.session_factory.statement_inspector", sqlInterceptor);
    }
}
