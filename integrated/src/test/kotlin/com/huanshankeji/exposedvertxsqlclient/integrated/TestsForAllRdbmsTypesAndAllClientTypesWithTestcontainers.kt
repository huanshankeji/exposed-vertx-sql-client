package com.huanshankeji.exposedvertxsqlclient.integrated

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
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.vertx.core.Vertx
import io.vertx.sqlclient.SqlClient
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.*

/**
 * Creates a database-specific [DatabaseClientConfig] using the given [transactionProvider].
 */
private fun createDatabaseClientConfig(
    rdbmsType: RdbmsType,
    transactionProvider: StatementPreparationExposedTransactionProvider
): DatabaseClientConfig =
    when (rdbmsType) {
        RdbmsType.Postgresql -> PgDatabaseClientConfig(transactionProvider)
        RdbmsType.Mysql -> MysqlDatabaseClientConfig(transactionProvider)
        RdbmsType.Oracle -> OracleDatabaseClientConfig(transactionProvider)
        RdbmsType.Mssql -> MssqlDatabaseClientConfig(transactionProvider)
    }

/**
 * Context name for each provider type for test display.
 */
private fun ExposedStatementPreparationTransactionProviderType.contextName(): String =
    when (this) {
        ExposedStatementPreparationTransactionProviderType.Database -> "DatabaseProvider"
        ExposedStatementPreparationTransactionProviderType.JdbcTransaction -> "JdbcTransactionProvider"
    }

/**
 * Represents the SQL client types supported by each RDBMS type.
 */
private val rdbmsSupportedSqlClientTypes: Map<RdbmsType, Set<SqlClientType>> = mapOf(
    RdbmsType.Postgresql to EnumSet.allOf(SqlClientType::class.java),
    RdbmsType.Mysql to EnumSet.allOf(SqlClientType::class.java),
    // Oracle only supports Pool and SqlConnection (no Client type)
    RdbmsType.Oracle to EnumSet.of(SqlClientType.Pool, SqlClientType.SqlConnection),
    // MSSQL only supports Pool and SqlConnection (no Client type)
    RdbmsType.Mssql to EnumSet.of(SqlClientType.Pool, SqlClientType.SqlConnection)
)

/**
 * Helper class to encapsulate database setup for a specific RDBMS type.
 */
private class RdbmsTestContext(
    val rdbmsType: RdbmsType,
    val connectionConfig: ConnectionConfig.Socket,
    val exposedDatabase: Database
)

@OptIn(ExperimentalEvscApi::class)
abstract class TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers(
    tests: suspend FunSpecContainerScope.(
        databaseClient: DatabaseClient<*>,
        rdbmsType: RdbmsType,
        sqlClientType: SqlClientType,
        providerType: ExposedStatementPreparationTransactionProviderType
    ) -> Unit,
    // for disabling some tests for debugging
    enabledRdbmsTypes: EnumSet<RdbmsType> = EnumSet.allOf(RdbmsType::class.java),
    enabledSqlClientTypes: EnumSet<SqlClientType> = EnumSet.allOf(SqlClientType::class.java),
    enabledStatementPreparationExposedTransactionProviderTypes: EnumSet<ExposedStatementPreparationTransactionProviderType> =
        EnumSet.allOf(ExposedStatementPreparationTransactionProviderType::class.java)
) : FunSpec({
    val vertx = Vertx.vertx()
    afterSpec { vertx.close().await() }

    // This causes passing tests to fail. Not sure why.
    //testExecutionMode = TestExecutionMode.Concurrent

    /**
     * Creates the Vert.x SQL client for the given RDBMS type and SQL client type.
     */
    suspend fun createVertxSqlClient(
        rdbmsType: RdbmsType,
        sqlClientType: SqlClientType,
        connectionConfig: ConnectionConfig.Socket
    ): SqlClient =
        when (rdbmsType) {
            RdbmsType.Postgresql -> when (sqlClientType) {
                SqlClientType.Client -> createPgClient(null, connectionConfig)
                SqlClientType.Pool -> createPgPool(null, connectionConfig)
                SqlClientType.SqlConnection -> createPgConnection(vertx, connectionConfig)
            }
            RdbmsType.Mysql -> when (sqlClientType) {
                SqlClientType.Client -> createMysqlClient(null, connectionConfig)
                SqlClientType.Pool -> createMysqlPool(null, connectionConfig)
                SqlClientType.SqlConnection -> createMysqlConnection(vertx, connectionConfig)
            }
            RdbmsType.Oracle -> when (sqlClientType) {
                SqlClientType.Client -> throw IllegalArgumentException("Oracle does not support Client type")
                SqlClientType.Pool -> createOraclePool(null, connectionConfig)
                SqlClientType.SqlConnection -> createOracleConnection(vertx, connectionConfig)
            }
            RdbmsType.Mssql -> when (sqlClientType) {
                SqlClientType.Client -> throw IllegalArgumentException("MSSQL does not support Client type")
                SqlClientType.Pool -> createMssqlPool(null, connectionConfig)
                SqlClientType.SqlConnection -> createMssqlConnection(vertx, connectionConfig)
            }
        }

    /**
     * Runs tests for all enabled provider types and SQL client types within a specific RDBMS context.
     */
    suspend fun FunSpecContainerScope.runTestsForRdbms(ctx: RdbmsTestContext) {
        val supportedSqlClientTypes = rdbmsSupportedSqlClientTypes[ctx.rdbmsType]!!
        val activeSqlClientTypes = enabledSqlClientTypes.filter { it in supportedSqlClientTypes }

        for (providerType in enabledStatementPreparationExposedTransactionProviderTypes) {
            context(providerType.contextName()) {
                val transactionProvider = createStatementPreparationExposedTransactionProvider(
                    ctx.exposedDatabase, providerType
                )
                val databaseClientConfig = createDatabaseClientConfig(ctx.rdbmsType, transactionProvider)

                for (sqlClientType in activeSqlClientTypes) {
                    context(sqlClientType.name) {
                        // TODO Also consider closing the clients. This isn't a big issue now though.
                        val vertxSqlClient = createVertxSqlClient(
                            ctx.rdbmsType, sqlClientType, ctx.connectionConfig
                        )
                        val databaseClient = DatabaseClient(vertxSqlClient, databaseClientConfig)
                        tests(databaseClient, ctx.rdbmsType, sqlClientType, providerType)
                    }
                }
            }
        }
    }

    // TODO consider not running all tests against all kinds of `SqlClient`s to save some time
    if (RdbmsType.Postgresql in enabledRdbmsTypes)
        context("PostgreSQL") {
            val postgresqlContainer = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
            val connectionConfig = postgresqlContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
            runTestsForRdbms(RdbmsTestContext(RdbmsType.Postgresql, connectionConfig, exposedDatabase))
        }

    if (RdbmsType.Mysql in enabledRdbmsTypes)
        context("MySQL") {
            val mysqlContainer = install(TestContainerSpecExtension(LatestMySQLContainer()))
            val connectionConfig = mysqlContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectMysql()
            runTestsForRdbms(RdbmsTestContext(RdbmsType.Mysql, connectionConfig, exposedDatabase))
        }

    if (RdbmsType.Oracle in enabledRdbmsTypes)
        context("Oracle") {
            val oracleContainer = install(TestContainerSpecExtension(LatestOracleContainer()))
            val connectionConfig = oracleContainer.connectionConfig()
            val exposedDatabase = connectionConfig.exposedDatabaseConnectOracle()
            runTestsForRdbms(RdbmsTestContext(RdbmsType.Oracle, connectionConfig, exposedDatabase))
        }

    if (RdbmsType.Mssql in enabledRdbmsTypes)
        context("MSSQL") {
            val mssqlContainer = install(TestContainerSpecExtension(LatestMssqlContainer()))
            val connectionConfig = mssqlContainer.connectionConfig()
            val exposedDatabase = with(connectionConfig) {
                exposedDatabaseConnectMssql(sqlServerJdbcUrlWithEncryptEqFalse())
            }
            runTestsForRdbms(RdbmsTestContext(RdbmsType.Mssql, connectionConfig, exposedDatabase))
        }
})