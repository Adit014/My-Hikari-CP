# My-Hikari-CP

📖 [Read the article: Why Your Database is Slower Than It Should Be and How HikariCP Fixes It - Medium URL](https://medium.com/@adi003/why-your-database-is-slower-than-it-should-be-and-how-hikaricp-fixes-it-9ddc6883c070)

📖 [Read the article: Why Your Database is Slower Than It Should Be and How HikariCP Fixes It - Medium URL](https://adi-backend.hashnode.dev/why-your-database-is-slower-than-it-should-be-and-how-hikaricp-fixes-it)


A simple **connection pool** prototype with design ideas similar to [HikariCP](https://github.com/brettwooldridge/HikariCP). It maintains a pool of JDBC connections, hands them out to callers, and takes them back on release.

## How it works

- **Pool configuration** — You configure the pool via `ConnectionProperties`: JDBC URL, credentials, `minIdle`, `maxSize`, `connectionTimeout`, `keepAlive`, and `maxAlive` (max connection age in ms).

- **Borrowing a connection** — `getConnection()` first tries connections already used by the current thread (thread-local list), then an available connection from the shared pool, then creates a new connection if under `maxSize`. If the pool is full, the thread waits on a queue until a connection is released or the timeout is reached.

- **Returning a connection** — `release(connection)` either gives the connection to a waiting thread (if any) or marks it `AVAILABLE` and puts it back in the pool. Dead connections are removed and replaced to keep at least `minIdle`.

- **Background scheduler** — A scheduled task runs at `keepAlive` intervals: it checks connections (liveness, max age) and evicts or recycles them so the pool stays healthy.

- **Shutdown** — `shutdown()` disables the pool, stops the scheduler, and closes all connections after in-use ones are released.

## How to run

**Prerequisites:** Java 11+, Maven.

1. Clone or open the project and go to the project root.

2. Build:
   ```bash
   mvn compile
   ```

3. Run the main class (demo: single get/release, 5-thread concurrency, pool exhaustion with 6 threads, then shutdown):
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.myhikaricp.Main"
   ```
   Or run `com.example.myhikaricp.Main` from your IDE.

The demo uses an in-memory H2 database (`jdbc:h2:mem:testdb`); no external DB setup is required.
