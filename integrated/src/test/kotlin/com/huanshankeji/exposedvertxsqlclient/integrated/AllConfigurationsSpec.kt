package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.ExperimentalApi
import com.huanshankeji.arrow.fx.coroutines.kotest.asResourceScope
import com.huanshankeji.exposedvertxsqlclient.*
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlClient
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.JdbcDatabaseContainer
import java.util.*

@OptIn(ExperimentalEvscApi::class, ExperimentalApi::class)
// name suggested by Copilot
abstract class AllConfigurationsSpec(
    tests: suspend FunSpecContainerScope.(databaseClient: DatabaseClient<*>, rdbmsType: RdbmsType, sqlClientType: SqlClientType) -> Unit,
    // for disabling some tests for debugging
    enabledRdbmsTypes: EnumSet<RdbmsType> = EnumSet.allOf(RdbmsType::class.java),
    //extraPgConnectOptions: PgConnectOptions.() -> Unit = {}
    enabledSqlClientTypes: EnumSet<SqlClientType> = EnumSet.allOf(SqlClientType::class.java),
    enabledStatementPreparationExposedTransactionProviderTypes: EnumSet<StatementPreparationExposedTransactionProviderType> = EnumSet.allOf(
        StatementPreparationExposedTransactionProviderType::class.java
    )
) : FunSpec({
    // This causes passing tests to fail. Not sure why.
    //testExecutionMode = TestExecutionMode.Concurrent

    suspend fun <T : JdbcDatabaseContainer<*>> FunSpecContainerScope.startContainer(create: () -> T): T =
        asResourceScope().install({ create().also { it.start() } }) { container, _ -> container.stop() }

    suspend fun FunSpecContainerScope.installSqlClient(create: suspend () -> SqlClient): SqlClient =
        asResourceScope().install({ create() }) { client, _ -> client.close().coAwait() }

    suspend fun FunSpecContainerScope.installSqlConnection(create: suspend (Vertx) -> SqlClient): SqlClient =
        asResourceScope().run {
            val vertx = install({ Vertx.vertx() }) { it, _ -> it.close().coAwait() }
            install({ create(vertx) }) { it, _ -> it.close().coAwait() }
        }

    suspend fun FunSpecContainerScope.testsForAllProviderTypes(
        exposedDatabase: Database,
        tests: suspend FunSpecContainerScope.(statementPreparationExposedTransactionProvider: StatementPreparationExposedTransactionProvider) -> Unit
    ) {
        for (providerType in enabledStatementPreparationExposedTransactionProviderTypes) {
            when (providerType) {
                StatementPreparationExposedTransactionProviderType.Database ->
                    context("DatabaseExposedTransactionProvider") {
                        tests(DatabaseExposedTransactionProvider(exposedDatabase))
                    }

                StatementPreparationExposedTransactionProviderType.JdbcTransaction ->
                    context("JdbcTransactionExposedTransactionProvider") {
                        tests(JdbcTransactionExposedTransactionProvider(exposedDatabase))
                    }
            }
        }
    }

    // TODO consider not running all tests against all kinds of `SqlClient`s to save some time
    if (RdbmsType.Postgresql in enabledRdbmsTypes)
        context("PostgreSQL") {
            val postgresqlContainer = startContainer { LatestPostgreSQLContainer() }
            val connectionConfig = postgresqlContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
            testsForAllProviderTypes(exposedDatabase) { statementPreparationExposedTransactionProvider ->
                val databaseClientConfig = PgDatabaseClientConfig(statementPreparationExposedTransactionProvider)
                suspend fun FunSpecContainerScope.tests(
                    databaseClient: DatabaseClient<*>, sqlClientType: SqlClientType
                ) =
                    tests(databaseClient, RdbmsType.Postgresql, sqlClientType)
                if (SqlClientType.Client in enabledSqlClientTypes)
                    context("Client") {
                        tests(
                            DatabaseClient(
                                installSqlClient { createPgClient(null, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.Client
                        )
                    }
                if (SqlClientType.Pool in enabledSqlClientTypes)
                    context("Pool") {
                        tests(
                            DatabaseClient(
                                installSqlClient { createPgPool(null, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.Pool
                        )
                    }
                if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                    context("SqlConnection") {
                        tests(
                            DatabaseClient(
                                installSqlConnection { createPgConnection(it, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.SqlConnection
                        )
                    }
            }
        }
    if (RdbmsType.Mysql in enabledRdbmsTypes)
        context("MySQL") {
            val mysqlContainer = startContainer { LatestMySQLContainer() }
            val connectionConfig = mysqlContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectMysql()
            testsForAllProviderTypes(exposedDatabase) { statementPreparationExposedTransactionProvider ->
                val databaseClientConfig = MysqlDatabaseClientConfig(statementPreparationExposedTransactionProvider)
                suspend fun FunSpecContainerScope.tests(
                    databaseClient: DatabaseClient<*>,
                    sqlClientType: SqlClientType
                ) =
                    tests(databaseClient, RdbmsType.Mysql, sqlClientType)
                if (SqlClientType.Client in enabledSqlClientTypes)
                    context("Client") {
                        tests(
                            DatabaseClient(
                                installSqlClient { createMysqlClient(null, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.Client
                        )
                    }
                if (SqlClientType.Pool in enabledSqlClientTypes)
                    context("Pool") {
                        tests(
                            DatabaseClient(
                                installSqlClient { createMysqlPool(null, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.Pool
                        )
                    }
                if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                    context("SqlConnection") {
                        tests(
                            DatabaseClient(
                                installSqlConnection { createMysqlConnection(it, connectionConfig) },
                                databaseClientConfig
                            ),
                            SqlClientType.SqlConnection
                        )
                    }
            }
        }
    if (RdbmsType.Oracle in enabledRdbmsTypes)
        context("Oracle") {
            val oracleContainer = startContainer { LatestOracleContainer() }
            val connectionConfig = oracleContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectOracle()
            testsForAllProviderTypes(exposedDatabase) { statementPreparationExposedTransactionProvider ->
                val databaseClientConfig = OracleDatabaseClientConfig(statementPreparationExposedTransactionProvider)
                suspend fun FunSpecContainerScope.tests(
                    databaseClient: DatabaseClient<*>,
                    sqlClientType: SqlClientType
                ) =
                    tests(databaseClient, RdbmsType.Oracle, sqlClientType)
                if (SqlClientType.Pool in enabledSqlClientTypes)
                    context("Pool") {
                        tests(
                            DatabaseClient(
                                installSqlClient { createOraclePool(null, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.Pool
                        )
                    }
                if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                    context("SqlConnection") {
                        tests(
                            DatabaseClient(
                                installSqlConnection { createOracleConnection(it, connectionConfig) },
                                databaseClientConfig
                            ),
                            SqlClientType.SqlConnection
                        )
                    }
            }
        }
    if (RdbmsType.Mssql in enabledRdbmsTypes)
        context("MSSQL") {
            val mssqlContainer = startContainer { LatestMssqlContainer() }
            val connectionConfig = mssqlContainer.connectionConfig()
            val exposedDatabase = with(connectionConfig) {
                exposedDatabaseConnectMssql(sqlServerJdbcUrlWithEncryptEqFalse())
            }
            testsForAllProviderTypes(exposedDatabase) { statementPreparationExposedTransactionProvider ->
                val databaseClientConfig = MssqlDatabaseClientConfig(statementPreparationExposedTransactionProvider)
                suspend fun FunSpecContainerScope.tests(
                    databaseClient: DatabaseClient<*>,
                    sqlClientType: SqlClientType
                ) =
                    tests(databaseClient, RdbmsType.Mssql, sqlClientType)
                if (SqlClientType.Pool in enabledSqlClientTypes)
                    context("Pool") {
                        tests(
                            DatabaseClient(
                                installSqlClient { createMssqlPool(null, connectionConfig) }, databaseClientConfig
                            ),
                            SqlClientType.Pool
                        )
                    }
                if (SqlClientType.SqlConnection in enabledSqlClientTypes)
                    context("SqlConnection") {
                        tests(
                            DatabaseClient(
                                installSqlConnection { createMssqlConnection(it, connectionConfig) },
                                databaseClientConfig
                            ),
                            SqlClientType.SqlConnection
                        )
                    }
            }
        }
})