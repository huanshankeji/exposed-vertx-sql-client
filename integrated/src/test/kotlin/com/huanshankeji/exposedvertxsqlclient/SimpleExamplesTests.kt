package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlClient
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgClient
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.vertx.core.Vertx

class SimpleExamplesTests : StringSpec({
    val vertx: Vertx? = null

    fun crudTests(databaseClient: DatabaseClient<*>) {
        "test CRUD with Statements" {
            withTables { crudWithStatements(databaseClient) }
        }
        "test CRUD Extensions" {
            crudExtensions(databaseClient)
        }
    }

    "PostgreSQL" {
        val postgresqlContainer = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
        val connectionConfig = postgresqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
        val databaseClientConfig = PgDatabaseClientConfig()
        "SQLClient" {
            crudTests(DatabaseClient(createPgClient(vertx, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        "Pool" {
            crudTests(DatabaseClient(createPgPool(vertx, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        "SqlConnection" {
            crudTests(
                DatabaseClient(createPgConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
    "MySQL" {
        val mysqlContainer = install(TestContainerSpecExtension(LatestMySQLContainer()))
        val connectionConfig = mysqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectMysql()
        val databaseClientConfig = MysqlDatabaseClientConfig()
        "SQLClient" {
            crudTests(DatabaseClient(createMysqlClient(vertx, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        "Pool" {
            crudTests(DatabaseClient(createMysqlClient(vertx, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        "SqlConnection" {
            crudTests(
                DatabaseClient(createMysqlClient(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
})