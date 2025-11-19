package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.db2.Db2DatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.db2.exposed.exposedDatabaseConnectDb2
import com.huanshankeji.exposedvertxsqlclient.db2.vertx.db2client.createDb2Connection
import com.huanshankeji.exposedvertxsqlclient.db2.vertx.db2client.createDb2Pool
import com.huanshankeji.exposedvertxsqlclient.mssql.MssqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mssql.exposed.exposedDatabaseConnectMssql
import com.huanshankeji.exposedvertxsqlclient.mssql.vertx.mssqlclient.createMssqlConnection
import com.huanshankeji.exposedvertxsqlclient.mssql.vertx.mssqlclient.createMssqlPool
import com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlClient
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlConnection
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlPool
import com.huanshankeji.exposedvertxsqlclient.oracle.OracleDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.oracle.exposed.exposedDatabaseConnectOracle
import com.huanshankeji.exposedvertxsqlclient.oracle.vertx.oracleclient.createOracleConnection
import com.huanshankeji.exposedvertxsqlclient.oracle.vertx.oracleclient.createOraclePool
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
    afterSpec { vertx.close().await() }

    // This causes passing tests to fail. Not sure why.
    //testExecutionMode = TestExecutionMode.Concurrent

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
            // TODO Also consider closing the clients. This isn't a big issue now though.
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
    context("DB2") {
        val db2Container = install(TestContainerSpecExtension(LatestDb2Container()))
        val connectionConfig = db2Container.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectDb2()
        val databaseClientConfig = Db2DatabaseClientConfig()
        context("Pool") {
            crudTests(DatabaseClient(createDb2Pool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTests(
                DatabaseClient(createDb2Connection(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
    context("Oracle") {
        val oracleContainer = install(TestContainerSpecExtension(LatestOracleContainer()))
        val connectionConfig = oracleContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectOracle()
        val databaseClientConfig = OracleDatabaseClientConfig()
        context("Pool") {
            crudTests(DatabaseClient(createOraclePool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTests(
                DatabaseClient(createOracleConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
    context("MSSQL") {
        val mssqlContainer = install(TestContainerSpecExtension(LatestMssqlContainer()))
        val connectionConfig = mssqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectMssql()
        val databaseClientConfig = MssqlDatabaseClientConfig()
        context("Pool") {
            crudTests(DatabaseClient(createMssqlPool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTests(
                DatabaseClient(createMssqlConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig)
            )
        }
    }
})