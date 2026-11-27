# Prometheus metrics

Part of `jmix-configure-observability`. Read this when the task asks for Prometheus metrics.

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

```properties
management.endpoints.web.exposure.include=health,prometheus
management.metrics.tags.application=${spring.application.name}
```

The Jmix Grafana dashboards filter by the `application` tag
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

Check:

1. `curl -s localhost:8080/actuator/prometheus | grep -E 'jmix_ui_views_seconds_count|jmix_data_load'`
   lists series with `view_id`, `lifecycle_name` and `application` labels.
2. No `view=` / `lifeCycle=` labels: otherwise the legacy schema is still on.
