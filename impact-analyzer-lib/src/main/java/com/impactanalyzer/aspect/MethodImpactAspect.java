package com.impactanalyzer.aspect;

import com.impactanalyzer.annotation.TrackImpact;
import com.impactanalyzer.context.MethodContext;
import com.impactanalyzer.model.ImpactReport;
import com.impactanalyzer.report.ImpactReportStore;
import com.impactanalyzer.report.JaCoCoLineCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Root AOP aspect — wraps every @TrackImpact method.
 *
 * Lifecycle per invocation:
 *  1. Reset JaCoCo counters
 *  2. Init ThreadLocal MethodContext
 *  3. Execute the method (interceptors populate the context)
 *  4. Collect context + line count → build ImpactReport → save
 *
 * NOTE: No @Component — declared as a @Bean in ImpactAnalyzerAutoConfiguration
 * so it only exists when impact.analyzer.enabled=true.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class MethodImpactAspect {

    private final ImpactReportStore store;
    private final JaCoCoLineCounter lineCounter;

    @Around("@annotation(trackImpact)")
    public Object track(ProceedingJoinPoint pjp, TrackImpact trackImpact) throws Throwable {

        MethodSignature sig       = (MethodSignature) pjp.getSignature();
        String          signature = sig.getDeclaringType().getSimpleName()
                                    + "." + sig.getName() + "()";
        String          label     = trackImpact.label().isBlank() ? signature : trackImpact.label();

        lineCounter.reset();
        MethodContext.init();

        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long          execMs = System.currentTimeMillis() - start;
            int           lines  = lineCounter.countLinesExecuted(sig.getDeclaringType());
            MethodContext ctx    = MethodContext.collectAndClear();

            if (ctx != null) {
                ImpactReport report = ImpactReport.from(
                        signature, label, lines, execMs,
                        ctx.getDbOps(), ctx.getApiCalls(), ctx.getCallStack());
                store.save(report);
                log.info("[ImpactAnalyzer] {} — {}ms | {} lines | {} DB ops | {} API calls",
                        signature, execMs, lines, ctx.getDbOps().size(), ctx.getApiCalls().size());
            }
        }
    }
}
