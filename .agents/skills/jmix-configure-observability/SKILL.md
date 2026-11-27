---
name: jmix-configure-observability
description: Use when adding monitoring, metrics, tracing, Prometheus or OTLP export, Grafana dashboards, MDC log context, or custom observations to a Jmix 3 application.
---

# Configure Observability

Jmix records its operations as Micrometer observations. One observation produces a
timer (metric) and a span (trace). Everything is off by default.

## Steps

1. Enable Jmix observations:
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
   `dataLoader`), and fragment and action metrics are not recorded. Set it to `false`
   unless an existing Jmix 2 dashboard must keep working.
2. Add what the task asks for:
   - Metrics for Prometheus or the Jmix Grafana dashboards: first read `references/prometheus.md` in this skill's folder.
   - Traces or OTLP export: first read `references/tracing.md` in this skill's folder.
   - Metrics or a span for a business operation: first read `references/custom-observation.md` in this skill's folder.
   - The user or custom keys in log lines: first read `references/mdc.md` in this skill's folder.
   - Settings for production: read **When the task asks for production settings** below.
   - If the project uses multitenancy and exports metrics, read **When the project uses multitenancy** below.
3. Verify. Open a view or call the changed code in the running application first:
   series and spans appear only after the operation runs. Then run the check of each
   special case you used, and the usual gates (`jmix-ide-static-analysis`,
   `jmix-verify-bootrun`).

## Observations

| Observation          | Span name                                    | Main tags                               |
|----------------------|----------------------------------------------|-----------------------------------------|
| `jmix.ui.views`      | `view lifecycle`                             | `view.id`, `lifecycle.name`             |
| `jmix.ui.data`       | `data loader lifecycle`                      | `loader.id`, `view.id`, `lifecycle.name`|
| `jmix.ui.fragments`  | `fragment lifecycle`                         | `fragment.class`, `lifecycle.name`      |
| `jmix.ui.actions`    | `execute action`                             | `action.id`, `view.id`                  |
| `jmix.data.load`     | `load entity`, `load list of entities`, ...  | `entity.name`, `method`                 |
| `jmix.data.save`     | `save entities`                              | `dataStore`                             |

- In Prometheus, dots become underscores and the unit is appended:
  `jmix.ui.views` → `jmix_ui_views_seconds_count|sum|max`, tag `view.id` → label `view_id`.
- Over OTLP the unit is `milliseconds`.
- Data loading spans also carry `jpql.query` and `jpql.parameters`; all spans carry
  `jmix.user.username` (`jmix.core.use-user-info-for-observation=false` removes it).

Reference: Jmix docs, Features → Observability (Metrics, Tracing, Logging pages).

## Forbidden

- `jmix.ui.legacy-monitoring-enabled=true` in a new Jmix 3 project.
- Unique values (ids, usernames, order numbers) in `lowCardinalityKeyValue` or common
  tags: every value becomes a separate time series.
- Percentile histograms for every meter: enable them per meter only where a percentile is
  queried (`management.metrics.distribution.percentiles-histogram.jmix.ui.data=true`).
- Permanent `debug` for `eclipselink.logging.sql` or `io.jmix`: switch it on for an
  investigation through the Actuator `loggers` endpoint.
- `MDC.put` without removing the key: it leaks into the next request on the same thread.

## Special cases

### When the task asks for production settings

- Drop unused observations by prefix: `management.observations.enable.spring.security=false`.
- Drop long task timers (`*_active_*` series) if unused:
  `management.metrics.observations.ignored-meters=long-task-timer`.

### When the project uses multitenancy

`jmix.user.tenantId` is a tag of every `jmix.*` metric (jmix-framework/jmix#5795) and
multiplies the series by the number of tenants. Unless a per-tenant breakdown is needed,
drop it. Spring Boot applies `MeterFilter` beans to its registries, and the filter takes
the dotted Micrometer key:

```java
@Bean
MeterFilter ignoreTenantTag() {   // io.micrometer.core.instrument.config.MeterFilter
    return MeterFilter.ignoreTags("jmix.user.tenantId");
}
```
