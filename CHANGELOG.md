# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## What's Changed

* Add `readOnlyTransactionIsolationLevel` in `DatabaseClientConfig` and update
  `exposedReadOnlyTransaction` to use this value, which defaults to
  `Connection.TRANSACTION_READ_UNCOMMITTED` by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/69
* Rename `exposedReadOnlyTransaction` to
  `statementPreparationExposedTransaction` by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/71
* Benchmark JDBC `suspendTransaction` and rename other functions adding underscores and unifying capitalization in
  `TransactionBenchmark` by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/74
* Some more unmerged changes that should've been included in #74 by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/75
* Benchmark Exposed
  `transaction` with HikariCP and extract some common code by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/76
* Benchmark R2DBC transaction by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/77
* Mark `EvscConfig` and `LocalConnectionConfig` with a newly
  `ExperimentalUnixDomainSocketApi` opt-in annotation and update README about alternatives by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/80
* Revamp transaction & savepoint APIs by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/81
* Update README adding and organizing the important notes by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/83
* Fix and update dokka-gh-pages.yml by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/86
* Update/Fix the included Dokka modules, which are outdated by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/89
* Add comprehensive tests for, overhaul, and fix bugs in extension CRUD DSL and transaction (including savepoint) APIs by @Copilot in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/82
* Review and improve library maturity for open source promotion by @Copilot in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/90
* Review and update the
  `crud-with-mapper` module by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/91
* Resolve the remaining opt-in and deprecation warnings by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/92
* v0.7.0 release by @ShreckYe in https://github.com/huanshankeji/exposed-vertx-sql-client/pull/93

**Full Changelog**: https://github.com/huanshankeji/exposed-vertx-sql-client/compare/v0.6.0...v0.7.0

## [0.6.0] - 2025-11-26

### Added

- Support all other databases commonly supported by Exposed and Vert.x (#18, #25, #53).
  - MySQL (#18)
  - Oracle (#53)
  - Microsoft SQL Server (#53)
- Add the `DatabaseClientConfig` interface which is an abstraction of the configurable options passed to `DatabaseClient` and add corresponding creator functions for each database (#25).
- Add an `exposedReadOnlyTransaction` variant to prevent accidentally writing to databases with Exposed APIs (#48).
- Add some basic integration tests adapted from the example code with Testcontainers (and Kotest) (#50). `@Untested` annotation usages are removed.
- Show test coverage with Kover and Codecov (#62, #63).

### Changed

- Bump dependencies to the latest.
  - Kotlin 2.2.21
  - Exposed 1.0.0-rc-3
  - Vert.x 5.0.5
  - JVM toolchain / JDK 11 (required by Vert.x)
  - Gradle 9.2.1
  - SLF4J 2.0.17
- Migrate to Exposed 1.0.0 and Vert.x 5 (#33, #27). Some APIs are updated accordingly.
- Update the instructions including the user guide in README.md (#51, #50).
- Rename the `sql-dsl` modules to `crud` modules to better reflect their contents (part of #33 and #27).
- Revamp Exposed transaction handling in `DatabaseClient`, not always creating only possibly needed Exposed transactions (#44). If you encounter "No transaction in context." issues, see the corresponding section in README.md.
- Update `exposedTransaction` to match the updated Exposed `transaction` API (part of #47).

### Deprecated

- Deprecate the statement creation APIs [in kotlin-common v0.7.0](https://github.com/huanshankeji/kotlin-common/blob/main/CHANGELOG.md#v070--2025-10-28) which are replaced by the Exposed `buildStatement` APIs.
- Deprecate the `WithTransaction` functions in `DatabaseClient`, which are not supposed to be used by consuming users.
- Deprecate the `jdbcUrl` function, whose format is not actually universal, and the `ConnectionConfig.Socket.exposedDatabaseConnect` function which depends on it.

### Removed

- Some old APIs are migrated thus their original signatures are removed.

### Internal

- Use extracted CI actions (#24).
- Enable Gradle Configuration Cache.
- Bump Dokka to 2.1.0.
- Improve the Exposed transaction benchmarks, fixing some bugs, better ruling out the overhead of some implementations, and benchmarking the performance improvements of read-only Exposed transactions, which showed no significant improvements (#45, #47).
- Onboard with Copilot (#37, #36, #64).
- Enable CI for pull requests (#65).

## [0.5.0] - 2024-11-29

Because of [the Exposed SELECT DSL design changes](https://github.com/JetBrains/Exposed/pull/1916), and also because the old `DatabaseClient` creation APIs were poorly designed and too cumbersome, causing additional cognitive burdens on the users, this release has been completely overhauled. Some old APIs are removed directly because deprecating and delegating them to new ones fills the code base with unused code. Therefore, this release is **not source-compatible or binary-compatible** with v0.4.0. Please do not update unless you have time to adapt to the refactored changes. We are sorry for the inconvenience. From this version on, we will try to maintain source and binary compatibility, deprecating APIs instead of removing them in the foreseeable future.

Please check out the [updated README](README.md) to upgrade to v0.5.0.

Functional changes:

* adapt to [the Exposed SELECT DSL design changes](https://github.com/JetBrains/Exposed/pull/1916) (resolve #8)
* rename the SQL DSL functions taking mapper parameters, adding "withMapper" prefixes (resolve #6)
* split the library into multiple modules including "core", "postgresql", "crud", and "crud-with-mapper"
* generalize the functions with the types `PgPool` and `PgConnection` to work with different RDBMSs, replacing them with `Pool` and `SqlConnection`
* get rid of all usages of `PgPool` which was deprecated in Vert.x 4.5
* extract some new APIs and move some existing APIs into finer-grained subpackages, including `jdbc`, `exposed`, `vertx.sqlclient`, and `local` in the "core" module, and `postgresql` in the "postgresql" module
* overhaul the APIs related to the creation of Exposed `Database`s, Vert.x SQL clients, and `DatabaseClient`s

  * remove the `create...andSetRole` functions to create Vert.x SQL Clients, with their functionalities merged into the `create...` functions to create Vert.x SQL Clients

  * refactor the Exposed `Database` and Vert.x `SqlClient` creation APIs to be more configurable and straightforward

  * remove the extra shortcut `DatabaseClient` creation APIs such as `createPgPoolDatabaseClient` as they were poorly designed and too cumbersome, causing additional cognitive burdens on the users

     There are too many different combinations with different RDMBSs such as PostgreSQL and MySQL, and different Vert.x SQL Clients such as `SqlClient`, `Pool`, and `SqlConnection`. Therefore we don't provide such shortcut APIs anymore as they are just too cumbersome and cause additional cognitive burdens on the users, and we encourage the library users to create their own (see the updated guide in README.md for instructions). In this way, the Exposed `Databse`s and Vert.x SQL Clients are also more configurable.

* adopt `EvscConfig` as the single-source-of-truth database client config and no longer prefer local connections

   `LocalConnectionConfig` should be converted to `EvscConfig` or `ConnectionConfig` to be used.

* mark some APIs as experimental as they are subject to change
* make `DatabaseClient` implement our new `CoroutineAutoCloseable` interface
* add a version of `selectWithMapper` without `buildQuery`
* point out in the usage guide that you are encouraged to share/reuse an Exposed `Database` which generates SQLs among multiple `DatabaseClient`s in multiple verticles, which improves performance as shown in our benchmark results

Miscellaneous changes:

* add API documentation generated by Dokka hosted at <https://huanshankeji.github.io/exposed-vertx-sql-client/>
* add CODE_OF_CONDUCT.md and CONTRIBUTING.md
* use the Kotlin binary compatibility validator
* bump Exposed to v0.56.0

## [0.4.0] - 2024-10-19

* bump Exposed to 0.53.0
* fix a bug that an Exposed transaction is required if a query `FieldSet` contains custom functions depending on dialects and no such a transaction is provided
* Add a basic usage guide

[Unreleased]: https://github.com/huanshankeji/exposed-vertx-sql-client/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/huanshankeji/exposed-vertx-sql-client/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/huanshankeji/exposed-vertx-sql-client/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/huanshankeji/exposed-vertx-sql-client/releases/tag/v0.4.0
