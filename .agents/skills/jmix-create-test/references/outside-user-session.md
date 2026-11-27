# Code under test that runs outside a user session

Part of `jmix-create-test`. Read this when the code under test runs outside a user session.

A scheduler / `@Async` / application event listener path has no authenticated user, and a test that sets up authentication hides exactly the defect worth catching. Call the entry point without authentication, and authenticate only the data setup:

```java
@Test
void generateDailyReportsRunsOnTheSchedulerThread() {
    systemAuthenticator.runWithSystem(() -> givenOrder());   // setup only
    // no authentication around the call — reproduces the scheduler thread
    reportService.generateDailyReports();
}
```

See `jmix-run-background-code` for the `@Authenticated` / `SystemAuthenticator` rules on the production side.
