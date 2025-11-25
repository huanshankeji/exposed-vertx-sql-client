# Copilot Instructions for exposed-vertx-sql-client

## Repository Overview

### Summary
This is **Exposed Vert.x SQL Client**, a Kotlin library that provides integration between JetBrains Exposed (SQL DSL/ORM) and Vert.x Reactive SQL Client. The library enables reactive database operations using Exposed's type-safe SQL DSL.

### High Level Repository Information
- **Type**: Kotlin JVM library project
- **Size**: Medium-sized (8 modules across core, database-specific, and CRUD extensions)
- **Languages**: Kotlin (~450 source files), Gradle Kotlin DSL for build scripts
- **Target Platform**: JVM (JDK 11+)
- **Build System**: Gradle with custom convention plugins in buildSrc
- **Documentation**: Generated with Dokka
- **Testing**: Integration tests in `integrated` module using Testcontainers, plus benchmarks

**Key Facts:**
- Language: Kotlin (~450 source files)
- Build: Gradle (Gradle 9.1.0, Kotlin 2.2.21)
- JVM Target: JDK 11 (configured via `kotlin.jvmToolchain(11)`)
- Supported Databases: PostgreSQL, MySQL, Oracle, Microsoft SQL Server
- Size: ~18MB repository
- Current Version: 0.6.0-SNAPSHOT
- Exposed Version: v1.0.0-rc-3 (important: stick to this exact version for compatibility)

## Project Structure

### Modules (Multi-module Gradle project)

The repository uses Gradle's `settings.gradle.kts` with concatenated project names:

1. **core** (`exposed-vertx-sql-client-core`) - Core abstractions and base functionality
2. **postgresql** (`exposed-vertx-sql-client-postgresql`) - PostgreSQL-specific implementation
3. **mysql** (`exposed-vertx-sql-client-mysql`) - MySQL-specific implementation
4. **oracle** (`exposed-vertx-sql-client-oracle`) - Oracle-specific implementation
5. **mssql** (`exposed-vertx-sql-client-mssql`) - Microsoft SQL Server implementation
6. **crud** (`exposed-vertx-sql-client-crud`) - Extension CRUD operations
7. **crud-with-mapper** (`exposed-vertx-sql-client-crud-with-mapper`) - CRUD with GADT mapping support
8. **integrated** (`exposed-vertx-sql-client-integrated`) - Integration tests, benchmarks and examples (not published)

### Key Files and Directories

**Root Level:**
- `build.gradle.kts` - Root build file with Dokka and API validation config
- `settings.gradle.kts` - Multi-module configuration
- `gradle.properties` - Gradle configuration (configuration cache enabled)
- `buildSrc/` - Shared build logic and version management
  - `buildSrc/src/main/kotlin/VersionsAndDependencies.kt` - Central version definitions
  - `buildSrc/src/main/kotlin/conventions.gradle.kts` - Common conventions
  - `buildSrc/src/main/kotlin/lib-conventions.gradle.kts` - Library conventions
- `.github/workflows/kotlin-jvm-ci.yml` - CI configuration

**Module Structure (each module):**
- `build.gradle.kts` - Module-specific dependencies
- `src/main/kotlin/` - Source code
- `api/*.api` - Binary compatibility API definitions (for Kotlin binary compatibility validator)

**Important Configuration Files:**
- All linting/formatting is handled via Kotlin plugin defaults (no custom linters)
- API compatibility validation uses `.api` files in each module's `api/` directory

## Build and Validation Instructions

### Prerequisites

**Required:**
- JDK 11 or higher (JDK 11 for compilation, CI tests with JDK 11 and 17)
- Internet connection for dependency downloads

**Setup:**
The Gradle wrapper (`./gradlew`) handles all tooling. No additional installation needed.

**Initial setup time**: ~1-2 minutes for Gradle daemon initialization and dependency download on first build.

### Environment Setup

Always ensure JDK 11 or higher is properly configured before building. The project uses Gradle wrapper, so no manual Gradle installation is needed.

**IMPORTANT**: If the project uses snapshot dependencies of other `com.huanshankeji` libraries, especially in a branch other than `main` such as `dev`, refer to the setup instructions at <https://github.com/huanshankeji/.github/blob/main/dev-instructions.md#about-snapshot-dependencies-of-our-library-projects>.

### Build Commands (In Order of Priority)

**ALWAYS use the Gradle wrapper (`./gradlew`) NOT `gradle`.**

1. **Clean Build** (recommended before first build or after major changes):
   ```bash
   ./gradlew clean build
   ```
   - Time: ~4-5 minutes for clean build with tests, ~5-15 seconds with daemon for incremental builds
   - What it does: Compiles all modules, runs integration tests, validates API compatibility

2. **Check** (validation only - recommended for verifying changes):
   ```bash
   ./gradlew check
   ```
   - Time: ~4-5 minutes (includes integration tests with Testcontainers)
   - What it does: Runs all verification tasks including `apiCheck` and integration tests
   - **This is what CI runs** via the `gradle-test-and-check` action

3. **API Compatibility Check** (required after API changes):
   ```bash
   ./gradlew apiCheck
   ```
   - Time: ~18 seconds
   - What it does: Validates API against `.api` files using `binary-compatibility-validator`
   - Run this after modifying public APIs

4. **API Dump** (update API definitions after intentional API changes):
   ```bash
   ./gradlew apiDump
   ```
   - Updates `.api` files in each module's `api/` directory
   - Commit these changes along with API modifications

5. **Publish to Maven Local** (for testing in other projects):
   ```bash
   ./gradlew publishToMavenLocal
   ```
   - Time: ~50 seconds
   - Publishes all library modules to `~/.m2/repository`

6. **Documentation Generation**:
   ```bash
   ./gradlew dokkaGeneratePublicationHtml
   ```
   - Generates API docs in `build/dokka/html/`

### Build Timing and Known Issues

**Timing Expectations**:
- First build with tests: ~4-5 minutes (includes dependency resolution and Testcontainers startup)
- Subsequent builds with daemon: ~5-15 seconds for incremental changes
- Clean builds with tests: ~4-5 minutes
- Check task: ~4-5 minutes (runs integration tests)
- API validation only: ~18 seconds

**Common Warnings (can be ignored)**:
- Dokka package-list download errors from external sites (builds still succeed)
- Configuration cache warnings (cache is enabled and working)

**Error Handling**:
- Gradle daemon issues: Use `./gradlew --stop` then retry
- Configuration cache issues: Delete `.gradle/` directory and rebuild
- Dependency resolution fails: Check internet connection; mavenCentral() is the primary repository

### Important Build Notes

**ALWAYS:**
- Run `./gradlew check` before committing to ensure API compatibility
- Use the Gradle daemon (default) for faster builds - DO NOT use `--no-daemon` unless necessary
- Configuration cache is enabled by default (speeds up subsequent builds)

**Common Issues:**
- **Build fails with package-list download errors**: These are warnings from Dokka and can be ignored - builds still succeed
- **"No transaction in context" errors**: Wrap code in `databaseClient.exposedTransaction { ... }` or use transaction-aware variants like `forUpdateWithTransaction()`
- **API validation failures**: If you intentionally changed APIs, run `./gradlew apiDump` to update the golden API files

**Never:**
- DO NOT run tasks without `./` prefix (use `./gradlew`, not `gradlew`)
- DO NOT modify `.api` files manually - always use `apiDump`
- DO NOT add custom test runners or linting tools unless specifically requested

## Testing

**Current State:** This project has integration tests in the `integrated` module using Testcontainers.

**Integration Tests:** Located in `integrated/src/test/kotlin/` using Kotest framework with Testcontainers:
```bash
./gradlew :exposed-vertx-sql-client-integrated:test
```
- Tests run against real database instances via Testcontainers
- Multiple integration tests covering core functionality across different databases

**Benchmarks:** Located in `integrated/src/benchmarks/kotlin/` using kotlinx-benchmark:
```bash
./gradlew :exposed-vertx-sql-client-integrated:benchmark
```

**Quality Assurance:** The project's quality is also validated by internal consuming projects (not in this repo).

## CI/CD Pipeline

**GitHub Actions Workflows:**

### `.github/workflows/kotlin-jvm-ci.yml` (Main CI)
Runs on every push and pull request to any branch:
1. **test-and-check** job:
   - Uses shared action: `huanshankeji/.github/actions/gradle-test-and-check@v0.2.0`
   - Tests with: JDK 11-temurin, JDK 17-temurin
   - Runs: `./gradlew check`

2. **dependency-submission** job:
   - Submits dependency graph to GitHub

### `.github/workflows/codecov.yml` (Code Coverage)
- Runs tests with Kover and uploads coverage reports to Codecov
- Triggered on push and pull request to any branch

### `.github/workflows/dokka-gh-pages.yml` (Documentation)
- Deploys API documentation to GitHub Pages
- Triggered on push/PR to `plugins-release` branch
- Generates documentation with `./gradlew :dokkaGeneratePublicationHtml`

### `.github/workflows/copilot-setup-steps.yml` (Copilot Setup)
- Sets up JDK 11 and 17 with Gradle for Copilot coding agent

**To replicate CI locally:**
```bash
./gradlew check
```

### Validation and Quality Checks

Before check-in, the following validations run:
1. **Compilation**: All modules compile successfully
2. **Integration Tests**: Run via Testcontainers against real database instances
3. **API Compatibility**: Binary compatibility validation via `apiCheck`
4. **Dependency Analysis**: Automated dependency submission to GitHub (in CI)
5. **Code Coverage**: Coverage reports uploaded to Codecov (in CI)

**Code Style:**
- Follow [our Kotlin code style guide](https://github.com/huanshankeji/.github/blob/main/kotlin-code-style.md) for all Kotlin code contributions

## Development Workflow

### Making Code Changes

1. **Understand the module structure**: Changes to core functionality go in `core/`, database-specific code in `postgresql/` or `mysql/`, CRUD helpers in `crud/` or `crud-with-mapper/`

2. **Build incrementally**:
   ```bash
   ./gradlew :exposed-vertx-sql-client-core:build
   ```

3. **Check your changes**:
   ```bash
   ./gradlew check
   ```

4. **If you modified public APIs**:
   ```bash
   ./gradlew apiDump
   git add */api/*.api
   ```

### Key Dependencies

- **Exposed**: 1.0.0-rc-3 (via `commonDependencies.exposed.*`)
- **Vert.x**: Managed by `vertx.platformStackDepchain()` (uses Vert.x BOM)
- **Kotlin**: 2.2.21
- **Arrow**: For functional constructs
- **exposed-gadt-mapping**: 0.4.0 (for mapper modules)

**Dependency Management:** All versions are centralized in `buildSrc/src/main/kotlin/VersionsAndDependencies.kt` using `common-gradle-dependencies` library.

**Additional Development Resources:**
- For snapshot dependencies and development branch workflows, see [@huanshankeji/.github/dev-instructions.md](https://github.com/huanshankeji/.github/blob/main/dev-instructions.md)

## Architecture Notes

### Code Organization

Each module follows standard Kotlin source structure:
```
module-name/
  ├── api/                           # API compatibility files
  ├── build.gradle.kts              # Module dependencies
  └── src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/
```

**Key Packages (in core module):**
- `jdbc/` - Exposed JDBC integration
- `exposed/` - Exposed-specific utilities
- `vertx/sqlclient/` - Vert.x SQL Client abstractions
- `local/` - Local connection configuration helpers

**Database-specific packages:**
- `postgresql/` - In postgresql module
- `mysql/` - In mysql module
- `oracle/` - In oracle module
- `mssql/` - In mssql module

### Important Implementation Details

1. **DatabaseClient** is the main entry point for executing reactive database operations
2. **EvscConfig** is the single-source-of-truth for database configuration (since v0.5.0)
3. API marked with `@ExperimentalEvscApi` is subject to change
4. Shared `Database` instances improve performance (can be shared across verticles)
5. Some Exposed APIs require wrapping in `exposedTransaction { ... }`

### Build System

- **buildSrc**: Precompiled script plugins with shared conventions
- **Convention plugins**: `conventions.gradle.kts` and `lib-conventions.gradle.kts`
- **API Validation**: Uses `binary-compatibility-validator` plugin (v0.18.1)
- **Documentation**: Uses Dokka plugin (v2.1.0) for KDoc generation

## Key Files Reference

### Build Configuration
- `build.gradle.kts`: Root build file with Dokka setup, API validation
- `settings.gradle.kts`: Project structure, naming conventions (concatenated module names)
- `gradle.properties`: JVM settings, configuration cache enabled
- `buildSrc/build.gradle.kts`: buildSrc/meta-build plugin dependencies and versions
- `buildSrc/src/main/kotlin/VersionsAndDependencies.kt`: Shared compilation dependencies and versions

### Documentation
- `README.md`: Maven coordinates, basic usage guide, API docs link
- `CONTRIBUTING.md`: Development setup, JDK requirements, workflow guidelines
- [@huanshankeji/.github/dev-instructions.md](https://github.com/huanshankeji/.github/blob/main/dev-instructions.md): Additional development instructions from the organization
- Each module has `api/` directory for compatibility validation files

### Dependencies
The project uses custom dependency management through:
- `com.huanshankeji:common-gradle-dependencies` for shared dependencies
- `com.huanshankeji.team:gradle-plugins` for build conventions
- Kotlin 2.2.21, Dokka 2.1.0

## Quick Reference

### Most Common Commands
```bash
./gradlew check                    # Verify all checks pass
./gradlew apiCheck                 # Check API compatibility
./gradlew apiDump                  # Update API files after changes
./gradlew publishToMavenLocal      # Install locally
./gradlew clean build              # Full clean build
```

### File Paths to Know
- Version management: `buildSrc/src/main/kotlin/VersionsAndDependencies.kt`
- Main config: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- CI config: `.github/workflows/kotlin-jvm-ci.yml`
- API definitions: `*/api/*.api` (generated, commit after running `apiDump`)
- Convention plugins: `buildSrc/src/main/kotlin/conventions.gradle.kts`, `buildSrc/src/main/kotlin/lib-conventions.gradle.kts`

### When Things Go Wrong

1. **Build fails mysteriously**: Try `./gradlew clean build`
2. **API check fails**: Run `./gradlew apiDump` if the API change was intentional, then commit the updated `.api` files
3. **Configuration cache issues**: Delete `.gradle/` directory and rebuild
4. **Dependency resolution fails**: Check internet connection; mavenCentral() is the primary repository
5. **Gradle daemon issues**: Use `./gradlew --stop` then retry the build

## Trust These Instructions

These instructions are based on thorough exploration and testing of the repository. The commands listed have been validated to work correctly. Only search for additional information if:
- These instructions are incomplete for your specific task
- You encounter an error not documented here
- You need details about internal implementation not covered here

For questions or issues, refer to `CONTRIBUTING.md` or open a GitHub Discussion.

**Trust these instructions**: This information has been validated through actual command execution and file inspection. Only search for additional information if these instructions are incomplete or found to be incorrect.
