---
name: jmix-create-fragment
description: Create or change reusable Jmix Flow UI fragments, embedded fragment instances, provided data components, fragment facets, host event subscriptions, or fragment renderers.
---

# Create Fragment

Use this skill when UI code should be reusable inside one or more views or fragments.

## Steps

1. Confirm the UI is reusable enough to justify a fragment; keep one-off layout inside the view.
2. Create a fragment controller in the relevant `view/...` package. It extends `Fragment<RootComponentType>` and has `@FragmentDescriptor("fragment-file.xml")`.
3. Create the XML descriptor with the fragment namespace and a required `<content>` root. The root component inside `<content>` must match the controller generic type.
4. Add `<data>` only for data the fragment owns. If the fragment shows or edits the host view's entity or container, first read `references/host-data.md` in this skill's folder.
5. Load the fragment's own loaders with `getFragmentData().loadAll()` in a `ReadyEvent` handler. If the fragment needs automatic loading, saved settings, URL query parameters, or a timer, first read `references/facets.md` in this skill's folder.
6. Pass parameters through public setters; set them in XML `<properties>`, or call the setters before adding the fragment.
7. Add message keys for user-visible labels, captions, and action text.
8. If a grid or list cell needs the fragment, read **When a grid or list cell renders a fragment** below.
9. Run the checks in **Verify** below.

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

## Verify

Fragment wiring errors compile and then throw `GuiDevelopmentException` (or a load failure) when the host view opens: an XML root that does not match the `Fragment<...>` generic type, a `provided="true"` container with no matching host id, or a view facet name inside the fragment (`dataLoadCoordinator` instead of `fragmentDataLoadCoordinator`).

1. **API symbols.** Before you type them, check that `Fragment`, `@FragmentDescriptor`, the `Fragments.create(...)` overload you use, and the fragment facet names (`fragmentDataLoadCoordinator`, not `dataLoadCoordinator`; `fragmentSettings`, not `settings`) exist in this project. Copy them from an existing fragment in `src/`, or confirm them with `jmix-verify-api-symbol`.
2. **Static inspection (Gate 1).** Run `jmix-ide-static-analysis` (get_file_problems) on the fragment descriptor and the host view. The Jmix XSD-aware inspection flags what the compiler ignores: a view facet name used in a fragment, an unknown component, or a `provided` container with no host counterpart.
3. **Open the host (Gate 2).** Compile the host and fragment together, then run the test or view that embeds the fragment; root-type and provided-data mismatches show only when the host initializes.

## Forbidden

- Fragment controller without matching XML descriptor.
- A fragment descriptor without the `<content>` root.
- Calling a fragment's parameter setters after the fragment is added to the host.
- XML root component different from `Fragment<...>` generic type.
- One-off view layout extracted into a fragment without reuse or isolation benefit.
- `provided="true"` without a same-id host data component.
- The view facet names `dataLoadCoordinator` / `settings` inside a fragment; use `fragmentDataLoadCoordinator` / `fragmentSettings` (Jmix 2.8+).
- `urlQueryParameters` entries referencing components that are not declared in the fragment.
- Fragment API symbols or facet names typed without checking them in `src/` or with `jmix-verify-api-symbol`.
- Creating or adding a fragment that subscribes to host events after that host event has fired.
- Hardcoded user-visible labels.
- Using `UiComponentUtils` to find inner fragment components by Vaadin ids.

## Special cases

### When a grid or list cell renders a fragment

- Use a fragment renderer only when the cell needs reusable UI more complex than a simple renderer.
- Keep renderer fragments read-only unless the workflow explicitly supports editing from the cell.
