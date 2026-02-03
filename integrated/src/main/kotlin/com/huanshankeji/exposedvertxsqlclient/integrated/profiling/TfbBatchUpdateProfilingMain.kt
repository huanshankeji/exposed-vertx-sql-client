@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.integrated.profiling

import com.huanshankeji.exposedvertxsqlclient.*
import com.huanshankeji.exposedvertxsqlclient.integrated.LatestPostgreSQLContainer
import com.huanshankeji.exposedvertxsqlclient.integrated.connectionConfig
import com.huanshankeji.exposedvertxsqlclient.integrated.exposedDatabaseConnect
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.random.Random

/**
 * Profiling entry point for TfbBatchUpdate benchmark.
 *
 * This file extracts the benchmark logic from TfbBatchUpdateBenchmark to allow easier profiling
 * with tools like async-profiler that can generate Flame Graphs.
 *
 * Usage: Run with async-profiler attached, e.g.:
 * java -agentpath:/path/to/async-profiler/lib/libasyncProfiler.so=start,event=cpu,file=output.html ...
 */

/** Same WorldTable definition as in benchmark */
object WorldTable : IdTable<Int>("world") {
    override val id = integer("id").entityId()
    val randomNumber = integer("randomnumber").default(0)
}

/**
 * Encapsulates the profiling setup and execution for both transaction providers
 */
class TfbBatchUpdateProfiling(private val providerName: String) {
    val postgreSQLContainer = LatestPostgreSQLContainer()
    val vertx = Vertx.vertx()
    lateinit var database: Database
    lateinit var pgConnection: PgConnection
    val random = Random(0)

    fun setup() {
        println("[$providerName] Starting PostgreSQL container...")
        postgreSQLContainer.start()

        println("[$providerName] Connecting to database...")
        database = postgreSQLContainer.exposedDatabaseConnect()

        transaction(database) {
            SchemaUtils.create(WorldTable)
            WorldTable.batchInsert(1..10_000) { id ->
                this[WorldTable.id] = id
                this[WorldTable.randomNumber] = random.nextInt(1, 10001)
            }
        }

        pgConnection = runBlocking {
            createPgConnection(vertx, postgreSQLContainer.connectionConfig(), {
                cachePreparedStatements = true
                pipeliningLimit = 256
            })
        }
        println("[$providerName] Setup complete.")
    }

    fun tearDown() {
        println("[$providerName] Tearing down...")
        pgConnection.close()
        transaction(database) { SchemaUtils.drop(WorldTable) }
        postgreSQLContainer.stop()
        println("[$providerName] Teardown complete.")
    }

    fun nextIntBetween1And10000() = random.nextInt(1, 10001)

    fun run1kBatchUpdateWithDatabaseExposedTransactionProvider(iterations: Int) {
        println("[$providerName] Running with DatabaseExposedTransactionProvider for $iterations iterations...")
        val databaseClient = DatabaseClient(
            pgConnection,
            PgDatabaseClientConfig(
                DatabaseExposedTransactionProvider(database),
                validateBatch = false
            )
        )
        run1kBatchUpdateWithDatabaseClient(databaseClient, iterations)
    }

    fun run1kBatchUpdateWithJdbcTransactionExposedTransactionProvider(iterations: Int) {
        println("[$providerName] Running with JdbcTransactionExposedTransactionProvider for $iterations iterations...")
        val databaseClient = DatabaseClient(
            pgConnection,
            PgDatabaseClientConfig(
                JdbcTransactionExposedTransactionProvider(database),
                validateBatch = false
            )
        )
        run1kBatchUpdateWithDatabaseClient(databaseClient, iterations)
    }

    private fun run1kBatchUpdateWithDatabaseClient(databaseClient: DatabaseClient<PgConnection>, iterations: Int) {
        repeat(iterations) { iteration ->
            if ((iteration + 1) % 10 == 0) {
                println("[$providerName] Iteration ${iteration + 1}/$iterations")
            }
            runBlocking {
                awaitAll(*Array(1000) {
                    async {
                        val ids = List(20) { nextIntBetween1And10000() }
                        val sortedIds = ids.sorted()
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
                })
            }
        }
        println("[$providerName] Completed $iterations iterations.")
    }
}

/**
 * Main entry point for profiling.
 *
 * This allows profiling with async-profiler or other JVM profilers.
 *
 * Command line arguments:
 *   - First arg: "database" or "jdbc" to select the transaction provider type
 *   - Second arg (optional): number of iterations (default: 50)
 *
 * Example usage with async-profiler:
 *   ./gradlew :exposed-vertx-sql-client-integrated:run -PmainClass=com.huanshankeji.exposedvertxsqlclient.integrated.profiling.TfbBatchUpdateProfilingMainKt --args="database 50"
 */
fun main(args: Array<String>) {
    val providerType = args.getOrNull(0) ?: "database"
    val iterations = args.getOrNull(1)?.toIntOrNull() ?: 50

    println("Starting TfbBatchUpdate profiling")
    println("Provider type: $providerType")
    println("Iterations: $iterations")
    println()

    val profiling = TfbBatchUpdateProfiling(providerType)
    try {
        profiling.setup()

        val startTime = System.currentTimeMillis()
        when (providerType.lowercase()) {
            "database" -> profiling.run1kBatchUpdateWithDatabaseExposedTransactionProvider(iterations)
            "jdbc" -> profiling.run1kBatchUpdateWithJdbcTransactionExposedTransactionProvider(iterations)
            else -> {
                println("Unknown provider type: $providerType. Use 'database' or 'jdbc'.")
                return
            }
        }
        val endTime = System.currentTimeMillis()

        println()
        println("=".repeat(60))
        println("Profiling completed in ${endTime - startTime} ms")
        println("=".repeat(60))
    } finally {
        profiling.tearDown()
    }
}
