---
name: jmix-add-i18n-keys
description: Add complete Jmix message keys for entities, enums, views, actions, and validation text, and configure official Jmix framework translations for non-English applications.
---

# Add I18n Keys

Use this skill whenever adding user-visible text, entities, enum values, views,
validation messages, or a non-English application locale.

## Steps

1. If the application has a non-English locale, first read `references/non-english-locale.md` in this skill's folder; add the official translation dependency before
   writing any framework or add-on message overrides. If the task adds a
   locale, or a supported locale is missing from
   `jmix.core.available-locales`, read **When the task adds a locale** below.
2. Find all locale files in the application message bundle.
3. Add the same application-owned keys to every locale file.
4. Use entity keys for entity captions and attributes, and enum keys for every
   enum constant.
5. Use view-local keys for titles, button text, labels, and dialog text.
6. Use `msg://` in XML descriptors. Use a full `msg://<message-group>/<key>`
   reference when the key belongs to another message group.
7. Use `MessageBundle` in view controllers and `Messages` in services/beans.
8. Before finishing, run both passes of the **Exact reference audit** below.

## Key patterns

```properties
com.company.app.entity/Customer=Customer
com.company.app.entity/Customer.name=Name

com.company.app.entity/OrderStatus=Order status
com.company.app.entity/OrderStatus.NEW=New

customerListView.title=Customers
customerDetailView.title=Customer
createOrderButton.text=Create order
```

Follow the key style of the project's existing views: a generated project may use
`UserListView.title` where this example uses `customerListView.title`. Mixing the
two renders raw keys as titles.

## XML usage

A message reference has two forms:

- Brief: `msg://<key>`.
- Full: `msg://<message-group>/<key>`.

The message group is the Java package-style group before `/` in a properties key.
Entity messages often use the entity package group
(`com.company.app.entity/Customer.name`). View-local messages usually use the view
package group (`com.company.app.view.customer/customerListView.title`).

A brief reference resolves against the message group of the current XML
descriptor, so use it only for keys stored next to that descriptor:

```xml
<view title="msg://customerListView.title">
    <button id="createOrderButton" text="msg://createOrderButton.text"/>
</view>
```

Use a full reference when the key is in another group: entity captions, menu keys,
shared application keys, or text used from a descriptor whose package does not
match the key group:

```xml
<item view="Customer.list" title="msg://com.company.app.view.customer/customerListView.title"/>
<h4 text="msg://com.company.app.entity/Customer.orders"/>
```

For Bean Validation messages, put the same full reference inside braces:

```java
@NotNull(message = "{msg://com.company.app.entity/Customer.email.required}")
private String email;
```

## Java usage in views

```java
@ViewComponent
private MessageBundle messageBundle;

String text = messageBundle.getMessage("createOrderButton.text");
```

For localized entity and attribute captions (not bundle-key formatting), inject
`io.jmix.core.MessageTools` and `Metadata`:
`messageTools.getEntityCaption(metadata.getClass(Customer.class))` and
`messageTools.getPropertyCaption(metadata.getClass(Customer.class), "name")`.

## Exact reference audit

### 1. Verify references

Search changed XML and Java for message references and check that each key exists
in the correct bundle with identical casing. Key lookup is case-sensitive.

```xml
<button id="createOrderButton" text="msg://createOrderButton.text"/>
```

```properties
createOrderButton.text=Create order
```

`CreateOrderButton.text` or `createorderButton.text` does not match.

### 2. Verify translated values

Review every locale bundle other than the one whose language the keys are written
in for values left from the template. This includes an `en,<other>` application:
its second bundle is generated as an English template, and every key in it
resolves, so pass 1 does not find these values.

Read each such `messages_<locale>.properties` file completely, not only the lines
changed in the task. Check the seed keys for the login view, main view, menu, and
user entity as well as the new keys.

For a locale that normally uses non-Latin text, this search finds likely English
values left from the template:

```bash
rg -n '^[^#=]+=[\x00-\x7F]+$' \
  -g 'messages_<locale>.properties' src/main/resources
```

Review every match; the search is a heuristic, not proof of an error. Product
names, URLs, abbreviations, numbers, and technical terms left untranslated on
purpose can be valid. Values such as `MainView.title=App`, `User=User`, or
`loginForm.username=Username` usually need translation in a non-English-only
application.

When the task adds keys to a bundle that is still an untouched template, choose
one of two outcomes and say which: translate the whole bundle, or translate only
the new keys and record the rest as a known gap.

## Forbidden

- Hardcoded user-visible text in XML or Java controllers.
- Adding a key to only one locale file.
- `msg://` keys that differ from properties keys only by case.
- Brief `msg://key` references to keys stored in another message group.
- Mixing the project's view key style (`UserListView.title`) with another
  (`customerListView.title`).
- Missing enum constant messages.
- `${0}` placeholders in `formatMessage`; use Java formatter placeholders such as
  `%s`.
- English template values left without review in any locale bundle other than the
  one the keys are written in, an `en,<other>` application included.
- Reviewing only the lines changed in the task instead of reading each such
  locale file completely.
- Translating the new keys of an untouched template bundle and silently leaving
  its seed keys; the result is a half-translated UI.
- Copies of framework or add-on message bundles created before checking for an
  official `jmix-translations-<lang>` artifact.

## Special cases

### When the task adds a locale

The login view offers the locales in `jmix.core.available-locales`. Set the full
list (`jmix.core.available-locales=en,es`) in the application's
`application.properties`. An add-on's `module.properties` has the lowest
precedence and cannot add a locale.
