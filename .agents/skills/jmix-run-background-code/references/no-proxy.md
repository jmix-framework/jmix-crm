# Call that does not go through a Spring proxy

Part of `jmix-run-background-code`. Read this when the call does not go through a Spring proxy.

- A self-call inside one instance (`this.generateDailyReports()`) never passes
  through the proxy, so the annotation is ignored. The call must come from
  outside the instance.
- A job (or any object) built by code Spring does not own, such as
  `new ReportJob()` or a custom Quartz `JobFactory` that creates the class
  directly instead of through the bean factory, gets neither injection nor
  interception.

If you are not sure, do not reason about it: run the path with no authentication set up and see whether it
throws (see **Verify** in `SKILL.md`).
