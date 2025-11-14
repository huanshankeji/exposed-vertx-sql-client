@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.*
import com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlPool
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.vertx.core.Vertx
import io.vertx.sqlclient.Pool
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

object Examples : IntIdTable("examples") {
    val name = varchar("name", 64)
}

enum class DbType {
    POSTGRESQL,
    MYSQL
}

class ExamplesTest {
    private lateinit var container: JdbcDatabaseContainer<*>
    private lateinit var vertx: Vertx
    private lateinit var exposedDatabase: Database
    private lateinit var pool: Pool
    private lateinit var databaseClient: DatabaseClient<Pool>
    private lateinit var dbType: DbType

    private fun setUp(type: DbType) {
        dbType = type
        container = when (type) {
            DbType.POSTGRESQL -> PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
            DbType.MYSQL -> MySQLContainer(DockerImageName.parse("mysql:latest"))
        }
        container.start()

        val evscConfig = ConnectionConfig.Socket(
            container.host,
            container.firstMappedPort,
            container.username,
            container.password,
            container.databaseName
        ).toUniversalEvscConfig()

        vertx = Vertx.vertx()

        when (type) {
            DbType.POSTGRESQL -> {
                exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()
                pool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
                databaseClient = DatabaseClient(pool, exposedDatabase, PgDatabaseClientConfig())
            }

            DbType.MYSQL -> {
                exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectMysql()
                pool = createMysqlPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
                databaseClient = DatabaseClient(pool, exposedDatabase, MysqlDatabaseClientConfig())
            }
        }

        transaction(exposedDatabase) {
            SchemaUtils.create(Examples)
        }
    }

    private fun tearDown() {
        pool.close()
        vertx.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS)
        container.stop()
    }

    @Test
    fun testExamplesWithBuildStatementPostgreSQL() = runTest {
        setUp(DbType.POSTGRESQL)
        try {
            testExamplesWithBuildStatement()
        } finally {
            tearDown()
        }
    }

    @Test
    fun testExamplesWithBuildStatementMySQL() = runTest {
        setUp(DbType.MYSQL)
        try {
            testExamplesWithBuildStatement()
        } finally {
            tearDown()
        }
    }

    private suspend fun testExamplesWithBuildStatement() {
        val insertRowCount = databaseClient.executeUpdate(buildStatement { Examples.insert { it[name] = "A" } })
        assertEquals(1, insertRowCount)

        databaseClient.executeSingleUpdate(buildStatement { Examples.insert { it[name] = "B" } })

        val isInserted =
            databaseClient.executeSingleOrNoUpdate(buildStatement { Examples.insertIgnore { it[name] = "B" } })
        assertEquals(true, isInserted)

        val updateRowCount =
            databaseClient.executeUpdate(buildStatement { Examples.update({ Examples.id eq 1 }) { it[name] = "AA" } })
        assertEquals(1, updateRowCount)

        val exampleName = databaseClient.executeQuery(Examples.select(Examples.name).where(Examples.id eq 1))
            .single()[Examples.name]
        assertEquals("AA", exampleName)

        databaseClient.executeSingleUpdate(buildStatement { Examples.deleteWhere { id eq 1 } })
        databaseClient.executeSingleUpdate(buildStatement { Examples.deleteWhere { id eq 2 } })
    }

    @Test
    fun testExamplesWithCrudExtensionsPostgreSQL() = runTest {
        setUp(DbType.POSTGRESQL)
        try {
            testExamplesWithCrudExtensions()
        } finally {
            tearDown()
        }
    }

    @Test
    fun testExamplesWithCrudExtensionsMySQL() = runTest {
        setUp(DbType.MYSQL)
        try {
            testExamplesWithCrudExtensions(supportDeleteIgnore = true)
        } finally {
            tearDown()
        }
    }

    private suspend fun testExamplesWithCrudExtensions(supportDeleteIgnore: Boolean = false) {
        databaseClient.insert(Examples) { it[name] = "A" }
        val isInserted = databaseClient.insertIgnore(Examples) { it[name] = "B" }
        assertEquals(true, isInserted)

        val updateRowCount = databaseClient.update(Examples, { Examples.id eq 1 }) { it[name] = "AA" }
        assertEquals(1, updateRowCount)

        val exampleName1 =
            databaseClient.select(Examples) { select(Examples.name).where(Examples.id eq 1) }.single()[Examples.name]
        assertEquals("AA", exampleName1)

        val exampleName2 =
            databaseClient.selectSingleColumn(Examples, Examples.name) { where(Examples.id eq 2) }.single()
        assertEquals("B", exampleName2)

        val examplesExist = databaseClient.selectExpression(exists(Examples.selectAll()))
        assertEquals(true, examplesExist)

        val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
        assertEquals(1, deleteRowCount1)

        if (supportDeleteIgnore) {
            val deleteRowCount2 = databaseClient.deleteIgnoreWhere(Examples) { id eq 2 }
            assertEquals(1, deleteRowCount2)
        } else {
            val deleteRowCount2 = databaseClient.deleteWhere(Examples) { id eq 2 }
            assertEquals(1, deleteRowCount2)
        }
    }
}
