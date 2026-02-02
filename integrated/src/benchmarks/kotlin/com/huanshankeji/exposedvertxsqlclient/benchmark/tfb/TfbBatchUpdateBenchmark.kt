package com.huanshankeji.exposedvertxsqlclient.benchmark.tfb

import com.huanshankeji.exposed.benchmark.jdbc.WithContainerizedDatabaseAndExposedDatabaseBenchmark
import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.DatabaseExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.JdbcTransactionExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgConnection
import kotlinx.benchmark.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.random.Random

/**
 * Benchmark replicating the TechEmpower Framework Benchmark (TFB) `updates` test scenario.
 *
 * This benchmark compares the performance of batch updates using:
 * - `JdbcTransactionExposedTransactionProvider` (reuses a single JDBC transaction for SQL preparation)
 * - `DatabaseExposedTransactionProvider` (creates a new transaction per SQL preparation)
 *
 * Based on https://github.com/huanshankeji/FrameworkBenchmarks/blob/aa271b70ff99411c8a47e99a06cfa2d856245dd0/frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-vertx-sql-client/src/main/kotlin/MainVerticle.kt#L34-L42
 */
@State(Scope.Benchmark)
@OptIn(ExperimentalEvscApi::class)
class TfbBatchUpdateBenchmark : WithContainerizedDatabaseAndExposedDatabaseBenchmark() {
    lateinit var vertx: Vertx
    lateinit var pgConnection: PgConnection

    lateinit var databaseClient: DatabaseClient<PgConnection>

    // TFB uses query counts from 1 to 500
    @Param("1", "10", "20", "50", "100", "200", "500")
    var queryCount: Int = 20

    @Param("JdbcTransaction", "DatabaseTransaction")
    var transactionProviderType: String = "JdbcTransaction"

    // Pre-generated worlds for consistency
    lateinit var worldsToUpdate: List<World>

    @Setup
    override fun setup() {
        super.setup()

        transaction(database) {
            SchemaUtils.drop(WorldTable) // TODO This should be removed?
            SchemaUtils.create(WorldTable)

            val batchSize = 1000
            repeat(10) { batch ->
                val idsToInsert = (1..batchSize).map { batch * batchSize + it }
                WorldTable.batchInsert(idsToInsert) { id ->
                    this[WorldTable.id] = id
                    this[WorldTable.randomNumber] = Random.nextInt(1, 10001)
                }
            }
        }

        vertx = Vertx.vertx()

        pgConnection = runBlocking {
            createPgConnection(vertx, connectionConfig, {
                cachePreparedStatements = true
                pipeliningLimit = 256
            })
        }

        val transactionProvider = when (transactionProviderType) {
            "JdbcTransaction" -> JdbcTransactionExposedTransactionProvider(database)
            "DatabaseTransaction" -> DatabaseExposedTransactionProvider(database)
            else -> error("Unknown transaction provider type: $transactionProviderType")
        }

        databaseClient = DatabaseClient(
            pgConnection,
            PgDatabaseClientConfig(
                transactionProvider,
                validateBatch = false
            )
        )

        // Pre-generate random worlds to update
        worldsToUpdate = (1..500).map {
            World(
                id = Random.nextInt(1, 10001),
                randomNumber = Random.nextInt(1, 10001)
            )
        }
    }

    @TearDown
    override fun tearDown() {
        runBlocking {
            pgConnection.close().coAwait()
            vertx.close().coAwait()
        }
        transaction(database) {
            SchemaUtils.drop(WorldTable)
        }
        super.tearDown()
    }

    @Benchmark
    fun batchUpdate() = runBlocking {
        val worlds = worldsToUpdate.take(queryCount)
        databaseClient.executeBatchUpdate(
            worlds.map { world ->
                buildStatement {
                    WorldTable.update({ WorldTable.id eq world.id }) {
                        it[randomNumber] = world.randomNumber
                    }
                }
            }
        )
    }
}