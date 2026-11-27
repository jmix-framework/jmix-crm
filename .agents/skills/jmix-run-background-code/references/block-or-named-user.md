# Authentication for a block or for a named user

Part of `jmix-run-background-code`. Read this when you need authentication for a block or for a named user.

Use `io.jmix.core.security.SystemAuthenticator` when you cannot put the work in
its own proxied method, or when you need a specific user instead of the system
user. Prefer `@Authenticated` for a whole entry point; use
`SystemAuthenticator` for a narrow block inside a larger method.

```java
import io.jmix.core.security.SystemAuthenticator;

private final SystemAuthenticator authenticator;

public void onMessage(String payload) {
    authenticator.runWithSystem(() -> {
        // DataManager calls are authenticated here
    });
}
```

- `runWithSystem(Runnable)` / `withSystem(AuthenticatedOperation<T>)`: the
  system user, which bypasses security constraints. `withSystem` returns a
  value; its argument is a lambda, like `Runnable`.
- `runWithUser(String, Runnable)` / `withUser(String, AuthenticatedOperation<T>)`:
  a named user, so role policies apply as they would for that person.
