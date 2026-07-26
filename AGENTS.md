See [Copilot instructions](.github/copilot-instructions.md) for this repository.

Organization-wide standards and open-source library map: [@huanshankeji/.github general agent instructions](https://github.com/huanshankeji/.github/blob/main/docs/general-agent-instructions.md).

## Cursor Cloud specific instructions

This is a Kotlin JVM (Gradle) library. Standard build/lint/test commands are in
[Copilot instructions](.github/copilot-instructions.md) (`./gradlew check`, `./gradlew apiCheck`,
`./gradlew test`, `./gradlew publishToMavenLocal`). Notes below are the non-obvious Cloud caveats only.

### JDK toolchain
- Gradle requires a **JDK 11 toolchain** (`kotlin.jvmToolchain(11)`); CI also uses 17. JDK 11, 17 and 21
  are installed on the VM. The default `java` on `PATH` is 21, which is fine — Gradle auto-detects the
  JDK 11 toolchain from `/usr/lib/jvm`. No `JAVA_HOME` override is needed.

### Docker / Testcontainers (integration tests)
- `./gradlew test` / `check` run Kotest + Testcontainers integration tests that need a Docker daemon.
- Docker is installed but is **not** a running service on boot. Start it once per session:
  `sudo dockerd > /tmp/dockerd.log 2>&1 &` (it is configured for the `fuse-overlayfs` storage driver
  with the containerd snapshotter disabled in `/etc/docker/daemon.json`; required for this VM).
- Run Gradle with `sudo -E` (or as root) so it can reach `/var/run/docker.sock`.
- Set `TESTCONTAINERS_RYUK_DISABLED=true`: the Ryuk cleanup image lives on Docker Hub whose blob CDN is
  blocked here (see egress note), so leaving Ryuk enabled makes container startup fail.
- Every `AllConfigurationsSpec` subclass eagerly starts **all four** DB containers (PostgreSQL, MySQL,
  Oracle, MSSQL) in `beforeSpec`, so a single spec cannot be narrowed to one DB via Gradle `--tests`.

### Egress limitation for container images (important)
- Network egress is allowlist-based. Registry indexes are reachable but most image **blob CDNs are
  blocked** (connection reset): Docker Hub blobs (`docker-images-prod.s3.dualstack.us-east-1.amazonaws.com`)
  and MCR blobs (`*.data.mcr.microsoft.com`). `public.ecr.aws` blobs **are** reachable.
- Workaround for the two Docker Hub official images used by the tests — pull from the AWS ECR public
  mirror and retag to the exact names Testcontainers expects:
  - `docker pull public.ecr.aws/docker/library/postgres:latest && docker tag public.ecr.aws/docker/library/postgres:latest postgres:latest`
  - `docker pull public.ecr.aws/docker/library/mysql:latest && docker tag public.ecr.aws/docker/library/mysql:latest mysql:latest`
- With the two images above, PostgreSQL and MySQL integration tests run end-to-end.
- `gvenzl/oracle-free:latest` (Oracle, Docker Hub community image) and `mcr.microsoft.com/mssql/server:2022-latest`
  (MSSQL) have **no reachable source** under the current allowlist, so the full `./gradlew test` / `check`
  (which starts all four DBs) cannot pass until `docker-images-prod.s3.dualstack.us-east-1.amazonaws.com`
  and `*.data.mcr.microsoft.com` are allowlisted (then plain `docker pull` works for every image and the
  ECR retag workaround / Ryuk-disable are no longer needed).
