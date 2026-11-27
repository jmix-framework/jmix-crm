# Application with a non-English locale

Part of `jmix-add-i18n-keys`. Read this when the application has a non-English locale.

Application message bundles translate only application-owned text. Add the
official translation module for each supported non-English locale to
`build.gradle`, without a version (the Jmix BOM resolves it):

```groovy
implementation 'io.jmix.translations:jmix-translations-<locale>'
```

Existing `<locale>` codes: `ar` (Arabic), `ckb` (Central Kurdish), `cs` (Czech),
`da` (Danish), `de` (German), `el` (Greek), `es` (Spanish), `fr` (French), `it`
(Italian), `nl` (Dutch), `pt-br` (Brazilian Portuguese), `ro` (Romanian), `ru`
(Russian), `tr` (Turkish), `zh-cn` (Simplified Chinese), and `zh-tw`
(Traditional Chinese). Add only the dependencies for the application's supported
locales.

- Add the dependency before creating files such as
  `src/main/resources/io/jmix/core/messages_<lang>.properties` or message bundles
  inside add-on packages.
- Official translations contain most Jmix framework and supported add-on
  messages, but some can be missing. Add local overrides only for missing
  messages or intentional wording changes. Do not copy complete bundles; they
  become outdated after a Jmix upgrade.
- Run the application in the target locale and inspect framework-owned views,
  filter dialogs, and add-on views. Also inspect a Boolean value rendered by Jmix:
  English `True` and `False` captions show that the framework translation is not
  active or does not contain those keys.
