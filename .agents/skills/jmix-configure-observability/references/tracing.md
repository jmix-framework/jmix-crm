# Traces and OTLP export

Part of `jmix-configure-observability`. Read this when the task asks for traces or OTLP export.

Spring Boot 4:

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

Check (set `management.tracing.sampling.probability=1.0` for the check, or most requests
leave no trace):

1. Log lines of the request carry a 32-hex trace ID (`grep -E '[0-9a-f]{32}'` on the
   application log): tracing puts `traceId` and `spanId` into MDC, and Spring Boot prints them
   through `logging.pattern.correlation`. No ID means no tracing bridge on the classpath, or a
   custom Logback pattern without `%X{traceId}`.
2. The spans reached the backend. With the `grafana/otel-lgtm` image, query Tempo through
   the Grafana proxy (data source uid `tempo`; Tempo itself listens on `:3200/api/search`).
   Spans are exported in batches, so wait a few seconds. Pass a time window; the default one
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
