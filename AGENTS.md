# Repository Guidelines

Before working in this repository, **load and read** the [Huanshankeji general agent instructions](https://github.com/huanshankeji/.github/blob/main/docs/general-agent-instructions.md) as the required organization-wide baseline. A skipped or failed load, or an unread document, is a blocking error until the document is in context and has been read.

## Project Structure

- `core/` bridges Exposed SQL preparation with Vert.x SQL execution. Put database-specific clients and SQL handling in `postgresql/`, `mysql/`, `oracle/`, or `mssql/`.
- `crud/` provides CRUD extensions, `crud-with-mapper/` adds mapping support, and `testcontainers/` provides database container helpers.
- Production Kotlin lives in each module’s `src/main/kotlin/`. In `core/`, `exposed/` and `jdbc/` contain Exposed and JDBC integration, and `vertx/sqlclient/` contains Vert.x client helpers. The unpublished `integrated/` module contains examples, tests in `src/test/kotlin/`, and benchmarks in `src/benchmarks/kotlin/`.
- Shared Gradle conventions live in `buildSrc/`. When changing Exposed compatibility, check both [VersionsAndDependencies.kt](buildSrc/src/main/kotlin/VersionsAndDependencies.kt) and the Exposed entry in [the version catalog](gradle/libs.versions.toml). Published modules track Kotlin ABI snapshots in `api/*.api`.

## Database Clients and Transactions

- Use [`EvscConfig`](README.md#create-a-databaseclient) as the common source of connection settings for Exposed and Vert.x. `DatabaseClient` prepares Exposed statements and executes the resulting SQL through Vert.x. APIs marked `@ExperimentalEvscApi` may change.
- `DatabaseClient.withTransaction` opens a Vert.x transaction. Use `statementPreparationExposedTransaction` when building or preparing statements needs Exposed's transaction context. For a missing context in `Query.forUpdate()`, use `Query.forUpdateWithExposedTransaction()`. For blocking Exposed operations, use Exposed's `transaction(database)`. See [transaction troubleshooting](README.md#common-issues) for examples.
- The [statement preparation provider](core/src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/StatementPreparationExposedTransactionProvider.kt) controls the Exposed transaction lifecycle. Use `ReusingJdbcTransactionExposedTransactionProvider.createWithClosedTransaction(database)` by default. Initialize it where blocking JDBC I/O is allowed so it can cache SQL preparation metadata and close any connection it acquires. Use a separate provider and transaction per thread or verticle for concurrent preparation. The underlying Exposed `Database` can be shared.
- `DatabaseExposedTransactionProvider` is the fallback and creates a transaction per call. See the [README](README.md#create-a-databaseclient) for provider options.

## Build, Test, and Development

Run the Gradle wrapper from the repository root. Use JDK 17 or newer to run Gradle and make JDK 11 available for the Kotlin toolchain, as in [CI](.github/workflows/ci.yml).

- `./gradlew build` compiles, verifies, and assembles the modules. Use `./gradlew check` for verification alone.
- `./gradlew :exposed-vertx-sql-client-core:build` builds and verifies the core module.
- `./gradlew :exposed-vertx-sql-client-integrated:test` runs the integration suite. Append `--tests '*TransactionTests'` to run one spec.
- `./gradlew :exposed-vertx-sql-client-integrated:benchmark` runs the integration benchmarks.
- Run `./gradlew checkKotlinAbi` to compare the current ABI with the tracked reference snapshots. For intentional ABI changes, run `./gradlew updateKotlinAbi` and review the generated diff before including the updated snapshots. See [Kotlin’s ABI validation guide](https://kotlinlang.org/docs/gradle-binary-compatibility-validation.html).
- `./gradlew publishToMavenLocal` makes the libraries available to other local projects.
- `./gradlew :dokkaGeneratePublicationHtml` generates the API reference used by the [Dokka Pages workflow](.github/workflows/dokka-gh-pages.yml).

For unpublished sibling library changes, follow the [organization’s local development workflow](https://github.com/huanshankeji/.github/blob/main/docs/dev-instructions.md#local-development-workflow). See [CONTRIBUTING.md](CONTRIBUTING.md) for GitHub Packages credentials when resolving development dependencies.

## Coding Style and Naming

Follow the official Kotlin style configured in `gradle.properties`. Use four spaces, `PascalCase` for types, and `lowerCamelCase` for functions and properties. Match source paths to their packages and add KDoc for public APIs where callers need usage or compatibility details.

## Testing

Use Kotest on JUnit Platform. Name specs by behavior with a `Tests` suffix, such as `TransactionTests`. Reuse `AllConfigurationsSpec` for behavior shared across databases or client types. Start Docker before running the integration suite, which uses Testcontainers to exercise real database dialects. The [coverage workflow](.github/workflows/codecov.yml) publishes Kover results to Codecov. Add regression cases for behavior changes.

## Commits and Pull Requests

Use short, imperative subjects such as `Clarify Exposed transaction context in troubleshooting`. In pull requests, explain the behavior change, link related issues or discussions, and list the checks you ran. Call out intentional public API changes and any affected database dialects.
