# Reference attributes

Part of `jmix-create-detail-view`. Read this when the entity has a reference attribute.

For `@ManyToOne` and other entity references, pick the pattern by the candidate set:

- **`entityComboBox` with `itemsQuery` — preferred.** The JPQL runs against the database on every type-ahead and fetches only matching candidates. Use it for any non-trivial candidate set.
- **`entityComboBox` with `itemsContainer`** — for small, fixed sets that fit in memory and rarely change. Loaded once at view open.
- **`entityPicker`** with lookup and clear actions — when users need a full lookup screen. Use the standard action ids `entity_lookup` and `entity_clear` exactly; do not invent ids.

**`itemsQuery` shape.** The JPQL must use `like :searchString`. The parameter is the bare combo input unless `searchStringFormat` wraps it: an empty string when the dropdown opens, so `like ''` returns no rows, and the exact, case-sensitive text afterwards. Set the format and the escaping on the `itemsQuery` element; without them every gate stays green and only the browser shows an empty dropdown. The JPQL must use the JPA/Jmix entity name, not the database table name.

```xml
<entityComboBox id="refField" property="ref">
    <itemsQuery class="com.company.app.entity.Ref"
                searchStringFormat="(?i)%${inputString}%"
                escapeValueForLike="true">
        <query>
            <![CDATA[
            select e from Ref e
            where e.name like :searchString escape '\'
            order by e.name
            ]]>
        </query>
    </itemsQuery>
</entityComboBox>
```

A UI test that fetches the picker's items only with a partial filter does not catch a missing `searchStringFormat`; fetch with the empty filter too.

`itemsQuery` does not bind `:container_*` / `:component_*` parameters. When the reference list depends on another component or container, use `itemsContainer` with a regular loader.

**`itemsContainer`.** Declare a `<collection>` for the candidate entities and point the combo at it; `<dataLoadCoordinator auto="true"/>` loads it at open:

```xml
<data>
    <instance id="<entity>Dc" class="com.company.app.entity.<Entity>">
        <fetchPlan extends="_base">
            <property name="ref" fetchPlan="_instance_name"/>
        </fetchPlan>
        <loader id="<entity>Dl"/>
    </instance>
    <collection id="refsDc" class="com.company.app.entity.Ref">
        <fetchPlan extends="_instance_name"/>
        <loader id="refsDl">
            <query><![CDATA[select e from Ref e order by e.name]]></query>
        </loader>
    </collection>
</data>
...
<entityComboBox id="refField" property="ref" itemsContainer="refsDc"/>
```

Before finishing, verify that saved reference entities can appear in the component data provider. If a field is required, do not leave a reference component without a working item source or lookup action.
