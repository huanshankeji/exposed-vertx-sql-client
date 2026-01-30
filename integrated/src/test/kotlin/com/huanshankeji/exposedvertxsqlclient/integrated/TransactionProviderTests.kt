package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.*
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.vertx.core.Vertx

@OptIn(ExperimentalEvscApi::class)
class TransactionProviderTests : FunSpec({
    val vertx = Vertx.vertx()
    afterSpec { vertx.close().await() }

    val postgresqlContainer = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
    val connectionConfig = postgresqlContainer.connectionConfig()
    val exposedDatabase = connectionConfig.exposedDatabaseConnectPostgresql()
    val databaseClientConfig = PgDatabaseClientConfig()

    context("DatabaseExposedTransactionProvider") {
        val provider = DatabaseExposedTransactionProvider(exposedDatabase)
        val databaseClient = DatabaseClient(
            createPgPool(null, connectionConfig),
            provider,
            databaseClientConfig
        )

        test("DatabaseClient operations work") {
            // This should use the transaction provider for SQL preparation
            val isWorking = databaseClient.isWorking()
            assert(isWorking)
        }
    }

    context("SharedJdbcTransactionExposedTransactionProvider") {
        // Note: The provider holds a shared transaction that was created during initialization.
        // In this test, the connection is managed by the transaction() block in the provider's constructor,
        // which commits and closes the connection when the block completes. The transaction object
        // itself remains usable for SQL preparation purposes.
        val provider = SharedJdbcTransactionExposedTransactionProvider(exposedDatabase)
        val databaseClient = DatabaseClient(
            createPgPool(null, connectionConfig),
            provider,
            databaseClientConfig
        )

        test("DatabaseClient operations work with shared transaction") {
            // This should use the shared transaction provider for SQL preparation
            val isWorking = databaseClient.isWorking()
            assert(isWorking)
        }
    }

    context("Backward compatibility with deprecated constructor") {
        val databaseClient = @Suppress("DEPRECATION") DatabaseClient(
            createPgPool(null, connectionConfig),
            exposedDatabase,
            databaseClientConfig
        )

        test("Deprecated constructor still works") {
            val isWorking = databaseClient.isWorking()
            assert(isWorking)
        }

        test("exposedDatabase property is accessible") {
            @Suppress("DEPRECATION")
            val db = databaseClient.exposedDatabase
            assert(db != null)
            assert(db == exposedDatabase)
        }
    }
})
