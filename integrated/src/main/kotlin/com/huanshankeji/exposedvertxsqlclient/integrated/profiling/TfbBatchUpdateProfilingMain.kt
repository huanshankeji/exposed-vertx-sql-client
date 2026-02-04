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

    fun run1kBatchUpdateWithDatabaseExposedTransactionProvider() {
        println("[$providerName] Running with DatabaseExposedTransactionProvider...")
        val databaseClient = DatabaseClient(
            pgConnection,
            PgDatabaseClientConfig(
                DatabaseExposedTransactionProvider(database),
                validateBatch = false
            )
        )
        run1kBatchUpdateWithDatabaseClient(databaseClient)
    }

    fun run1kBatchUpdateWithJdbcTransactionExposedTransactionProvider() {
        println("[$providerName] Running with JdbcTransactionExposedTransactionProvider...")
        val databaseClient = DatabaseClient(
            pgConnection,
            PgDatabaseClientConfig(
                JdbcTransactionExposedTransactionProvider(database),
                validateBatch = false
            )
        )
        run1kBatchUpdateWithDatabaseClient(databaseClient)
    }

    private fun run1kBatchUpdateWithDatabaseClient(databaseClient: DatabaseClient<PgConnection>) {
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
        println("[$providerName] Completed.")
    }
}

/**
 * Main entry point for profiling.
 *
 * This allows profiling with async-profiler or other JVM profilers.
 *
 * Command line arguments:
 *   - First arg: "database" or "jdbc" to select the transaction provider type
 *
 * Example usage with async-profiler:
 *   ./gradlew :exposed-vertx-sql-client-integrated:run -PmainClass=com.huanshankeji.exposedvertxsqlclient.integrated.profiling.TfbBatchUpdateProfilingMainKt --args="database"
 */
fun main(args: Array<String>) {
    val providerType = args.getOrNull(0) ?: "database"

    println("Starting TfbBatchUpdate profiling")
    println("Provider type: $providerType")
    println()

    val profiling = TfbBatchUpdateProfiling(providerType)
    try {
        profiling.setup()

        val startTime = System.currentTimeMillis()
        when (providerType.lowercase()) {
            "database" -> profiling.run1kBatchUpdateWithDatabaseExposedTransactionProvider()
            "jdbc" -> profiling.run1kBatchUpdateWithJdbcTransactionExposedTransactionProvider()
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
