package com.huanshankeji.exposedvertxsqlclient.profiling

import com.huanshankeji.exposed.benchmark.jdbc.WithContainerizedDatabaseAndExposedDatabaseBenchmark
import com.huanshankeji.exposedvertxsqlclient.*
import com.huanshankeji.exposedvertxsqlclient.benchmark.tfb.WorldTable
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Standalone profiler for TfbBatchUpdateBenchmark that can be run with profiling tools.
 * This extracts the benchmark code to enable profiling with tools like async-profiler.
 */
@OptIn(ExperimentalEvscApi::class)
class TfbBatchUpdateProfiler(
    private val transactionProviderType: TransactionProviderType
) : WithContainerizedDatabaseAndExposedDatabaseBenchmark() {
    enum class TransactionProviderType {
        DATABASE_EXPOSED,
        JDBC_TRANSACTION
    }

    private lateinit var vertx: Vertx
    private lateinit var pgConnection: PgConnection
    private lateinit var databaseClient: DatabaseClient<PgConnection>
    private val random = Random(0)

    override fun setup() {
        super.setup()

        transaction(database) {
            SchemaUtils.create(WorldTable)

            WorldTable.batchInsert(1..10_000) { id ->
                this[WorldTable.id] = id
                this[WorldTable.randomNumber] = random.nextInt(1, 10001)
            }
        }

        vertx = Vertx.vertx()

        pgConnection = runBlocking {
            createPgConnection(vertx, connectionConfig, {
                cachePreparedStatements = true
                pipeliningLimit = 256
            })
        }

        val exposedTransactionProvider = when (transactionProviderType) {
            TransactionProviderType.DATABASE_EXPOSED -> {
                println("Using DatabaseExposedTransactionProvider")
                DatabaseExposedTransactionProvider(database)
            }
            TransactionProviderType.JDBC_TRANSACTION -> {
                println("Using JdbcTransactionExposedTransactionProvider")
                JdbcTransactionExposedTransactionProvider(database)
            }
        }

        databaseClient = DatabaseClient(
            pgConnection,
            PgDatabaseClientConfig(
                exposedTransactionProvider,
                validateBatch = false
            )
        )
    }

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

    private fun nextIntBetween1And10000() = random.nextInt(1, 10001)

    private suspend fun executeBatchUpdateWithIds(sortedIds: List<Int>) {
        databaseClient.executeBatchUpdate(
            sortedIds.map { id ->
                buildStatement {
                    WorldTable.update({ WorldTable.id eq id }) {
                        it[randomNumber] = nextIntBetween1And10000()
                    }
                }
            }
        )
    }

    fun run1kBatchUpdate() = runBlocking {
        awaitAll(*Array(1000) {
            async {
                val ids = List(20) { nextIntBetween1And10000() }
                val sortedIds = ids.sorted()
                executeBatchUpdateWithIds(sortedIds)
            }
        })
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val providerType = when (args.getOrNull(0)) {
                "database" -> TransactionProviderType.DATABASE_EXPOSED
                "jdbc" -> TransactionProviderType.JDBC_TRANSACTION
                else -> {
                    println("Usage: TfbBatchUpdateProfiler <database|jdbc>")
                    return
                }
            }

            println("=".repeat(80))
            println("Profiling TfbBatchUpdate with ${providerType} provider")
            println("=".repeat(80))
            
            val profiler = TfbBatchUpdateProfiler(providerType)

            try {
                profiler.setup()

                // Warm up
                println("\nWarming up (3 iterations)...")
                repeat(3) {
                    print(".")
                    profiler.run1kBatchUpdate()
                }
                println(" done")

                // Actual profiling run
                println("\nStarting profiling run (10 iterations)...")
                val duration = measureTimeMillis {
                    repeat(10) { iteration ->
                        print("Iteration ${iteration + 1}/10...")
                        val iterDuration = measureTimeMillis {
                            profiler.run1kBatchUpdate()
                        }
                        println(" ${iterDuration}ms")
                    }
                }
                println("\n=".repeat(80))
                println("Completed 10 iterations in ${duration}ms")
                println("Average: ${duration / 10}ms per iteration")
                println("=".repeat(80))

            } finally {
                profiler.tearDown()
            }
        }
    }
}
