# Method Impact Analyzer — Two-Project Setup

```
impact-analyzer-lib/     ← Spring Boot Starter library (install to local Maven)
demo-microservice/       ← Demo app that imports the library
```

---

## Quick Start (5 minutes)

### Step 1 — Build and install the library locally
```bash
cd impact-analyzer-lib
mvn clean install
```
This puts `impact-analyzer-lib-1.0.0.jar` into your local `~/.m2` repository.

### Step 2 — Run the demo app with the local profile
```bash
cd ../demo-microservice
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Step 3 — Trigger the tracked methods
```bash
# Place an order (hits 3 external APIs + 4 DB operations)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":2,"quantity":1}'

# Get order summary (DB reads + 1 external API)
curl http://localhost:8080/orders/summary/1
```

### Step 4 — View the impact reports
```bash
# List all tracked methods
curl http://localhost:8080/api/impact

# Latest report for placeOrder
curl "http://localhost:8080/api/impact/OrderService.placeOrder()"

# History (last 20 invocations)
curl "http://localhost:8080/api/impact/OrderService.placeOrder()/history"
```

### Step 5 — Open the visual dashboard
Open `impact-analyzer-lib/dashboard/method-impact.html` in your browser.

---

## Environment Behaviour

| Environment | `impact.analyzer.enabled` | What happens |
|-------------|--------------------------|--------------|
| **local**   | `true` (application-local.properties) | All interceptors active, `/api/impact` available |
| **dev/staging/prod** | `false` (default) | Library JAR is present but **zero beans are created** — no overhead |

To run locally: `--spring.profiles.active=local` or `-Dimpact.analyzer.enabled=true`

---

## Adding @TrackImpact to Your Own Services

```java
import com.impactanalyzer.annotation.TrackImpact;

@Service
public class YourService {

    @TrackImpact                         // label defaults to "YourService.yourMethod()"
    public SomeResult yourMethod(...) { ... }

    @TrackImpact(label = "billing-flow") // custom label
    public void processPayment(...) { ... }
}
```

---

## Using the Library in Your Own Project

1. Install: `cd impact-analyzer-lib && mvn install`

2. Add to your `pom.xml`:
```xml
<dependency>
    <groupId>com.impactanalyzer</groupId>
    <artifactId>impact-analyzer-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

3. Add to `application-local.properties`:
```properties
impact.analyzer.enabled=true
```

4. That's it. No `@ComponentScan`, no `@Import`. Spring Boot's auto-configuration handles everything.

---

## Architecture

```
impact-analyzer-lib (JAR)
│
├── ImpactAnalyzerAutoConfiguration
│     @ConditionalOnProperty(impact.analyzer.enabled=true)
│     │
│     ├── MethodImpactAspect          ← wraps @TrackImpact methods
│     ├── SqlStatementInterceptor     ← Hibernate StatementInspector
│     ├── JdbcTemplateInterceptor     ← AOP on JdbcTemplate
│     ├── HttpClientInterceptor       ← AOP on RestTemplate
│     ├── ImpactReportStore           ← in-memory report storage
│     ├── JaCoCoLineCounter           ← line counting (needs agent)
│     └── ImpactReportController      ← /api/impact REST API
│
└── META-INF/spring/
      AutoConfiguration.imports       ← Spring Boot picks this up automatically


demo-microservice
│
├── OrderService   @TrackImpact ← calls UserService, ProductService, 3 external APIs
├── UserService                 ← JPA reads (users table)
├── ProductService              ← JPA reads + writes (products table)
└── application-local.properties:  impact.analyzer.enabled=true
```
