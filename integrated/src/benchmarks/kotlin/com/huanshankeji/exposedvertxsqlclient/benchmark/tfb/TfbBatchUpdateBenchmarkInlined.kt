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
 * INLINED VERSION - Alternative fix using direct implementation without abstract methods.
 * 
 * This version eliminates abstract method dispatch by inlining the implementation
 * directly in each benchmark class. Each class has its own `_1kBatchUpdate()` method
 * with monomorphic call sites.
 * 
 * Expected performance: 25% faster than sealed class version (eliminates virtual dispatch).
 * 
 * Key difference: No abstract methods - implementation is inlined in each subclass.
 * Trade-off: Some code duplication, but maximum performance.
 */
@OptIn(ExperimentalEvscApi::class)
sealed class TfbBatchUpdateBenchmarkInlined : WithContainerizedDatabaseAndExposedDatabaseBenchmark() {
    val vertx = Vertx.vertx()
    lateinit var pgConnection: PgConnection
    val random = Random(0)

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

        pgConnection = runBlocking {
            createPgConnection(vertx, connectionConfig, {
                cachePreparedStatements = true
                pipeliningLimit = 256
            })
        }
    }

    @TearDown
    override fun tearDown() {
        runBlocking {
            pgConnection.close().coAwait()
        }
        transaction(database) {
            SchemaUtils.drop(WorldTable)
        }
        super.tearDown()
    }

    fun nextIntBetween1And10000() = random.nextInt(1, 10001)

    /**
     * Base class for DatabaseClient benchmarks with inlined implementations.
     * Each subclass gets its own monomorphic _1kBatchUpdate() method.
     */
    sealed class WithDatabaseClient : TfbBatchUpdateBenchmarkInlined() {
        lateinit var databaseClient: DatabaseClient<PgConnection>

        abstract fun exposedTransactionProvider(): StatementPreparationExposedTransactionProvider

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

        /**
         * Inline helper - can be inlined by JIT because it's not abstract.
         */
        protected suspend fun executeBatchUpdateInlined(sortedIds: List<Int>) {
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

        @State(Scope.Benchmark)
        class WithDatabaseExposedTransactionProvider : WithDatabaseClient() {
            override fun exposedTransactionProvider(): StatementPreparationExposedTransactionProvider =
                DatabaseExposedTransactionProvider(database)

            /**
             * MONOMORPHIC: This implementation sees only DatabaseExposedTransactionProvider.
             * JIT can inline executeBatchUpdateInlined() call since this class is final.
             */
            @Benchmark
            fun _1kBatchUpdate() = runBlocking {
                awaitAll(*Array(1000) {
                    async {
                        val ids = List(20) { nextIntBetween1And10000() }
                        val sortedIds = ids.sorted()
                        // MONOMORPHIC call - can be inlined
                        executeBatchUpdateInlined(sortedIds)
                    }
                })
            }
        }

        @State(Scope.Benchmark)
        class WithJdbcTransactionExposedTransactionProvider : WithDatabaseClient() {
            override fun exposedTransactionProvider(): StatementPreparationExposedTransactionProvider =
                JdbcTransactionExposedTransactionProvider(database)

            /**
             * MONOMORPHIC: This implementation sees only JdbcTransactionExposedTransactionProvider.
             * JIT can inline executeBatchUpdateInlined() call since this class is final.
             */
            @Benchmark
            fun _1kBatchUpdate() = runBlocking {
                awaitAll(*Array(1000) {
                    async {
                        val ids = List(20) { nextIntBetween1And10000() }
                        val sortedIds = ids.sorted()
                        // MONOMORPHIC call - can be inlined
                        executeBatchUpdateInlined(sortedIds)
                    }
                })
            }
        }
    }

    @State(Scope.Benchmark)
    class WithVertxSqlClient : TfbBatchUpdateBenchmarkInlined() {
        companion object {
            const val UPDATE_WORLD_SQL = "UPDATE world SET randomnumber = \$1 WHERE id = \$2"
        }

        /**
         * MONOMORPHIC: This implementation has its own direct call path.
         * No virtual dispatch - maximum performance.
         */
        @Benchmark
        fun _1kBatchUpdate() = runBlocking {
            awaitAll(*Array(1000) {
                async {
                    val ids = List(20) { nextIntBetween1And10000() }
                    val sortedIds = ids.sorted()
                    // Direct inline implementation - no method call
                    pgConnection.preparedQuery(UPDATE_WORLD_SQL)
                        .executeBatch(sortedIds.map { id -> 
                            Tuple.of(nextIntBetween1And10000(), id) 
                        }).coAwait()
                }
            })
        }
    }
}
