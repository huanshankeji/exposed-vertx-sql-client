# Exposed Vert.x SQL Client

[![Maven Central](https://img.shields.io/maven-central/v/com.huanshankeji/exposed-vertx-sql-client-postgresql)](https://search.maven.org/artifact/com.huanshankeji/exposed-vertx-sql-client-postgresql)

[Exposed](https://github.com/JetBrains/Exposed) on top of [Vert.x Reactive SQL Client](https://github.com/eclipse-vertx/vertx-sql-client)

## Note

This project currently serves our own use and there are temporarily no guides or docs. See [DatabaseClient.kt](lib/src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/DatabaseClient.kt) for the major APIs.

Only PostgreSQL with [Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) is currently supported.

## Maven coordinate

```kotlin
"com.huanshankeji:exposed-vertx-sql-client-postgresql:$version"
```

## About the code

Also see https://github.com/huanshankeji/kotlin-common/tree/main/exposed for some dependency code which serves this
library.
