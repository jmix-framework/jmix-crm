# Text field with a live `valueChangeMode`

Part of `jmix-create-detail-view`. Read this when a text field has a live `valueChangeMode`.

For free-text fields with `EAGER`, `LAZY`, or `TIMEOUT` value updates, set `trimEnabled="false"`. Otherwise a client-originated update can trim the trailing space or newline and rewrite the text before the user types the next word:

```xml
<textArea id="descriptionField" property="description"
          valueChangeMode="TIMEOUT" trimEnabled="false"/>
```

If normalization is required, do it at an explicit commit or validation boundary. Test a client-originated value ending in a space and a newline (or real typing), not only server-side `setValue`, which bypasses this trimming.
