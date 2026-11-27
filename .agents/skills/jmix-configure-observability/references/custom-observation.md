# Custom observation of a business operation

Part of `jmix-configure-observability`. Read this when the task asks for a custom observation of a business operation.

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

- `DataManager` and HTTP spans inside `observe()` become child spans; an exception is
  recorded in the span and in the `error` tag of the timer.
- For a plain counter without a span, register it through `MeterRegistry`.

Check: in the trace backend, the observation shows up under its `contextualName`:
`{ name = "confirm order" }`.
