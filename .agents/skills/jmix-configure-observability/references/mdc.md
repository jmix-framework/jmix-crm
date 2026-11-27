# Log context (MDC)

Part of `jmix-configure-observability`. Read this when the task asks for log context (MDC).

Jmix puts the username into MDC as `jmixUser` (REST requests, `SystemAuthenticator`,
background tasks); tracing adds `traceId` and `spanId`. Spring Boot's default pattern
prints neither the user nor custom keys:

```properties
logging.pattern.level=%5p [%X{jmixUser:-}]
```

- Custom keys: `try (MDC.MDCCloseable c = MDC.putCloseable("orderNumber", n)) { ... }`.
- MDC is per thread: copy it explicitly into `@Async` / executor code
  (`MDC.getCopyOfContextMap()` / `MDC.setContextMap()`).
