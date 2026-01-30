package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.sqlServerJdbcUrlWithEncryptEqFalse
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
import java.util.*

@OptIn(ExperimentalEvscApi::class)
abstract class TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers(
    tests: suspend FunSpecContainerScope.(databaseClient: DatabaseClient<*>, rdbmsType: RdbmsType, sqlClientType: SqlClientType) -> Unit,
    // for disabling some tests for debugging
    enabledRdbmsTypes: EnumSet<RdbmsType> = EnumSet.allOf(RdbmsType::class.java),
    //extraPgConnectOptions: PgConnectOptions.() -> Unit = {}
    enabledSqlClientTypes: EnumSet<SqlClientType> = EnumSet.allOf(SqlClientType::class.java)
) : FunSpec({
    val vertx = Vertx.vertx()
    afterSpec { vertx.close().await() }

    // This causes passing tests to fail. Not sure why.
    //testExecutionMode = TestExecutionMode.Concurrent

    // TODO consider not running all tests against all kinds of `SqlClient`s to save some time
    if (RdbmsType.Postgresql in enabledRdbmsTypes)
        context("PostgreSQL") {
            val postgresqlContainer = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
            val connectionConfig = postgresqlContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
            val databaseClientConfig = PgDatabaseClientConfig(exposedDatabase)
            suspend fun FunSpecContainerScope.tests(databaseClient: DatabaseClient<*>, sqlClientType: SqlClientType) =
                tests(databaseClient, RdbmsType.Postgresql, sqlClientType)
            if (SqlClientType.Client in enabledSqlClientTypes)
                context("Client") {
                    // TODO Also consider closing the clients. This isn't a big issue now though.
                    tests(
                        DatabaseClient(
                            createPgClient(null, connectionConfig), databaseClientConfig
                        ),
                        SqlClientType.Client
                    )
                }
            if (SqlClientType.Pool in enabledSqlClientTypes)
                context("Pool") {
                    tests(
                        DatabaseClient(
                            createPgPool(null, connectionConfig), databaseClientConfig
                        ),
                        SqlClientType.Pool
                    )
                }
            if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                context("SqlConnection") {
                    tests(
                        DatabaseClient(
                            createPgConnection(vertx, connectionConfig), databaseClientConfig
                        ),
                        SqlClientType.SqlConnection
                    )
                }
        }
    if (RdbmsType.Mysql in enabledRdbmsTypes)
        context("MySQL") {
            val mysqlContainer = install(TestContainerSpecExtension(LatestMySQLContainer()))
            val connectionConfig = mysqlContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectMysql()
            val databaseClientConfig = MysqlDatabaseClientConfig(exposedDatabase)
            suspend fun FunSpecContainerScope.tests(databaseClient: DatabaseClient<*>, sqlClientType: SqlClientType) =
                tests(databaseClient, RdbmsType.Mysql, sqlClientType)
            if (SqlClientType.Client in enabledSqlClientTypes)
                context("Client") {
                    tests(
                        DatabaseClient(
                            createMysqlClient(null, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.Client
                    )
                }
            if (SqlClientType.Pool in enabledSqlClientTypes)
                context("Pool") {
                    tests(
                        DatabaseClient(
                            createMysqlPool(null, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.Pool
                    )
                }
            if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                context("SqlConnection") {
                    tests(
                        DatabaseClient(
                            createMysqlConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.SqlConnection
                    )
                }
        }
    if (RdbmsType.Oracle in enabledRdbmsTypes)
        context("Oracle") {
            val oracleContainer = install(TestContainerSpecExtension(LatestOracleContainer()))
            val connectionConfig = oracleContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectOracle()
            val databaseClientConfig = OracleDatabaseClientConfig()
            suspend fun FunSpecContainerScope.tests(databaseClient: DatabaseClient<*>, sqlClientType: SqlClientType) =
                tests(databaseClient, RdbmsType.Oracle, sqlClientType)
            if (SqlClientType.Pool in enabledSqlClientTypes)
                context("Pool") {
                    tests(
                        DatabaseClient(
                            createOraclePool(null, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.Pool
                    )
                }
            if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                context("SqlConnection") {
                    tests(
                        DatabaseClient(
                            createOracleConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.SqlConnection
                    )
                }
        }
    if (RdbmsType.Mssql in enabledRdbmsTypes)
        context("MSSQL") {
            val mssqlContainer = install(TestContainerSpecExtension(LatestMssqlContainer()))
            val connectionConfig = mssqlContainer.connectionConfig()
            val exposedDatabase = with(connectionConfig) {
                exposedDatabaseConnectMssql(sqlServerJdbcUrlWithEncryptEqFalse())
            }
            val databaseClientConfig = MssqlDatabaseClientConfig()
            suspend fun FunSpecContainerScope.tests(databaseClient: DatabaseClient<*>, sqlClientType: SqlClientType) =
                tests(databaseClient, RdbmsType.Mssql, sqlClientType)
            if (SqlClientType.Pool in enabledSqlClientTypes)
                context("Pool") {
                    tests(
                        DatabaseClient(
                            createMssqlPool(null, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.Pool
                    )
                }
            if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                context("SqlConnection") {
                    tests(
                        DatabaseClient(
                            createMssqlConnection(vertx, connectionConfig), exposedDatabase, databaseClientConfig
                        ),
                        SqlClientType.SqlConnection
                    )
                }
        }
})