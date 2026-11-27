# Fragment that shows or edits the host's data

Part of `jmix-create-fragment`. Read this when the fragment shows or edits the host's data.

Mark host-owned containers and loaders with `provided="true"`:

```xml
<data>
    <instance id="customerDc"
              class="com.company.app.entity.Customer"
              provided="true"/>
</data>
<content>
    <formLayout id="form" dataContainer="customerDc">
        <textField id="nameField" property="name"/>
    </formLayout>
</content>
```

- The host view or enclosing fragment must declare a data component with the same id.
- Let the host view load `provided="true"` containers.
