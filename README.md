# Exposed Vert.x SQL Client

[![Maven Central](https://img.shields.io/maven-central/v/com.huanshankeji/exposed-vertx-sql-client-postgresql)](https://search.maven.org/artifact/com.huanshankeji/exposed-vertx-sql-client-postgresql)

[Exposed](https://github.com/JetBrains/Exposed) on top of [Vert.x Reactive SQL Client](https://github.com/eclipse-vertx/vertx-sql-client)

## Note

Only PostgreSQL with [Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) is currently supported.

## Experimental

This library is experimental now. The APIs are subject to change (especially those marked with `@ExperimentalEvscApi`), the tests are incomplete, and please expect bugs and report them.

## Add to your dependencies

### The Maven coordinates

```kotlin
"com.huanshankeji:exposed-vertx-sql-client-$module:$libraryVersion"
```

### **Important note**

As Exposed is a library that has not reached stability yet and often has incompatible changes, you are recommended to stick to the same version of Exposed used by this library. The current version is v0.56.0.

## API documentation

See the [hosted API documentation](https://huanshankeji.github.io/exposed-vertx-sql-client/) for the APIs.

## Basic usage guide

Here is a basic usage guide.

### Before v0.5.0

Add the PostgreSQL module, which was the only module, to your dependencies with the Gradle build script:

```kotlin
implementation("com.huanshankeji:exposed-vertx-sql-client-postgresql:$libraryVersion")
```

### Since v0.5.0

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
val sqlConnection = createPgClient(vertx, evscConfig.vertxSqlClientConnectionConfig)
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

### Use `exposedTransaction` to execute original blocking Exposed code

For example, to create tables:

```kotlin
databaseClient.exposedTransaction {
    SchemaUtils.create(*tables)
}
```

If you execute blocking Exposed statements inside `Verticle`s or event loop threads that you shouldn't block, you should use Vert.x `Vertx.executeBlocking` or Coroutines `Dispatchers.IO`.

### CRUD (DML and DQL) operations with `DatabaseClient`

#### Core APIs

With these core APIs, you create and execute Exposed `Statement`s. You don't need to learn many new APIs, and the
`Statement`s are more composable and easily editable. For example, you can move a query into an adapted subquery.

```kotlin
// The Exposed `Table` extension functions `insert`, `update`, and `delete` execute eagerly so `insertStatement`, `updateStatement`, `deleteStatement` have to be used.

val insertRowCount = databaseClient.executeUpdate(Examples.insertStatement { it[name] = "A" })
assert(insertRowCount == 1)
// `executeSingleUpdate` function requires that there is only 1 row updated and returns `Unit`.
databaseClient.executeSingleUpdate(Examples.insertStatement { it[name] = "B" })
// `executeSingleOrNoUpdate` requires that there is 0 or 1 row updated and returns `Boolean`.
val isInserted = databaseClient.executeSingleOrNoUpdate(Examples.insertIgnoreStatement { it[name] = "B" })
assert(isInserted)

val updateRowCount =
    databaseClient.executeUpdate(Examples.updateStatement({ Examples.id eq 1 }) { it[name] = "AA" })
assert(updateRowCount == 1)

// The Exposed `Table` extension function `select` doesn't execute eagerly so it can also be used directly.
val exampleName = databaseClient.executeQuery(Examples.selectStatement(Examples.name).where(Examples.id eq 1))
    .single()[Examples.name]

databaseClient.executeSingleUpdate(Examples.deleteWhereStatement { id eq 1 })
databaseClient.executeSingleUpdate(Examples.deleteIgnoreWhereStatement { id eq 2 })
```

#### Extension CRUD operations

the extension CRUD APIs are similar
to [those in Exposed](https://www.jetbrains.com/help/exposed/dsl-crud-operations.html). With them, your code becomes
more concise compared to using `buildStatement`, but it might be more difficult when you need to compose statements or
edit the code.

Gradle dependency configuration (only needed since v0.5.0):

```kotlin
implementation("com.huanshankeji:exposed-vertx-sql-client-crud:$libraryVersion")
```

Example code:

```kotlin
databaseClient.insert(Examples) { it[name] = "A" }
val isInserted = databaseClient.insertIgnore(Examples) { it[name] = "B" }

val updateRowCount = databaseClient.update(Examples, { Examples.id eq 1 }) { it[name] = "AA" }

val exampleName1 =
    databaseClient.select(Examples) { select(Examples.name).where(Examples.id eq 1) }.single()[Examples.name]
val exampleName2 =
    databaseClient.selectSingleColumn(Examples, Examples.name) { where(Examples.id eq 2) }.single()

val examplesExist = databaseClient.selectExpression(exists(Examples.selectAll()))

val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
assert(deleteRowCount1 == 1)
val deleteRowCount2 = databaseClient.deleteIgnoreWhere(Examples) { id eq 2 }
assert(deleteRowCount2 == 1)
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
val director = Director(directorId, "George Lucas")
databaseClient.insertWithMapper(Directors, director, Mappers.director)

val episodeIFilmDetails = FilmDetails(1, "Star Wars: Episode I – The Phantom Menace", directorId)
// insert without the ID since it's `AUTO_INCREMENT`
databaseClient.insertWithMapper(Films, episodeIFilmDetails, Mappers.filmDetailsWithDirectorId)

val filmId = 2
val episodeIIFilmDetails = FilmDetails(2, "Star Wars: Episode II – Attack of the Clones", directorId)
val filmWithDirectorId = FilmWithDirectorId(filmId, episodeIIFilmDetails)
databaseClient.insertWithMapper(Films, filmWithDirectorId, Mappers.filmWithDirectorId) // insert with the ID

val fullFilms = databaseClient.selectWithMapper(filmsLeftJoinDirectors, Mappers.fullFilm) {
    where(Films.filmId inList listOf(1, 2))
}
```

### About the code

Also see <https://github.com/huanshankeji/kotlin-common/tree/main/exposed> for some dependency code which serves this library.
