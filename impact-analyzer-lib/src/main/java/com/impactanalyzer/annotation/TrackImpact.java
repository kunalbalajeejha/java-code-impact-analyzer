package com.impactanalyzer.annotation;

import java.lang.annotation.*;

/**
 * Place on any Spring-managed method to track its full execution impact:
 *
 *   - Lines executed (requires JaCoCo agent at JVM startup)
 *   - DB tables read / written (via JPA StatementInspector + JdbcTemplate AOP)
 *   - External API calls made (via RestTemplate AOP)
 *
 * Only active when: impact.analyzer.enabled=true
 * Intended for local development only — set that property in application-local.properties.
 *
 * Example:
 *   @TrackImpact
 *   public OrderResponse placeOrder(OrderRequest req) { ... }
 *
 *   @TrackImpact(label = "checkout-flow")
 *   public void checkout() { ... }
 *
 * View results at: GET /api/impact
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackImpact {
    /** Optional label. Defaults to ClassName.methodName(). */
    String label() default "";
}
