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

class SimpleMappingTests : FunSpec({
    val vertx = Vertx.vertx()
    afterSpec { vertx.close().await() }

    suspend fun FunSpecContainerScope.crudTest(databaseClient: DatabaseClient<*>) =
        test("test CRUD mapper Extensions") {
            withMappingTables { crudMapperExtensions(databaseClient) }
        }

    context("PostgreSQL") {
        val postgresqlContainer = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
        val connectionConfig = postgresqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
        val databaseClientConfig = PgDatabaseClientConfig()
        context("SqlClient") {
            crudTest(DatabaseClient(createPgClient(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("Pool") {
            crudTest(DatabaseClient(createPgPool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTest(DatabaseClient(createPgConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig))
        }
    }
    context("MySQL") {
        val mysqlContainer = install(TestContainerSpecExtension(LatestMySQLContainer()))
        val connectionConfig = mysqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectMysql()
        val databaseClientConfig = MysqlDatabaseClientConfig()
        context("SqlClient") {
            crudTest(DatabaseClient(createMysqlClient(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("Pool") {
            crudTest(DatabaseClient(createMysqlPool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTest(
                DatabaseClient(
                    createMysqlConnection(vertx, connectionConfig),
                    exposedDatabase,
                    databaseClientConfig
                )
            )
        }
    }
})