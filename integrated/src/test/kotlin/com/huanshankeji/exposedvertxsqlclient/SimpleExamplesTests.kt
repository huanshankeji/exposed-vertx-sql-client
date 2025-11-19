package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlClient
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlConnection
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlPool
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgClient
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.vertx.core.Vertx

class SimpleExamplesTests : FunSpec({
    val vertx = Vertx.vertx()

    suspend fun FunSpecContainerScope.crudTests(databaseClient: DatabaseClient<*>) {
        test("test CRUD with Statements") {
            withTables { crudWithStatements(databaseClient) }
        }
        test("test CRUD extensions") {
            withTables { crudExtensions(databaseClient) }
        }
    }

    // TODO consider not running all tests against all kinds of `SqlClient`s to save some time
    context("PostgreSQL") {
        val postgresqlContainer = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
        val connectionConfig = postgresqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
        val databaseClientConfig = PgDatabaseClientConfig()
        context("SqlClient") {
            crudTests(DatabaseClient(createPgClient(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("Pool") {
            crudTests(DatabaseClient(createPgPool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTests(
                DatabaseClient(createPgConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
    context("MySQL") {
        val mysqlContainer = install(TestContainerSpecExtension(LatestMySQLContainer()))
        val connectionConfig = mysqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectMysql()
        val databaseClientConfig = MysqlDatabaseClientConfig()
        context("SqlClient") {
            crudTests(DatabaseClient(createMysqlClient(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("Pool") {
            crudTests(DatabaseClient(createMysqlPool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTests(
                DatabaseClient(createMysqlConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
})