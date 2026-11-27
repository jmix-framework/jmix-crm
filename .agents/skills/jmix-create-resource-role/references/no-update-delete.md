# Requirement that forbids updating or deleting an entity

Part of `jmix-create-resource-role`. Read this when the requirement forbids updating or deleting an entity.

- "Immutable" or "cannot be updated": leave out `UPDATE`.
- "Cannot be deleted": leave out `DELETE`.
- "Cannot be updated or deleted" or "create only": `READ`, `CREATE` only, not even
  `UPDATE` or `DELETE` for the manager role.
- `EntityPolicyAction.ALL` includes `UPDATE` and `DELETE` and contradicts all of
  these.
- The attribute policy is still `MODIFY` (**CREATE implies MODIFY** in `SKILL.md`): create forms
  need it even when update/delete are forbidden.
