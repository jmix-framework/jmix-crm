---
name: jmix-create-resource-role
description: Create or update Jmix resource and row-level roles and role assignments — entity, attribute, view, menu, and specific policies, UI login permission, "own records / only in status X" edit rules, and the CREATE-implies-MODIFY rule.
---

# Create Resource Role

Use this skill when adding or changing Jmix security access.

Resource roles are additive, with no deny policy: if any assigned resource role
grants access, the user has it. A role interface may `extend` several role
interfaces of the same kind to combine their policies; one role cannot mix
`@ResourceRole` and `@RowLevelRole`.

## Steps

1. List every entity the role must access.
2. For each entity, re-read the requirement and choose the `@EntityPolicy`
   actions from its exact wording (**Requirement wording → policy actions**).
   - If the wording forbids updating or deleting the entity ("immutable",
     "cannot be updated", "cannot be deleted", "create only"), first read `references/no-update-delete.md` in this skill's folder.
   - If the requirement limits which rows a user sees or edits ("own records",
     "only in status X", "own department"), first read `references/row-level.md` in this skill's folder.
3. Add `@EntityAttributePolicy` for the fields the role must view or edit. Apply
   **CREATE implies MODIFY**.
4. List in `@ViewPolicy` every view the role can open: list views, detail views,
   and detail views opened only as dialogs. If an entity is a `composition`
   property of another entity, first read `references/composition-child.md` in this skill's folder.
5. Add `@MenuPolicy` for the views opened from the menu, using real item ids
   from `menu.xml`, not group ids (**Menu policies**).
6. Give every UI user `ui.loginToUi` (**Every UI user needs `ui.loginToUi`**).
7. Do not grant broader actions than the workflow requires.
8. If any service, listener, scheduled job, or custom controller runs under the
   role's session and writes entities (including side effects on another entity
   when a record is saved), read **When code outside the UI saves entities for
   the role's users** below.
9. Run **Self-check before finishing**.

## Role example: read and create only

Take the actions from **Requirement wording → policy actions**, not from this
example.

```java
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "Employee", code = EmployeeRole.CODE)
public interface EmployeeRole {
    String CODE = "employee";

    @EntityAttributePolicy(entityClass = OrderRequest.class,
            attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = OrderRequest.class,
            actions = {EntityPolicyAction.READ, EntityPolicyAction.CREATE})
    void orderRequestEntity();

    @ViewPolicy(viewIds = {
            "OrderRequest.list",
            "OrderRequest.detail"
    })
    @MenuPolicy(menuIds = "OrderRequest.list")
    void orderRequestScreens();
}
```

## Requirement wording → policy actions

| Requirement wording (about an entity)           | EntityPolicyAction         |
|-------------------------------------------------|----------------------------|
| "view only", "read only"                        | `READ`                     |
| "view and create", "create only"                | `READ`, `CREATE`           |
| "view, edit"                                    | `READ`, `UPDATE`           |
| "view, create, delete" (no update)              | `READ`, `CREATE`, `DELETE` |
| "full CRUD", "manage", "all operations"         | `ALL`                      |
| "cannot be updated", "immutable"                | do not include `UPDATE`    |
| "cannot be deleted"                             | do not include `DELETE`    |
| "cannot be updated or deleted"                  | `READ`, `CREATE` only — even for managers |

`list_read` is an open mode (read-only access), not a `readOnly` descriptor.

## CREATE implies MODIFY

If a role grants `EntityPolicyAction.CREATE` on an entity, its
`@EntityAttributePolicy` for the editable fields must be
`EntityAttributePolicyAction.MODIFY`, never `VIEW`.

- This holds even when the entity cannot be updated or deleted after creation
  (`READ` + `CREATE` only).
- Reason: an attribute with only a `VIEW` policy is modify-denied
  (`EntityAttributeContext.canModify()` returns false). A `CREATE + VIEW` user can
  persist a row but gets a create form whose fields are all read-only.
- Apply it to every role. A frequent mistake: the manager role has `MODIFY`, but a
  read/employee role that also has `CREATE` stays on `VIEW`. Both need `MODIFY`.
- If some attributes must be read-only even at creation (for example generated
  audit fields), leave them out of the `attributes` list or give that subset a
  second `VIEW` policy — not a blanket `VIEW` on `*`.

```java
// CORRECT for a read-and-create-only entity:
@EntityAttributePolicy(entityClass = <Entity>.class,
        attributes = "*", action = EntityAttributePolicyAction.MODIFY)   // MODIFY, not VIEW
@EntityPolicy(entityClass = <Entity>.class,
        actions = { EntityPolicyAction.READ, EntityPolicyAction.CREATE })
void entityPolicy();
```

## Every UI user needs `ui.loginToUi`

The login view refuses a user without the specific policy `ui.loginToUi`, whatever
else their roles grant. Give every UI user the project's `UiMinimalRole`
(`ui-minimal`: `ui.loginToUi`, `MainView`, `LoginView`): the business role extends
it, or the user gets a `ui-minimal` assignment, in seed changelogs too.

## Menu policies

Menu groups are navigation containers. Read `menu.xml` and grant the item ids that
open views; granting the enclosing `<menu id="...">` group id does not grant its
items.

```xml
<menu id="sales" title="msg://menu.sales">
    <item view="Customer.list"/>
    <item id="orders" view="Order.list"/>
</menu>
```

```java
@MenuPolicy(menuIds = {"Customer.list", "orders"})
```

Grant only `sales` only when the role intentionally controls the group itself and
the project's security checks use that group id.

## Self-check before finishing

A clean compile is not "done": most role defects pass `compileJava` and show up
only at render or in tests. Confirm each item with a concrete check, not by
re-reading your own code:

1. **CREATE ⇒ MODIFY.** In every role file, each entity granted
   `EntityPolicyAction.CREATE` has `MODIFY`, not `VIEW`, on its editable attributes.
2. **`@MenuPolicy` uses leaf ids** — the exact `<item>` ids/views in `menu.xml`.
3. **Every reachable view has a `@ViewPolicy` entry**, including composition
   dialog detail views opened from a parent grid without a menu item.
4. **Every UI user holds `ui.loginToUi`** (`ui-minimal`), seed assignments
   included.
5. **No user holds two row-level roles narrowing the same entity** unless the
   intersection is intended, including assignments of existing roles in seed
   data.

To assert a permission in Java at runtime, inject `AccessManager`, call
`applyRegisteredConstraints(...)` on a context (for example
`EntityOperationContext`), then check `isPermitted()`.

## Forbidden

- `EntityPolicyAction.ALL` when update/delete are not required.
- Any action broader than the workflow needs.
- `UPDATE` for an entity described as "immutable" or "cannot be updated";
  `UPDATE` or `DELETE` for one described as "cannot be updated or deleted" or
  "create only", even in the manager role.
- A role that grants entity actions with no `@EntityAttributePolicy`.
- `CREATE` with a `VIEW` attribute policy (or no `MODIFY`) on editable fields, in
  any role.
- A blanket `VIEW` on `*` to keep a few attributes read-only at creation.
- A UI user without `ui.loginToUi` (`ui-minimal`), seed assignments included.
- View policies only for list views while create/edit dialogs use detail views.
- Menu policy for a parent group when the user needs access to concrete menu items.
- Menu policy for views that are not menu entries.
- `@ResourceRole` and `@RowLevelRole` in one role.
- Counting a clean compile as a finished role.
- Two row-level roles narrowing the same entity on one user, unless the
  intersection is intended (seed assignments included): row-level policies are
  joined with AND.

## Special cases

### When code outside the UI saves entities for the role's users

Any service, listener, scheduled job, or custom controller running under the
role's session, including side effects on another entity when a record is saved.

- Make sure the operation has the permissions it needs on every entity it writes,
  or run it through an appropriate trusted service path.
- Attribute policies are enforced by Flow UI value bindings, grid columns, and
  `EntitySerialization`, not by `DataManager`. The data layer checks only
  entity-level and row-level rules, so such code persists an attribute the role
  was never granted, with no error. Guard the non-granted attributes in that code
  yourself.
