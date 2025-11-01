# Copilot Instructions for exposed-vertx-sql-client

## Repository Overview

This is **Exposed Vert.x SQL Client**, a Kotlin library that provides integration between JetBrains Exposed (SQL DSL/ORM) and Vert.x Reactive SQL Client. The library enables reactive database operations using Exposed's type-safe SQL DSL.

**Key Facts:**
- Language: Kotlin (445 source files)
- Build: Gradle (Gradle 9.1.0, Kotlin 2.2.21)
- JVM Target: JDK 11 (configured via `kotlin.jvmToolchain(11)`)
- Supported Databases: PostgreSQL (stable), MySQL (experimental since v0.6.0)
- Size: ~18MB repository
- Current Version: 0.6.0-SNAPSHOT
- Exposed Version: v0.56.0 (important: stick to this exact version for compatibility)

## Project Structure

### Modules (Multi-module Gradle project)

The repository uses Gradle's `settings.gradle.kts` with concatenated project names:

1. **core** (`exposed-vertx-sql-client-core`) - Core abstractions and base functionality
2. **postgresql** (`exposed-vertx-sql-client-postgresql`) - PostgreSQL-specific implementation
3. **mysql** (`exposed-vertx-sql-client-mysql`) - MySQL-specific implementation (experimental)
4. **crud** (`exposed-vertx-sql-client-crud`) - Extension CRUD operations
5. **crud-with-mapper** (`exposed-vertx-sql-client-crud-with-mapper`) - CRUD with GADT mapping support
6. **integrated** (`exposed-vertx-sql-client-integrated`) - Benchmarks and examples (not published)

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
- JDK 11 or higher (JDK 11 for compilation, CI tests with JDK 8 and 17)
- Internet connection for dependency downloads

**Setup:**
The Gradle wrapper (`./gradlew`) handles all tooling. No additional installation needed.

### Build Commands (In Order of Priority)

**ALWAYS use the Gradle wrapper (`./gradlew`) NOT `gradle`.**

1. **Clean Build** (recommended before first build or after major changes):
   ```bash
   ./gradlew clean
   ./gradlew build
   ```
   - Time: ~35-50 seconds for clean build (without daemon), ~2-5 seconds with daemon
   - What it does: Compiles all modules, runs tests (NO-SOURCE - no tests exist), validates API compatibility

2. **Check** (validation only - recommended for verifying changes):
   ```bash
   ./gradlew check
   ```
   - Time: ~45-50 seconds clean
   - What it does: Runs all verification tasks including `apiCheck`
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

**Current State:** This project has NO unit tests. The `test` task always shows `NO-SOURCE`.

**Benchmarks:** Located in `integrated/src/benchmarks/kotlin/` using kotlinx-benchmark:
```bash
./gradlew :exposed-vertx-sql-client-integrated:benchmark
```

**Quality Assurance:** The project's quality is validated by internal consuming projects (not in this repo).

## CI/CD Pipeline

**GitHub Actions Workflow:** `.github/workflows/kotlin-jvm-ci.yml`

Runs on every push to any branch:
1. **test-and-check** job:
   - Uses shared action: `huanshankeji/.github/actions/gradle-test-and-check@v0.2.0`
   - Tests with: JDK 8-temurin, JDK 17-temurin
   - Runs: `./gradlew check`

2. **dependency-submission** job:
   - Submits dependency graph to GitHub

**To replicate CI locally:**
```bash
./gradlew check
```

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

- **Exposed**: 0.56.0 (via `commonDependencies.exposed.*`)
- **Vert.x**: Managed by `vertx.platformStackDepchain()` (uses Vert.x BOM)
- **Kotlin**: 2.2.21
- **Arrow**: For functional constructs
- **exposed-gadt-mapping**: 0.4.0 (for mapper modules)

**Dependency Management:** All versions are centralized in `buildSrc/src/main/kotlin/VersionsAndDependencies.kt` using `common-gradle-dependencies` library.

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
- `vertx.sqlclient/` - Vert.x SQL Client abstractions
- `local/` - Local connection configuration helpers

**Database-specific packages:**
- `postgresql/` - In postgresql module
- `mysql/` - In mysql module

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
- Main config: `build.gradle.kts`, `settings.gradle.kts`
- CI config: `.github/workflows/kotlin-jvm-ci.yml`
- API definitions: `*/api/*.api` (generated, commit after running `apiDump`)

### When Things Go Wrong

1. **Build fails mysteriously**: Try `./gradlew clean build`
2. **API check fails**: Run `./gradlew apiDump` if the API change was intentional, then commit the updated `.api` files
3. **Configuration cache issues**: Delete `.gradle/` directory and rebuild
4. **Dependency resolution fails**: Check internet connection; mavenCentral() is the primary repository

## Trust These Instructions

These instructions are based on thorough exploration and testing of the repository. The commands listed have been validated to work correctly. Only search for additional information if:
- These instructions are incomplete for your specific task
- You encounter an error not documented here
- You need details about internal implementation not covered here

For questions or issues, refer to `CONTRIBUTING.md` or open a GitHub Discussion.
