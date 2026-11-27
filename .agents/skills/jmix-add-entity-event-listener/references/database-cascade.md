# Children removed by a database cascade

Part of `jmix-add-entity-event-listener`. Read this when children are removed by a database cascade.

When a parent is hard-deleted and its children are removed through an `ON DELETE CASCADE` foreign key, no event is published for those children: there is nothing to subscribe to and no state left to read.

So any per-row cleanup a cascaded child owns (file storage, external APIs) must run before the parent delete is requested — load the children, clean up, then remove the parent, in a service method — or be accepted and recorded as a gap. Do not plan it as delete-side listener work.
