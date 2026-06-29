---
name: jmix-create-fragment
description: Create or change reusable Jmix Flow UI fragments, embedded fragment instances, provided data components, fragment facets, host event subscriptions, or fragment renderers.
---

# Create Fragment

Use this skill when UI code should be reusable inside one or more views or fragments.

## Steps

1. Confirm the UI is reusable enough to justify a fragment; keep one-off layout inside the view.
2. Create a fragment controller in the relevant `view/...` package.
3. Extend `Fragment<RootComponentType>`.
4. Add `@FragmentDescriptor("fragment-file.xml")`.
5. Create the XML descriptor with the fragment namespace and a required `<content>` root.
6. Make the XML root component match the controller generic type.
7. Add `<data>` only for data the fragment owns, or mark host-owned containers/loaders with `provided="true"`.
8. Use a `<facets>` block when needed (Jmix 2.8+): `fragmentDataLoadCoordinator` to auto-load the fragment's loaders, plus `fragmentSettings` / `urlQueryParameters` / `timer`. Otherwise load the loaders yourself (`getFragmentData().loadAll()` in a `ReadyEvent` handler).
9. Give an embedded fragment instance a stable `id` when a facet (e.g. `fragmentSettings`) must persist state.
10. Pass parameters through public setters; use XML `<properties>` or call setters before adding the fragment.
11. Add message keys for user-visible labels, captions, and action text.
12. Compile the host view and fragment together.

## Controller Template

```java
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;

@FragmentDescriptor("customer-summary-fragment.xml")
public class CustomerSummaryFragment extends Fragment<VerticalLayout> {
    @Subscribe
    public void onReady(final ReadyEvent event) {
        getFragmentData().loadAll();
    }
}
```

## XML Skeleton

```xml
<fragment xmlns="http://jmix.io/schema/flowui/fragment">
    <data>
        <collection id="customersDc" class="com.company.app.entity.Customer">
            <fetchPlan extends="_base"/>
            <loader id="customersDl" readOnly="true">
                <query><![CDATA[select e from Customer e]]></query>
            </loader>
        </collection>
    </data>
    <content>
        <vbox id="root">
            <dataGrid id="customersDataGrid" dataContainer="customersDc">
                <columns>
                    <column property="name"/>
                </columns>
            </dataGrid>
        </vbox>
    </content>
</fragment>
```

## Embedding

Declarative embedding:

```xml
<fragment id="customerSummaryFragment"
          class="com.company.app.view.customer.CustomerSummaryFragment"/>
```

Programmatic embedding:

```java
CustomerSummaryFragment fragment =
        fragments.create(this, CustomerSummaryFragment.class);
targetLayout.add(fragment);
```

If the fragment subscribes to host events, create and add it before that host event fires.

## Fragment Facets

Since Jmix 2.8, a fragment descriptor supports a `<facets>` element with
FRAGMENT-specific facet names (NOT the view ones): `fragmentDataLoadCoordinator`,
`fragmentSettings`, `urlQueryParameters`, and `timer`.

```xml
<facets>
    <fragmentDataLoadCoordinator auto="true"/>
</facets>
```

`fragmentDataLoadCoordinator auto="true"` loads the fragment's own `<data>`
loaders; do NOT also call `getFragmentData().loadAll()` for the same loaders.
Without the facet, load them yourself in a `ReadyEvent` handler, or let the host
view load `provided="true"` containers. A facet that persists state
(`fragmentSettings`) needs a stable fragment instance `id` — prefer declarative
embedding with an `id`.

## Provided Data Components

Use `provided="true"` when the fragment edits or displays the host view's entity/container:

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

The host view or enclosing fragment must declare a data component with the same id.

## Fragment Renderers

Use a fragment renderer only when a grid/list cell needs reusable UI more complex than a simple renderer. Keep renderer fragments read-only unless the workflow explicitly supports editing from the cell.

## Verify — fragment wiring fails at view init, not compile

A fragment whose XML root does not match the `Fragment<...>` generic type, a
`provided="true"` container with no matching host id, or a VIEW facet name used
inside the fragment (`dataLoadCoordinator` instead of `fragmentDataLoadCoordinator`)
all compile clean and then throw `GuiDevelopmentException` (or a load failure)
when the host view opens.

1. **API symbols — verify before you type them.** `Fragment`,
   `@FragmentDescriptor`, the `Fragments.create(...)` overload you use, and the
   FRAGMENT facet names (`fragmentDataLoadCoordinator` not `dataLoadCoordinator`,
   `fragmentSettings` not `settings`) must exist in this project. Confirm via
   Context7 (`/jmix-framework/jmix-context7`), IDE symbol search, or an existing
   fragment in `src/` — see `jmix-verify-api-symbol`.
2. **Static inspection (Gate 1).** Run `jmix-ide-static-analysis`
   (get_file_problems) on the fragment descriptor and the host view — the
   Jmix-XSD-aware inspection flags a VIEW facet name used in a fragment, an
   unknown component, or a `provided` container with no host counterpart that the
   compiler ignores.
3. **Open the host (Gate 2).** Compile the host and fragment together, then run
   the test/view that embeds the fragment; root-type and provided-data
   mismatches only surface when the host initializes.

## Forbidden

- Fragment controller without matching XML descriptor.
- XML root component different from `Fragment<...>` generic type.
- One-off view layout extracted into a fragment without reuse or isolation benefit.
- `provided="true"` without a same-id host data component.
- The VIEW facet names `dataLoadCoordinator` / `settings` inside a fragment — use `fragmentDataLoadCoordinator` / `fragmentSettings` (Jmix 2.8+).
- `urlQueryParameters` entries referencing components that are not declared in the fragment.
- Hardcoded user-visible labels.
- Using `UiComponentUtils` to find inner fragment components by Vaadin ids.
