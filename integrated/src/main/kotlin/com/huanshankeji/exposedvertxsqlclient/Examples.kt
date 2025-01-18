@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposed.*
import com.huanshankeji.exposedvertxsqlclient.local.toPerformantUnixEvscConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.mysql.local.defaultPostgresqlLocalConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createPgPool
import com.huanshankeji.exposedvertxsqlclient.sql.*
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.selectAll

object Examples : IntIdTable("examples") {
    val name = varchar("name", 64)
}

val tables = arrayOf(Examples)

val evscConfig = ConnectionConfig.Socket("localhost", user = "user", password = "password", database = "database")
    .toUniversalEvscConfig()

object Alternative {
    // Unix domain socket alternative
    val evscConfig = defaultPostgresqlLocalConnectionConfig(
        user = "user",
        socketConnectionPassword = "password",
        database = "database"
    ).toPerformantUnixEvscConfig()
}

@OptIn(ExperimentalEvscApi::class)
suspend fun examples(vertx: Vertx) {
    /** It may be more efficient to use a single shared [Database] to generate SQLs for multiple [DatabaseClient]s/[SqlClient]s in respective [Verticle]s. */
    val exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()
    val vertxPool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
    val databaseClient = DatabaseClient(vertxPool, exposedDatabase)

    withContext(Dispatchers.IO) {
        databaseClient.exposedTransaction {
            SchemaUtils.create(*tables)
        }
    }

    run {
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
    }

    run {
        databaseClient.insert(Examples) { it[name] = "A" }
        val isInserted = databaseClient.insertIgnore(Examples) { it[name] = "B" }

        val updateRowCount = databaseClient.update(Examples, { Examples.id eq 1 }) { it[name] = "AA" }

        val exampleName1 =
            databaseClient.select(Examples) { select(Examples.name).where(Examples.id eq 1) }.single()[Examples.name]
        // This function still depends on the old SELECT DSL and will be updated.
        val exampleName2 =
            databaseClient.selectSingleColumn(Examples, Examples.name) { where(Examples.id eq 2) }.single()

        val examplesExist = databaseClient.selectExpression(exists(Examples.selectAll()))

        val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
        assert(deleteRowCount1 == 1)
        val deleteRowCount2 = databaseClient.deleteIgnoreWhere(Examples) { id eq 2 }
        assert(deleteRowCount2 == 1)
    }
}