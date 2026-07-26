See [Copilot instructions](.github/copilot-instructions.md) for this repository.

Organization-wide standards and open-source library map: [@huanshankeji/.github general agent instructions](https://github.com/huanshankeji/.github/blob/main/docs/general-agent-instructions.md).

## Cursor Cloud specific instructions

This is a Kotlin/JVM Gradle library (Exposed + Vert.x SQL Client). Standard build/lint/test commands live in [`.github/copilot-instructions.md`](.github/copilot-instructions.md) (`./gradlew assemble`, `./gradlew apiCheck`, `./gradlew check`, `./gradlew test`). Notes below are only the non-obvious environment caveats.

- **JDK 11 is required to compile.** The Kotlin toolchain is pinned to `kotlin.jvmToolchain(11)`, but no toolchain download repository is configured, so Gradle needs a locally-installed JDK 11 (it auto-detects `/usr/lib/jvm`). The Gradle daemon itself runs fine on the default JDK 21. Without a JDK 11 present, builds fail with "Cannot find a Java installation ... matching languageVersion=11".
- **Integration tests need Docker.** `./gradlew check` / `./gradlew test` run the `integrated` module against Testcontainers (PostgreSQL, MySQL, Oracle, MSSQL). Start the daemon with `sudo service docker start` before running tests (it is not started automatically). Docker is configured with the `fuse-overlayfs` storage driver and the containerd snapshotter disabled.
- **Docker Hub blob egress is restricted.** Registry endpoints resolve, but image layer/config blobs are served from `docker-images-prod.s3.dualstack.us-east-1.amazonaws.com`, which is blocked by the default egress policy — so Testcontainers cannot pull `postgres`/`mysql`/`gvenzl/oracle-free` images. That S3 host must be added to the network allowlist to run the full Testcontainers suite. `mcr.microsoft.com` (MSSQL image) is reachable.
- **Quick end-to-end check without Docker:** a local PostgreSQL works for the core CRUD flow. Start it with `sudo service postgresql start` and create the role/db expected by `integrated` `Examples` (`evscConfig`): user `user`, password `password`, database `database` (owned by `user`) on `localhost:5432`. The library's CRUD examples (`crudWithStatements` / `crudExtensions` in `integrated/.../Examples.kt`) then run against it directly, bypassing Testcontainers.
- **Do not run `./gradlew apiDump` automatically** — see `.github/copilot-instructions.md` for the API-change workflow.
