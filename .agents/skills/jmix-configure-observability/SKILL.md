---
name: jmix-configure-observability
description: Enable and use Jmix 3 observability - metrics of views, data loaders, actions and DataManager, Prometheus or OTLP export, tracing spans, MDC log context, and custom observations for business operations. Use when adding monitoring, metrics, tracing, Grafana dashboards, or request-scoped log context to a Jmix application.
---

# Configure Observability

Jmix records its operations as Micrometer observations. One observation produces a
timer (metric) and a span (trace). Everything is off by default.

## Enable Jmix observations

```properties
# application.properties
# DataManager load/save
jmix.core.data-observation-enabled=true
# views, fragments, data loaders, actions
jmix.ui.ui-observation-enabled=true
# Jmix 3 tag schema
jmix.ui.legacy-monitoring-enabled=false
```

A project upgraded from Jmix 2 gets `jmix.ui.legacy-monitoring-enabled=true` from
Studio. With it, view and loader metrics carry the old tags (`view`, `lifeCycle`,
`dataLoader`) and fragment and action metrics are not recorded. Set it to `false`
unless an existing Jmix 2 dashboard must keep working.

## Observations

| Observation          | Span name                                    | Main tags                               |
|----------------------|----------------------------------------------|-----------------------------------------|
| `jmix.ui.views`      | `view lifecycle`                             | `view.id`, `lifecycle.name`             |
| `jmix.ui.data`       | `data loader lifecycle`                      | `loader.id`, `view.id`, `lifecycle.name`|
| `jmix.ui.fragments`  | `fragment lifecycle`                         | `fragment.class`, `lifecycle.name`      |
| `jmix.ui.actions`    | `execute action`                             | `action.id`, `view.id`                  |
| `jmix.data.load`     | `load entity`, `load list of entities`, ...  | `entity.name`, `method`                 |
| `jmix.data.save`     | `save entities`                              | `dataStore`                             |

In Prometheus, dots become underscores and the unit is appended:
`jmix.ui.views` → `jmix_ui_views_seconds_count|sum|max`, tag `view.id` → label `view_id`.
Over OTLP the unit is `milliseconds`. Data loading spans also carry `jpql.query` and
`jpql.parameters`; all spans carry `jmix.user.username`
(`jmix.core.use-user-info-for-observation=false` removes it).

## Metrics through Prometheus

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

```properties
management.endpoints.web.exposure.include=health,prometheus
management.metrics.tags.application=${spring.application.name}
```

The `application` tag is what the Jmix Grafana dashboards filter by
(https://github.com/jmix-framework/jmix-grafana-dashboards, "Jmix 3 Monitoring").

The Flow UI security chain covers every URL, so an anonymous `/actuator/prometheus`
request is redirected to the login page (Prometheus reports `unsupported Content-Type
"text/html"`). A scraper without credentials needs its own filter chain:

```java
@Bean
@Order(JmixSecurityFilterChainOrder.CUSTOM)   // io.jmix.core.JmixSecurityFilterChainOrder
SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/actuator/health", "/actuator/prometheus")
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
    return http.build();
}
```

Never permit `/actuator/**` as a whole: `loggers` changes log levels, `env` and `heapdump` expose secrets.

## Traces and OTLP (Spring Boot 4)

```groovy
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
```

```properties
management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
# share of kept traces: default 0.1; 1.0 for development, 0.01-0.1 in production
management.tracing.sampling.probability=1.0
```

- The trace continues into another service only through a client built from the injected
  `RestClient.Builder` / `RestTemplateBuilder`. `RestClient.create()` and
  `new RestTemplate()` drop the `traceparent` header.
- `jpql.parameters` and `jmix.user.username` are personal data in the trace store.

## Custom observation for a business operation

```java
@Autowired
private ObservationRegistry observationRegistry;

public void confirmOrder(Order order) {
    Observation.createNotStarted("sales.order.confirm", observationRegistry)
            .contextualName("confirm order")                                    // span name
            .lowCardinalityKeyValue("order.channel", order.getChannel().getId()) // metric + span
            .highCardinalityKeyValue("order.number", order.getNumber())          // span only
            .observe(() -> orderProcessor.process(order));
}
```

`DataManager` and HTTP spans inside `observe()` become child spans; an exception is
recorded in the span and in the `error` tag of the timer. For a plain counter without a
span, register it through `MeterRegistry`.

## Log context (MDC)

Jmix puts the username into MDC as `jmixUser` (REST requests, `SystemAuthenticator`,
background tasks); tracing adds `traceId` and `spanId`. Spring Boot's default pattern
prints neither user nor custom keys:

```properties
logging.pattern.level=%5p [%X{jmixUser:-}]
```

Custom keys: `try (MDC.MDCCloseable c = MDC.putCloseable("orderNumber", n)) { ... }`.
MDC is per thread: copy it explicitly into `@Async` / executor code
(`MDC.getCopyOfContextMap()` / `MDC.setContextMap()`).

## Forbidden

- Unique values (ids, usernames, order numbers) in `lowCardinalityKeyValue` or common
  tags: every value becomes a separate time series.
- `jmix.ui.legacy-monitoring-enabled=true` in a new Jmix 3 project.
- Percentile histograms for every meter: enable them per meter only where a percentile is
  queried (`management.metrics.distribution.percentiles-histogram.jmix.ui.data=true`).
- Permanent `debug` for `eclipselink.logging.sql` or `io.jmix`: switch it on for an
  investigation through the Actuator `loggers` endpoint.
- `MDC.put` without removing the key: it leaks into the next request on the same thread.

## Production knobs

- Drop unused observations by prefix: `management.observations.enable.spring.security=false`.
- Drop long task timers (`*_active_*` series) if unused:
  `management.metrics.observations.ignored-meters=long-task-timer`.
- With multitenancy, `jmix.user.tenantId` is a tag of every `jmix.*` metric
  (jmix-framework/jmix#5795) and multiplies the series by the number of tenants.
  Unless a per-tenant breakdown is needed, drop it; Spring Boot applies `MeterFilter`
  beans to its registries, and the filter takes the dotted Micrometer key:
  ```java
  @Bean
  MeterFilter ignoreTenantTag() {   // io.micrometer.core.instrument.config.MeterFilter
      return MeterFilter.ignoreTags("jmix.user.tenantId");
  }
  ```

## Verify

Open a view or call the changed code in the running application first: series and spans
appear only after the operation runs.

Metrics:

1. `curl -s localhost:8080/actuator/prometheus | grep -E 'jmix_ui_views_seconds_count|jmix_data_load'`
   lists series with `view_id`, `lifecycle_name` and `application` labels.
2. No `view=` / `lifeCycle=` labels: otherwise the legacy schema is still on.

Traces (set `management.tracing.sampling.probability=1.0` for the check, or most requests
leave no trace):

1. Log lines of the request carry a 32-hex trace ID (`grep -E '[0-9a-f]{32}'` on the
   application log): tracing puts `traceId` and `spanId` into MDC, and Spring Boot prints them
   through `logging.pattern.correlation`. No ID means no tracing bridge on the classpath, or a
   custom Logback pattern without `%X{traceId}`.
2. The spans reached the backend. With the `grafana/otel-lgtm` image, query Tempo through
   the Grafana proxy (data source uid `tempo`; Tempo itself listens on `:3200/api/search`).
   Spans are exported in batches, so wait a few seconds; pass a time window, the default one
   is short:
   ```bash
   curl -s -u admin:admin -G localhost:3000/api/datasources/proxy/uid/tempo/api/search \
     --data-urlencode 'q={ name = "view lifecycle" && span.view.id = "Order.list" }' \
     --data-urlencode "start=$(( $(date +%s) - 900 ))" --data-urlencode "end=$(date +%s)"
   ```
   Empty `traces`: check the exporter errors in the application log and the OTLP endpoint.
3. Propagation between services: the `serviceStats` of a trace in the same response lists
   every service the request passed through. A missing service means its client was not
   built from the injected `RestClient.Builder` / `RestTemplateBuilder`.
4. A custom observation shows up under its `contextualName`: `{ name = "confirm order" }`.

Then run the usual gates (`jmix-ide-static-analysis`, `jmix-verify-bootrun`).

Reference: Jmix docs, Features → Observability (Metrics, Tracing, Logging pages).
