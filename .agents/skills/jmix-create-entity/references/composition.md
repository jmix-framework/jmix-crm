# Parent and child in a `@Composition`

Part of `jmix-create-entity`. Read this when the entity is a parent or child in a `@Composition`.

- Parent collection has `@Composition`.
- Parent collection has `@OnDelete(DeletePolicy.CASCADE)` when the child lifecycle belongs
  to the parent and the parent is soft-deleted (has `@DeletedDate`/`@DeletedBy`). For a
  hard-deleted parent, see below.
- Child has a non-null back reference to the parent: `@NotNull`, `@ManyToOne` with
  `fetch = FetchType.LAZY` and `optional = false`, and a join column with `nullable = false`.
- Parent detail view edits the child collection via a property-bound
  `<collection property=...>` (no loader/query), and the parent fetchPlan includes the child
  property.
- The child's own detail view exists, plus its role policy, when the UI opens the child in a
  dialog.

```java
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.Composition;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

@Composition
@OnDelete(DeletePolicy.CASCADE)
@OneToMany(mappedBy = "parent")
private List<ChildLine> lines;  // leave uninitialized — Jmix returns a NotInstantiatedList

@NotNull
@JoinColumn(name = "PARENT_ID", nullable = false)
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Parent parent;
```

**Hard-deleted parent.** `@OnDelete(DeletePolicy.CASCADE)` is an application-layer policy,
and Jmix applies it only to a parent that supports soft delete. On a parent with no
`@DeletedDate`/`@DeletedBy` the annotation does nothing and the children are orphaned. Put
the cascade on the child's foreign key in the changelog instead:

```xml
<addForeignKeyConstraint baseTableName="CHILD_LINE" baseColumnNames="PARENT_ID"
                         constraintName="FK_CHILD_LINE_ON_PARENT"
                         referencedTableName="PARENT" referencedColumnNames="ID"
                         onDelete="CASCADE"/>
```

Also add `@OnDeleteInverse(DeletePolicy.CASCADE)` to the child's back reference. It does not
change runtime behavior, but Studio reads it when it generates changelogs:

```java
@OnDeleteInverse(DeletePolicy.CASCADE)
@NotNull
@JoinColumn(name = "PARENT_ID", nullable = false)
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Parent parent;
```

`jmix-create-liquibase-changelog` presents a DB-level `onDelete="CASCADE"` as the option you
almost never want, because it assumes the Jmix default of soft delete. A hard-deleted
composition parent is the exception that wording allows for.
