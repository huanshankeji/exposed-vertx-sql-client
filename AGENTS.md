# Repository Guidelines

Before working in this repository, **load and read** the [Huanshankeji general agent instructions](https://github.com/huanshankeji/.github/blob/main/docs/general-agent-instructions.md) as the required organization-wide baseline. A skipped or failed load, or an unread document, is a blocking error until the document is in context and has been read.

## Project Structure

- `core/` bridges Exposed SQL preparation with Vert.x SQL execution. Put database-specific clients and SQL handling in `postgresql/`, `mysql/`, `oracle/`, or `mssql/`.
- `crud/` provides CRUD extensions, `crud-with-mapper/` adds mapping support, and `testcontainers/` provides database container helpers.
- Production Kotlin lives in each module’s `src/main/kotlin/`. The `integrated/` module contains examples, tests in `src/test/kotlin/`, and benchmarks in `src/benchmarks/kotlin/`.
- Shared Gradle conventions live in `buildSrc/`. Published modules track Kotlin ABI snapshots in `api/*.api`.

## Build, Test, and Development

Run the Gradle wrapper from the repository root. Use JDK 17 or newer to run Gradle and make JDK 11 available for the Kotlin toolchain.

- `./gradlew build` compiles, verifies, and assembles the modules. Use `./gradlew check` for verification alone.
- `./gradlew :exposed-vertx-sql-client-integrated:test` runs the integration suite. Append `--tests '*TransactionTests'` to run one spec.
- `./gradlew checkKotlinAbi` checks API compatibility. For an intentional public API change, review the diff from `./gradlew updateKotlinAbi` and include the relevant snapshots.
- `./gradlew publishToMavenLocal` makes the libraries available to other local projects.

See [CONTRIBUTING.md](CONTRIBUTING.md) for GitHub Packages credentials when resolving development dependencies.

## Coding Style and Naming

Follow the official Kotlin style configured in `gradle.properties`. Use four spaces, `PascalCase` for types, and `lowerCamelCase` for functions and properties. Match source paths to their packages and add KDoc for public APIs where callers need usage or compatibility details.

## Testing

Use Kotest on JUnit Platform. Name specs by behavior with a `Tests` suffix, such as `TransactionTests`. Reuse `AllConfigurationsSpec` for behavior shared across databases or client types. Start Docker before running Testcontainers-backed tests. CI publishes Kover coverage to Codecov. Add regression cases for behavior changes.

## Commits and Pull Requests

Use short, imperative subjects such as `Clarify Exposed transaction context in troubleshooting`. In pull requests, explain the behavior change, link related issues or discussions, and list the checks you ran. Call out intentional public API changes and any affected database dialects.
