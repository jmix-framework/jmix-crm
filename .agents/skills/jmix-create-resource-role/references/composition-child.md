# Entity that is a `composition` child

Part of `jmix-create-resource-role`. Read this when an entity is a `composition` child of another entity.

The child's detail view opens as a dialog from the parent's detail view. That
dialog is a real Jmix view subject to `@ViewPolicy`, though no menu leads to it.
Include the child detail view id in `@ViewPolicy` of every role that can edit the
parent, and keep it out of `@MenuPolicy`.

```java
@ViewPolicy(viewIds = {
    "<Parent>.list",
    "<Parent>.detail",
    "<Child>.detail",          // composition child, opened from <Parent>.detail
})
@MenuPolicy(menuIds = {"<Parent>.list"})   // child detail is NOT in menu
void parentScreens();
```

Without the child detail id the role compiles and fails at runtime when the user
clicks "+" in the parent's composition table.
