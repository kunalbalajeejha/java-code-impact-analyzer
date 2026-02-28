package com.impactanalyzer.interceptor;

import com.impactanalyzer.context.MethodContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpMethod;

import java.net.URI;

/**
 * AOP interceptor for outbound RestTemplate HTTP calls.
 *
 * Only RestTemplate is intercepted here because AspectJ validates all type
 * names in pointcut expressions at startup — referencing OkHttp / Feign /
 * WebClient types when they are not on the classpath causes a hard crash.
 *
 * See ImpactAnalyzerAutoConfiguration for extension points (WebClient filter,
 * Feign RequestInterceptor).
 *
 * NOTE: No @Component — registered via ImpactAnalyzerAutoConfiguration only
 * when impact.analyzer.enabled=true.
 */
@Aspect
@Slf4j
public class HttpClientInterceptor {

    @Around("execution(* org.springframework.web.client.RestTemplate.exchange(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.getForObject(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.getForEntity(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.postForObject(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.postForEntity(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.put(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.delete(..))" +
            " || execution(* org.springframework.web.client.RestTemplate.patchForObject(..))")
    public Object interceptRestTemplate(ProceedingJoinPoint pjp) throws Throwable {
        if (!MethodContext.isActive()) return pjp.proceed();

        Object[] args   = pjp.getArgs();
        String   url    = resolveUrl(args);
        String   method = resolveMethod(pjp.getSignature().getName(), args);

        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long ms = System.currentTimeMillis() - start;
            MethodContext.current().recordApiCall(url, method, ms);
            log.debug("[ImpactAnalyzer] {} {} → {}ms", method, url, ms);
        }
    }

    private String resolveUrl(Object[] args) {
        if (args == null || args.length == 0) return "unknown";
        Object first = args[0];
        if (first instanceof String s) return s;
        if (first instanceof URI u)    return u.toString();
        return first == null ? "unknown" : first.toString();
    }

    private String resolveMethod(String methodName, Object[] args) {
        for (Object a : args) {
            if (a instanceof HttpMethod hm) return hm.name();
        }
        return switch (methodName) {
            case "getForObject", "getForEntity"   -> "GET";
            case "postForObject", "postForEntity" -> "POST";
            case "put"                            -> "PUT";
            case "delete"                         -> "DELETE";
            case "patchForObject"                 -> "PATCH";
            default                               -> "UNKNOWN";
        };
    }
}
