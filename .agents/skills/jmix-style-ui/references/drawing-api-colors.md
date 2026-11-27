# Colors passed to a drawing API

Part of `jmix-style-ui`. Read this when a component passes colors to a drawing API.

A JavaScript drawing API does not interpret `var(...)` as CSS. Before passing
a theme token to a component option, check whether the component resolves CSS
custom properties itself. If it does not, use the component's palette/theme
API, a literal color, or resolve the property first (for example with
`getComputedStyle(...).getPropertyValue(...)`) and pass the resulting color.

Such a value may have no computed style to inspect, so the check in **Verify** of `SKILL.md`
does not apply: verify the rendered result and, when necessary, read the
component's frontend implementation to confirm whether it resolves CSS custom
properties.
