# Add-on that owns persistent entities

Part of `jmix-create-liquibase-changelog`. Read this when you add a Jmix add-on that owns persistent entities.

Adding the add-on dependency does not make its module changelog reachable from a
root changelog that lists module changelogs explicitly. Find the add-on's
changelog resource and add an explicit `<include>` to the root `changelog.xml`.
For example, the Audit add-on requires:

```xml
<include file="/io/jmix/audit/liquibase/changelog.xml"/>
```

Jmix module changelogs commonly use this path pattern:

```xml
<include file="/io/jmix/<addon>/liquibase/changelog.xml"/>
```

Confirm the actual resource path in the add-on before adding it. Do not assume
that the application's `<includeAll>` covers changelogs packaged in add-on JARs.

After startup, verify that the add-on tables exist and use a feature that needs
them. Also search the complete startup log for missing-schema errors:

```bash
rg -n 'does not exist|relation .* does not exist' /path/to/startup.log
```

Treat every match as a failure until it is explained. A green `test`
does not prove that the add-on schema was created when no test accesses its
tables. Some add-ons log the missing-table error and let the
application keep running with the feature unavailable.
