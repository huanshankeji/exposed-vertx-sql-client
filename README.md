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

Here is a basic usage guide. This project currently serves our own use and there are temporarily no detailed docs. To learn more, see [DatabaseClient.kt](lib/src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/DatabaseClient.kt) and [DatabaseClientSql.kt](lib/src/main/kotlin/com/huanshankeji/exposedvertxsqlclient/sql/DatabaseClientSql.kt) for the major APIs.

### Create a `DatabaseClient`

```kotlin
val socketConnectionConfig =
    ConnectionConfig.Socket("localhost", user = "user", password = "password", database = "database")
val exposedDatabase = exposedDatabaseConnectPostgreSql(socketConnectionConfig)
val databaseClient = createPgPoolDatabaseClient(
    vertx, socketConnectionConfig, exposedDatabase = exposedDatabase
)
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

### CRUD (DDL and DQL) operations with `DatabaseClient`

#### Core APIs

With these core APIs, you create and execute Exposed `Statement`s. You don't need to learn many new APIs, and the `Statement`s are more composable and easily editable.

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
    databaseClient.selectSingleColumn(Examples, Examples.name) { selectAll().where(Examples.id eq 2) }.single()

val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
assert(deleteRowCount1 == 1)
val deleteRowCount2 = databaseClient.deleteIgnoreWhere(Examples) { id eq 2 }
assert(deleteRowCount2 == 1)
```

### About the code

Also see <https://github.com/huanshankeji/kotlin-common/tree/main/exposed> for some dependency code which serves this library.
