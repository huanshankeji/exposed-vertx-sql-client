package com.huanshankeji.exposedvertxsqlclient.benchmark.tfb

import com.huanshankeji.exposed.benchmark.jdbc.WithContainerizedDatabaseAndExposedDatabaseBenchmark
import com.huanshankeji.exposedvertxsqlclient.*
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.Tuple
import kotlinx.benchmark.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.random.Random

/**
 * Replicating the TechEmpower Framework Benchmark (TFB) `updates` test scenarios.
 *
 * Based on https://github.com/huanshankeji/FrameworkBenchmarks/blob/aa271b70ff99411c8a47e99a06cfa2d856245dd0/frameworks/Kotlin/vertx-web-kotlinx/with-db/exposed-vertx-sql-client/src/main/kotlin/MainVerticle.kt#L34-L42
 */
@State(Scope.Benchmark)
@OptIn(ExperimentalEvscApi::class)
sealed class TfbBatchUpdateBenchmark : WithContainerizedDatabaseAndExposedDatabaseBenchmark() {
    //lateinit var vertx: Vertx
    // to prevent `java.util.concurrent.RejectedExecutionException: event executor terminated`
    val vertx = Vertx.vertx()
    lateinit var pgConnection: PgConnection
    // `Pool`
    //lateinit var pgConnection: Pool

    val random = Random(0)

    //lateinit var executorService: ExecutorService

    @Setup
    override fun setup() {
        super.setup()

        transaction(database) {
            SchemaUtils.create(WorldTable)

            WorldTable.batchInsert(1..10_000) { id ->
                this[WorldTable.id] = id
                this[WorldTable.randomNumber] = random.nextInt(1, 10001)
            }
        }

        //vertx = Vertx.vertx()

        pgConnection = runBlocking {
            createPgConnection(vertx, connectionConfig, {
                cachePreparedStatements = true
                pipeliningLimit = 256
            })
            /*
            createPgPool(vertx, connectionConfig, {
                cachePreparedStatements = true
                pipeliningLimit = 256
            }, {
                maxSize = numProcessors
            })
            */
        }
        //executorService = Executors.newFixedThreadPool(numProcessors)
    }

    @TearDown
    override fun tearDown() {
        //executorService.shutdown()
        runBlocking {
            pgConnection.close().coAwait()
            //vertx.close().coAwait()
        }
        transaction(database) {
            SchemaUtils.drop(WorldTable)
        }
        super.tearDown()
    }

    fun nextIntBetween1And10000() =
        random.nextInt(1, 10001)
    // doesn't make a difference in benchmark results
    //ThreadLocalRandom.current().nextInt(1, 10000)

    protected abstract suspend fun executeBatchUpdateWithIds(sortedIds: List<Int>)

    @Benchmark
    // running on all cores doesn't make a difference
    // about 10x performance with `Pool` but it results in "io.vertx.pgclient.PgException: ERROR: deadlock detected (40P01)"
    fun _1kBatchUpdate() = runBlocking/*(executorService.asCoroutineDispatcher())*/ {
        awaitAll(*Array(1000) {
            async {
                val ids = List(20) { nextIntBetween1And10000() }
                val sortedIds = ids.sorted()
                //println("sortedIds: $sortedIds")
                executeBatchUpdateWithIds(sortedIds)
            }
        })
    }

    /**
     * Abstract base class for benchmarks using EVSC's `DatabaseClient` with different transaction providers.
     */
    sealed class WithDatabaseClient : TfbBatchUpdateBenchmark() {
        lateinit var databaseClient: DatabaseClient<PgConnection>
        //lateinit var databaseClient: DatabaseClient<Pool>

        @Setup
        override fun setup() {
            super.setup()

            val transactionProvider = exposedTransactionProvider()
            databaseClient = DatabaseClient(
                pgConnection,
                PgDatabaseClientConfig(
                    transactionProvider,
                    validateBatch = false
                )
            )
        }

        abstract fun exposedTransactionProvider(): StatementPreparationExposedTransactionProvider

        override suspend fun executeBatchUpdateWithIds(sortedIds: List<Int>) {
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

        class WithDatabaseExposedTransactionProvider : WithDatabaseClient() {
            override fun exposedTransactionProvider(): StatementPreparationExposedTransactionProvider =
                DatabaseExposedTransactionProvider(database)
        }

        class WithJdbcTransactionExposedTransactionProvider : WithDatabaseClient() {
            override fun exposedTransactionProvider(): StatementPreparationExposedTransactionProvider =
                JdbcTransactionExposedTransactionProvider(database)
        }
    }

    class WithVertxSqlClient : TfbBatchUpdateBenchmark() {
        companion object {
            const val UPDATE_WORLD_SQL = "UPDATE world SET randomnumber = $1 WHERE id = $2"
        }

        override suspend fun executeBatchUpdateWithIds(sortedIds: List<Int>) {
            pgConnection.preparedQuery(UPDATE_WORLD_SQL)
                .executeBatch(sortedIds.map { id -> Tuple.of(nextIntBetween1And10000(), id) }).coAwait()
        }
    }
}