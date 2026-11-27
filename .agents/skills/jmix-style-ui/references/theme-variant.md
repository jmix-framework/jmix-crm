# Theme variants

Part of `jmix-style-ui`. Read this when you add a theme variant.

Grep every stylesheet the app declares, not just the Vaadin one. A Jmix app
loads `Aura.STYLESHEET` and `JmixAura.STYLESHEET`, each with its own
`[theme~=...]` rules, so a name missing from one may be styled by the other:
`badge` and `pill` come from the Jmix layer, and a grep of the Vaadin jar alone
reports them as unstyled.

```bash
# the cache usually holds several versions — list them and set the two variables
# to the ones matching the project's Vaadin and Jmix versions
find ~/.gradle/caches \( -name 'vaadin-aura-theme-*.jar' \
  -o -name 'jmix-flowui-themes-*.jar' \) ! -name '*-sources.jar'
JAR=...     # vaadin-aura-theme-<vaadin version>.jar
JMIX=...    # jmix-flowui-themes-<jmix version>.jar

# 1. the Vaadin layer (Aura.STYLESHEET) — one file, values unquoted
unzip -p "$JAR" 'META-INF/resources/aura/aura.css' \
  | grep -ohE "theme~=['\"]?[a-z0-9-]+" | sed -E "s/theme~=['\"]?//" | sort -u

# 2. the Jmix layer (JmixAura.STYLESHEET) — ~120 files, values quoted.
# Extract, then grep: a member glob is not portable, because on Windows unzip a
# '*' does not cross '/', so 'META-INF/resources/themes/jmix-aura/*.css' matches
# only the top-level file and prints nothing. Same reason the whole jar comes
# out, not just the theme folder — it is under 1 MB.
DIR=$(mktemp -d)
unzip -oq "$JMIX" -d "$DIR"
grep -rhoE "theme~=['\"]?[a-z0-9-]+" "$DIR/META-INF/resources/themes/jmix-aura" \
  | sed -E "s/theme~=['\"]?//" | sort -u
```

Under Lumo the same two commands read `META-INF/resources/lumo/lumo.css` in
`vaadin-lumo-theme-*.jar` and the extracted `themes/jmix-lumo` folder from the
same Jmix jar. If grep reports that the folder does not exist, check the version
you picked — Jmix 2.x ships `jmix-lumo` only. A name can also be styled in the
project's own theme CSS — grep that folder too.

Across both layers (Aura 25.1/25.2 with Jmix 3.0/3.1) `primary` and `tertiary`
are styled, but `tertiary-inline` appears in neither — a name a Lumo-era call
site is likely to carry.

Read the selector, not just the name: a hit does not mean the variant works on
your component. In the Jmix layer `contrast` exists only as
`[theme~='badge'][theme~='contrast']` and `icon` only on one specific menu bar,
so on a plain button both still do nothing. (`icon-button` is a different name,
not a match for `icon`.)
