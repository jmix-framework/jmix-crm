# No suitable token in the active theme

Part of `jmix-style-ui`. Read this when the active theme has no suitable token.

Three acceptable options, in this order:

1. **A `--vaadin-*` token from the base-styles layer** — it usually has what
   the theme's own family lacks; enumerate and verify it the same way.
2. **Define your own custom property or class in the project theme stylesheet**
   and use it — the value lives in one place and survives a theme change.
3. **Use a literal, theme-neutral value** for a neutral decoration:

```java
card.getStyle()
        .set("border", "1px solid rgba(128, 128, 128, 0.3)")
        .set("border-radius", "6px");
```

Borrowing a token from the other theme is never an option: no gate can catch it.
