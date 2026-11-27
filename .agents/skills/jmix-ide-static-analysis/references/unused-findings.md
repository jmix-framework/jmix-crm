# Unused and can-be-converted findings

Part of `jmix-ide-static-analysis`. Read this when the inspection reports an unused or can-be-converted finding.

These findings are expected noise. Say that you understand them and move on:

| Finding | Why it is not a defect |
|---|---|
| "Method is never used" on `@ConfigurationProperties` getters/setters | called by Spring through reflection |
| "Method is never used" on entity getters/setters | called by Jmix/JPA through reflection and by view descriptors |
| "Unused property" on an i18n key | normal when the view that references it does not exist yet (e.g. a detail-view title key added while building the list view) |
| "Field can be converted to a local variable" in a test class | fields hold `@Autowired`/fixture state across methods |
| "Result of `DataManager.save()` is not used" in a test that keeps the entity for cleanup | a false positive only if the returned instance is used (e.g. added to a cleanup list); if the result is really discarded, it is a real finding: use `saveWithoutReload()` |
