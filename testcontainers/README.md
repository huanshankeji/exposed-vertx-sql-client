# EVSC Testcontainers Helpers

EVSC-specific Testcontainers helpers: converting a `JdbcDatabaseContainer` to an
EVSC `ConnectionConfig.Socket`, connecting Exposed `Database`s from containers,
and HikariCP helpers built on top of EVSC's connection config.

For generic, non-EVSC Testcontainers helpers (e.g. `LatestPostgreSQLContainer()`
and `JdbcDatabaseContainer.hostAndPort()`), see the
[`kotlin-common-testcontainers`](https://github.com/huanshankeji/kotlin-common/tree/main/testcontainers)
module.
