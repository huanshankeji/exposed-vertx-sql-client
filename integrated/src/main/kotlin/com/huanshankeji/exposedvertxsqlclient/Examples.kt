@file:OptIn(ExperimentalEvscApi::class, ExperimentalUnixDomainSocketApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.*
import com.huanshankeji.exposedvertxsqlclient.local.toPerformantUnixEvscConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.local.defaultPostgresqlLocalConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgClient
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.sqlclient.SqlClient
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Examples : IntIdTable("examples") {
    val name = varchar("name", 64)
}

val tables = arrayOf(Examples)

val evscConfig = ConnectionConfig.Socket("localhost", user = "user", password = "password", database = "database")
    .toUniversalEvscConfig()

object Alternatives {
    // Unix domain socket alternative
    val evscConfig = defaultPostgresqlLocalConnectionConfig(
        user = "user",
        socketConnectionPassword = "password",
        database = "database"
    ).toPerformantUnixEvscConfig()

    val connectionConfig =
        ConnectionConfig.Socket("localhost", user = "user", password = "password", database = "database")
}

suspend fun examples(vertx: Vertx) {
    /** It may be more efficient to reuse a single shared [Database] to generate SQLs in multiple [DatabaseClient]s for [SqlClient]s in respective [Verticle]s. */
    val exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()

    val sqlClient = createPgClient(vertx, evscConfig.vertxSqlClientConnectionConfig)
    val pool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
    val sqlConnection = createPgConnection(vertx, evscConfig.vertxSqlClientConnectionConfig)

    val vertxSqlClient = sqlClient

    val databaseClient = DatabaseClient(vertxSqlClient, exposedDatabase, PgDatabaseClientConfig())

    // put in `Vertx.executeBlocking` or `Dispatchers.IO` if needed
    createTablesWithExposedTransaction()

    val crudSupportConfig = CrudSupportConfig.POSTGRESQL
    crudWithStatements(databaseClient, crudSupportConfig)
    crudExtensions(databaseClient, crudSupportConfig)
}

// for copying to README.md
private fun allWaysOfCreatingTables(databaseClient: DatabaseClient<SqlClient>, exposedDatabase: Database) {
    // put in `Vertx.executeBlocking` or `Dispatchers.IO` if needed

    databaseClient.exposedTransaction {
        SchemaUtils.create(*tables)
    }

    transaction(exposedDatabase) {
        SchemaUtils.create(*tables)
    }

    transaction {
        SchemaUtils.create(*tables)
    }
}

fun createTablesWithExposedTransaction() =
    transaction {
        SchemaUtils.create(*tables)
    }

fun dropTablesWithExposedTransaction() =
    transaction {
        SchemaUtils.drop(*tables)
    }

// mainly for tests
suspend fun withTables(block: suspend () -> Unit) {
    createTablesWithExposedTransaction()
    try {
        block()
    } finally {
        dropTablesWithExposedTransaction()
    }
}

data class CrudSupportConfig(
    val dialectSupportsInsertIgnore: Boolean,
    val dialectSupportsDeleteIgnore: Boolean,
    val dialectSupportsExists: Boolean = true
) {
    companion object {
        val POSTGRESQL = CrudSupportConfig(true, false)
        val MYSQL = CrudSupportConfig(true, true)
        val ORACLE = CrudSupportConfig(false, false)
        val MSSQL = CrudSupportConfig(false, false, false)
        fun fromRdbmsType(rdbmsType: RdbmsType) =
            when (rdbmsType) {
                RdbmsType.Postgresql -> POSTGRESQL
                RdbmsType.Mysql -> MYSQL
                RdbmsType.Oracle -> ORACLE
                RdbmsType.Mssql -> MSSQL
            }
    }
}

suspend fun crudWithStatements(
    databaseClient: DatabaseClient<*>, crudSupportConfig: CrudSupportConfig
) = with(crudSupportConfig) {
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
}

@OptIn(ExperimentalEvscApi::class)
suspend fun crudExtensions(
    databaseClient: DatabaseClient<*>, crudSupportConfig: CrudSupportConfig
) = with(crudSupportConfig) {
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
}
