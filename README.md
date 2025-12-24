# Exposed Vert.x SQL Client

[![Maven Central](https://img.shields.io/maven-central/v/com.huanshankeji/exposed-vertx-sql-client-postgresql)](https://search.maven.org/artifact/com.huanshankeji/exposed-vertx-sql-client-postgresql)
[![CI](https://github.com/huanshankeji/exposed-vertx-sql-client/actions/workflows/kotlin-jvm-ci.yml/badge.svg)](https://github.com/huanshankeji/exposed-vertx-sql-client/actions/workflows/kotlin-jvm-ci.yml)
[![codecov](https://codecov.io/gh/huanshankeji/exposed-vertx-sql-client/graph/badge.svg)](https://codecov.io/gh/huanshankeji/exposed-vertx-sql-client)

<!-- [Exposed](https://github.com/JetBrains/Exposed) on top of [Vert.x Reactive SQL Client](https://github.com/eclipse-vertx/vertx-sql-client) -->
Execute [Exposed](https://github.com/JetBrains/Exposed) statements with [Vert.x Reactive SQL Client](https://github.com/eclipse-vertx/vertx-sql-client)

## Supported DBs

- PostgreSQL with [Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) and [Exposed PostgreSQL support](https://www.jetbrains.com/help/exposed/working-with-database.html#postgresql)
- MySQL with [Reactive MySQL Client](https://vertx.io/docs/vertx-mysql-client/java/) and [Exposed MySQL support](https://www.jetbrains.com/help/exposed/working-with-database.html#mysql)
- Oracle with [Reactive Oracle Client](https://vertx.io/docs/vertx-oracle-client/java/) and [Exposed Oracle support](https://www.jetbrains.com/help/exposed/working-with-database.html#oracle)
- Microsoft SQL Server with [Reactive MSSQL Client](https://vertx.io/docs/vertx-mssql-client/java/) and [Exposed SQL Server support](https://www.jetbrains.com/help/exposed/working-with-database.html#sql-server)

## Experimental

This library is experimental now.
The APIs are subject to change (especially those marked with `@ExperimentalEvscApi`).
There are some basic tests, but they are incomplete to cover all the APIs, so please expect bugs and report them.
We also have some internal consuming code to guarantee the usability of the APIs.

## Exposed DAO APIs are not supported

## Add to your dependencies

### The Maven coordinates

```kotlin
"com.huanshankeji:exposed-vertx-sql-client-$module:$libraryVersion"
```

### **Important note : compatibility with Exposed**

If you encounter issues likely caused by compatibility with Exposed, please try using the same version of Exposed this library depends on. The current Exposed version for v0.6.0 of this library is v1.0.0-rc-3.

## API documentation

See the [hosted API documentation](https://huanshankeji.github.io/exposed-vertx-sql-client/) for the APIs.

## Basic usage guide

Here is a basic usage guide (since v0.5.0).

### Add the dependencies

Add the core module to your dependencies with the Gradle build script:

```kotlin
implementation("com.huanshankeji:exposed-vertx-sql-client-core:$libraryVersion")
```

And add an RDBMS module, for example, the PostgreSQL module:

```kotlin
implementation("com.huanshankeji:exposed-vertx-sql-client-postgresql:$libraryVersion")
```

### Create a `DatabaseClient`

Create an `EvscConfig` as the single source of truth:

```kotlin
val evscConfig = ConnectionConfig.Socket("localhost", user = "user", password = "password", database = "database")
    .toUniversalEvscConfig()
```

Local alternative with Unix domain socket:

```kotlin
val evscConfig = defaultPostgresqlLocalConnectionConfig(
    user = "user",
    socketConnectionPassword = "password",
    database = "database"
).toPerformantUnixEvscConfig()
```

Create an Exposed `Database` with the `ConnectionConfig.Socket`, which can be shared and reused in multiple `Verticle`s:

```kotlin
val exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()
```

Create a Vert.x `SqlClient` with the `ConnectionConfig`, preferably in a `Verticle`:

```kotlin
val sqlClient = createPgClient(vertx, evscConfig.vertxSqlClientConnectionConfig)
val pool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
val sqlConnection = createPgConnection(vertx, evscConfig.vertxSqlClientConnectionConfig)
```

Create a `Database` with the provided Vert.x `SqlClient` and Exposed `Database`, preferably in a `Verticle`:

```kotlin
val databaseClient = DatabaseClient(vertxSqlClient, exposedDatabase, PgDatabaseClientConfig())
```

### Example table definitions

```kotlin
object Examples : IntIdTable("examples") {
    val name = varchar("name", 64)
}

val tables = arrayOf(Examples)
```

### Use `exposedTransaction` or `transaction` from Exposed to execute original blocking Exposed code

For example, to create tables:

```kotlin
databaseClient.exposedTransaction {
    SchemaUtils.create(*tables)
}
```

You can also use the `transaction` API from Exposed directly:

```kotlin
transaction(exposedDatabase) {
    SchemaUtils.create(*tables)
}
```

Or use the thread-local `Database` instance implicitly following Exposed conventions:

```kotlin
transaction {
    SchemaUtils.create(*tables)
}
```

If you execute blocking Exposed statements inside `Verticle`s or event loop threads that you shouldn't block, you should use Vert.x `Vertx.executeBlocking` or Coroutines `Dispatchers.IO`.

### CRUD (DML and DQL) operations with `DatabaseClient`

#### Core APIs

With these core APIs, you create and execute Exposed `Statement`s. You don't need to learn many new APIs, and the
`Statement`s are more composable and easily editable. For example, you can move a query into an adapted subquery.

```kotlin
val insertRowCount = databaseClient.executeUpdate(buildStatement { Examples.insert { it[name] = "A" } })
assert(insertRowCount == 1)
// `executeSingleUpdate` function requires that there is only 1 row updated and returns `Unit`.
databaseClient.executeSingleUpdate(buildStatement { Examples.insert { it[name] = "B" } })
// `executeSingleOrNoUpdate` requires that there is 0 or 1 row updated and returns `Boolean`.
val isInserted = if (dialectSupportsInsertIgnore)
    databaseClient.executeSingleOrNoUpdate(buildStatement { Examples.insertIgnore { it[name] = "B" } })
else
    databaseClient.executeSingleOrNoUpdate(buildStatement { Examples.insert { it[name] = "B" } })
assert(isInserted)

val updateRowCount =
    databaseClient.executeUpdate(buildStatement { Examples.update({ Examples.id eq 1 }) { it[name] = "AA" } })
assert(updateRowCount == 1)

// The Exposed `Table` extension function `select` doesn't execute eagerly so it can also be used directly.
val exampleName = databaseClient.executeQuery(Examples.select(Examples.name).where(Examples.id eq 1))
    .single()[Examples.name]
assert(exampleName == "AA")

databaseClient.executeSingleUpdate(buildStatement { Examples.deleteWhere { id eq 1 } })
if (dialectSupportsDeleteIgnore) {
    val isDeleted =
        databaseClient.executeSingleOrNoUpdate(buildStatement { Examples.deleteIgnoreWhere { id eq 2 } })
    assert(isDeleted)
}
```

#### Extension CRUD operations

The extension CRUD APIs are similar to [those in Exposed](https://www.jetbrains.com/help/exposed/dsl-crud-operations.html).
With them, your code becomes more concise compared to using `buildStatement`,
but it might be more difficult when you need to compose statements or edit the code.

Gradle dependency configuration:

```kotlin
implementation("com.huanshankeji:exposed-vertx-sql-client-crud:$libraryVersion")
```

Example code:

```kotlin
databaseClient.insert(Examples) { it[name] = "A" }
if (dialectSupportsInsertIgnore) {
    val isInserted = databaseClient.insertIgnore(Examples) { it[name] = "B" }
    assert(isInserted)
} else
    databaseClient.insert(Examples) { it[name] = "B" }

val updateRowCount = databaseClient.update(Examples, { Examples.id eq 1 }) { it[name] = "AA" }
assert(updateRowCount == 1)

val exampleName1 =
    databaseClient.select(Examples, { select(Examples.name).where(Examples.id eq 1) }).single()[Examples.name]
assert(exampleName1 == "AA")
val exampleName2 =
    databaseClient.selectSingleColumn(Examples, Examples.name, { where(Examples.id eq 2) }).single()
assert(exampleName2 == "B")

if (dialectSupportsExists) {
    val examplesExist = databaseClient.selectExpression(exists(Examples.selectAll()))
    assert(examplesExist)
}

val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
assert(deleteRowCount1 == 1)

if (dialectSupportsDeleteIgnore) {
    val deleteRowCount2 = databaseClient.deleteIgnoreWhere(Examples) { id eq 2 }
    assert(deleteRowCount2 == 1)
}
```

#### Extension CRUD APIs with [Exposed GADT mapping](https://github.com/huanshankeji/exposed-gadt-mapping)

Please read [that library's basic usage guide](https://github.com/huanshankeji/exposed-gadt-mapping?tab=readme-ov-file#basic-usage-guide) first. Here are examples of this library that correspond to [that library's CRUD operations](https://github.com/huanshankeji/exposed-gadt-mapping?tab=readme-ov-file#crud-operations).

Gradle dependency configuration (only needed since v0.5.0):

```kotlin
implementation("com.huanshankeji:exposed-vertx-sql-client-crud-with-mapper:$libraryVersion")
```

Example code:

```kotlin
val directorId = 1
val directorDetails = DirectorDetails("George Lucas")
databaseClient.insertWithMapper(Directors, directorDetails, Mappers.directorDetails)

val episodeIFilmDetails = FilmDetails(1, "Star Wars: Episode I – The Phantom Menace", directorId)
// insert without the ID since it's `AUTO_INCREMENT`
databaseClient.insertWithMapper(Films, episodeIFilmDetails, Mappers.filmDetailsWithDirectorId)

val filmId = 2
val episodeIIFilmDetails = FilmDetails(2, "Star Wars: Episode II – Attack of the Clones", directorId)
val filmWithDirectorId = FilmWithDirectorId(filmId, episodeIIFilmDetails)
if (dialectSupportsIdentityInsert)
    databaseClient.insertWithMapper(Films, filmWithDirectorId, Mappers.filmWithDirectorId) // insert with the ID
else
    databaseClient.insertWithMapper(Films, episodeIIFilmDetails, Mappers.filmDetailsWithDirectorId)

val fullFilms = databaseClient.selectWithMapper(filmsLeftJoinDirectors, Mappers.fullFilm) {
    where(Films.filmId inList listOf(1, 2))
}
assert(fullFilms.size() == 2)
```

### Common issues

#### "No transaction in context."

If you encounter
`java.lang.IllegalStateException: No transaction in context.` in your code, inspect the exception stacktrace and try these options:

1. wrap the `Statement` creation call with `databaseClient.statementPreparationExposedTransaction { ... }`.

   For example, this can happen if you call `Query.forUpdate()` without a transaction. In such a case, you can also use our `Query.forUpdateWithTransaction()` instead.

2. If your function call has a parameter with `WithExposedTransaction` in its name, try setting it to `true`. To make things easier, you can also set `autoExposedTransaction` to `true` in `DatabaseClientConfig` when creating the `DatabaseClient`. Note that this slightly degrades performance though.

Some Exposed APIs implicitly require a transaction and we can't guarantee that such exceptions are always avoided, as Exposed APIs are not fully decoupled and designed to serve this library, the transaction requirements in APIs sometimes change between versions and our APIs may need to evolve accordingly.
