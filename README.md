# Exposed Vert.x SQL Client

[![Maven Central](https://img.shields.io/maven-central/v/com.huanshankeji/exposed-vertx-sql-client-postgresql)](https://search.maven.org/artifact/com.huanshankeji/exposed-vertx-sql-client-postgresql)

[Exposed](https://github.com/JetBrains/Exposed) on top of [Vert.x Reactive SQL Client](https://github.com/eclipse-vertx/vertx-sql-client)

## Note

Only PostgreSQL with [Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) is currently supported.

## Maven coordinate

```kotlin
"com.huanshankeji:exposed-vertx-sql-client-postgresql:$version"
```

## Basic usage guide

Here is a basic usage guide. This project currently serves our own use, therefore, there are temporarily no detailed docs, APIs are experimental, tests are incomplete, and please expect bugs. To learn more in addition to the guide below, see the [hosted API documentation](https://huanshankeji.github.io/exposed-vertx-sql-client/), and see [DatabaseClient.kt](lib/src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/DatabaseClient.kt) and [DatabaseClientSql.kt](lib/src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/sql/DatabaseClientSql.kt) for the major APIs.

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

Create an Exposed `Database` with the `ConnectionConfig`, which can be reused for multiple `Verticle`s:

```kotlin
val exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()
```

Create a Vert.x `SqlClient` with the `ConnectionConfig`, preferably in a `Verticle`:

```kotlin
val vertxPool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
```

Create a `Database` with the provided Vert.x `SqlClient` and Exposed `Database`, preferably in a `Verticle`:

```kotlin
val databaseClient = DatabaseClient(vertxPool, exposedDatabase)
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
withContext(Dispatchers.IO) {
    databaseClient.exposedTransaction {
        SchemaUtils.create(*tables)
    }
}
```

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

// The Exposed `Table` extension function `select` doesn't execute eagerly so it can be used directly.
val exampleName = databaseClient.executeQuery(Examples.select(Examples.name).where(Examples.id eq 1))
    .single()[Examples.name]

databaseClient.executeSingleUpdate(Examples.deleteWhereStatement { Examples.id eq 1 }) // The function `deleteWhereStatement` still depends on the old DSL and will be updated.
databaseClient.executeSingleUpdate(Examples.deleteIgnoreWhereStatement { id eq 2 })
```

#### Extension SQL DSL APIs

With these extension APIs, your code becomes more concise, but it might be more difficult when you need to compose statements or edit the code:

```kotlin
databaseClient.insert(Examples) { it[name] = "A" }
val isInserted = databaseClient.insertIgnore(Examples) { it[name] = "B" }

val updateRowCount = databaseClient.update(Examples, { Examples.id eq 1 }) { it[name] = "AA" }

val exampleName1 =
    databaseClient.select(Examples) { select(Examples.name).where(Examples.id eq 1) }.single()[Examples.name]
// This function still depends on the old SELECT DSL and will be updated.
val exampleName2 =
    databaseClient.selectSingleColumn(Examples, Examples.name) { where(Examples.id eq 2) }.single()

val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
assert(deleteRowCount1 == 1)
val deleteRowCount2 = databaseClient.deleteIgnoreWhere(Examples) { id eq 2 }
assert(deleteRowCount2 == 1)
```

#### APIs using [Exposed GADT mapping](https://github.com/huanshankeji/exposed-adt-mapping)

Please read [that library's basic usage guide](https://github.com/huanshankeji/exposed-adt-mapping?tab=readme-ov-file#basic-usage-guide) first. Here are examples of this library that correspond to [that library's CRUD operations](https://github.com/huanshankeji/exposed-adt-mapping?tab=readme-ov-file#crud-operations).

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
    where(Films.filmId inList listOf(1, 2)) // This API still depends on the old SELECT DSL and will be refactored.
}
```

### About the code

Also see <https://github.com/huanshankeji/kotlin-common/tree/main/exposed> for some dependency code which serves this library.
