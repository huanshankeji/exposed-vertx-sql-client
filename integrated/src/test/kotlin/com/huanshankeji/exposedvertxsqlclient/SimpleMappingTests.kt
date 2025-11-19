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
    context("Oracle") {
        val oracleContainer = install(TestContainerSpecExtension(LatestOracleContainer()))
        val connectionConfig = oracleContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectOracle()
        val databaseClientConfig = OracleDatabaseClientConfig()
        context("Pool") {
            crudTest(DatabaseClient(createOraclePool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTest(DatabaseClient(createOracleConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig))
        }
    }
    context("MSSQL") {
        val mssqlContainer = install(TestContainerSpecExtension(LatestMssqlContainer()))
        val connectionConfig = mssqlContainer.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectMssql()
        val databaseClientConfig = MssqlDatabaseClientConfig()
        context("Pool") {
            crudTest(DatabaseClient(createMssqlPool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTest(
                DatabaseClient(
                    createMssqlConnection(vertx, connectionConfig),
                    exposedDatabase,
                    databaseClientConfig
                )
            )
        }
    }
    context("DB2") {
        val db2Container = install(TestContainerSpecExtension(LatestDb2Container()))
        val connectionConfig = db2Container.connectionConfig()
        val exposedDatabase = connectionConfig.exposedDatabaseConnectDb2()
        val databaseClientConfig = Db2DatabaseClientConfig()
        context("Pool") {
            crudTest(DatabaseClient(createDb2Pool(null, connectionConfig), exposedDatabase, databaseClientConfig))
        }
        context("SqlConnection") {
            crudTest(
                DatabaseClient(
                    createDb2Connection(vertx, connectionConfig),
                    exposedDatabase,
                    databaseClientConfig
                )
            )
        }
    }
})