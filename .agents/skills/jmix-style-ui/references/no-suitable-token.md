# No suitable token in the active theme

Part of `jmix-style-ui`. Read this when the active theme has no suitable token.

Four acceptable options, in this order:

1. **A verified `--lumo-*` token** from the active theme.
2. **A verified `--vaadin-*` token** when you specifically need a lower-level
   Vaadin component hook and the active theme defines it.
3. **Define your own custom property or class in the project theme stylesheet**
   and use it — the value lives in one place.
4. **Use a literal, theme-neutral value** for a neutral decoration:

```java
card.getStyle()
        .set("border", "1px solid rgba(128, 128, 128, 0.3)")
        .set("border-radius", "6px");
```

A token from another theme (`--aura-*`) is never an option: it is undefined in
Jmix 2, and no gate can catch it.
