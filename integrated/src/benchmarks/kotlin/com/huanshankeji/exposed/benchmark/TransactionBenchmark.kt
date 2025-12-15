package com.huanshankeji.exposed.benchmark

import com.huanshankeji.kotlinx.coroutines.benchmark.ParameterizedRunBlockingAwaitAsyncsBenchmark
import com.huanshankeji.kotlinx.coroutines.benchmark.RunBlockingAwaitAsyncsBenchmark
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.stream.IntStream
import kotlin.concurrent.thread

@State(Scope.Benchmark)
class TransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    @Benchmark
    fun transaction() {
        transaction(database) {}
    }

    @Benchmark
    fun transactionWithDefaultDb() {
        transaction {}
    }

    @Benchmark
    fun runBlocking_suspendTransaction() = runBlocking {
        suspendTransaction(database) {}
    }


    companion object {
        const val `10K` = 10_000

        private val numProcessors = Runtime.getRuntime().availableProcessors().also {
            println("Number of processors: $it")
        }
    }

    @Benchmark
    fun _10K_Transactions() {
        repeat(`10K`) { transaction(database) {} }
    }

    @Benchmark
    fun transactionNone_ReadOnly_10K_Transaction() {
        repeat(`10K`) { transaction(database, Connection.TRANSACTION_NONE, true) {} }
    }

    @Benchmark
    fun transactionReadUncommitted_ReadOnly_10K_Transaction() {
        repeat(`10K`) { transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {} }
    }

    @Benchmark
    fun runBlocking_10K_suspendTransactions() = runBlocking {
        repeat(`10K`) { suspendTransaction(database) {} }
    }

    // ! `await` is actually quite expensive.
    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10K(crossinline block: () -> Unit) =
        List(`10K`) { async { block() } }.awaitAll()

    /**
     * For debugging purposes.
     */
    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10KCountingThreads(crossinline block: () -> Unit) {
        val threadMap = ConcurrentHashMap.newKeySet<Thread>(numProcessors)
        List(`10K`) {
            async {
                threadMap.add(Thread.currentThread())
                block()
            }
        }.awaitAll()
        println("Number of threads used: " + threadMap.size)
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun CoroutineScope.awaitAsync10KTransactions() =
        awaitAsync10K { transaction(database) {} }

    /**
     * Compare with [RunBlockingAwaitAsyncsBenchmark].
     */
    @Benchmark
    fun singleThreadConcurrent10KTransactions() =
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        // `newSingleThreadContext("single thread")` yields poorer results.
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use {
            runBlocking(it) {
                awaitAsync10KTransactions()
            }
        }


    /**
     * Compare with [ParameterizedRunBlockingAwaitAsyncsBenchmark].
     */
    @Benchmark
    fun multiThreadConcurrent10KTransactionsWithSharedDatabase() =
        //runBlocking { awaitAsync10KTransactions() } // This does not run on multiple threads as tested.
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        // `newFixedThreadPoolContext(numProcessors(), "multiple threads")` yields much poorer results.
        Executors.newFixedThreadPool(numProcessors).asCoroutineDispatcher().use {
            runBlocking(it) {
                awaitAsync10KTransactions()
            }
        }

    @Benchmark
    fun multiThread_MultiConnection_Each_10K_LocalTransactions() {
        // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
        List(numProcessors) {
            thread {
                val database = databaseConnect()
                repeat(`10K`) { transaction(database) {} }
            }
        }.forEach {
            it.join()
        }
    }

    private inline fun multiThread_10K_NearlyEvenlyPartitioned_Helper(
        numThreads: Int = numProcessors,
        crossinline threadBlock: (num: Int) -> Unit
    ) {
        // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
        List(numThreads) { i ->
            thread {
                val start = i * `10K` / numThreads
                val end = (i + 1) * `10K` / numThreads
                threadBlock(end - start)
            }
        }.forEach { it.join() }
    }


    private inline fun multiThread_MultiConnection_InTotal_10K_Local_Transactions_NearlyEvenlyPartitioned_Helper(
        crossinline transactionBlock: (database: Database) -> Unit
    ) {
        multiThread_10K_NearlyEvenlyPartitioned_Helper { num ->
            val database = databaseConnect()
            repeat(num) { transactionBlock(database) }
        }
    }

    @Benchmark
    fun multiThread_MultiConnection_InTotal_10K_Local_Transactions_NearlyEvenlyPartitioned() =
        multiThread_MultiConnection_InTotal_10K_Local_Transactions_NearlyEvenlyPartitioned_Helper { database ->
            transaction(database) {}
        }

    @Benchmark
    fun multiThread_MultiConnection_InTotal_10K_Local_TransactionNone_ReadOnly_Transactions_NearlyEvenlyPartitioned() =
        multiThread_MultiConnection_InTotal_10K_Local_Transactions_NearlyEvenlyPartitioned_Helper { database ->
            transaction(database, Connection.TRANSACTION_NONE, true) {}
        }

    @Benchmark
    fun multiThread_MultiConnection_InTotal_10K_Local_TransactionReadUncommitted_ReadOnly_Transactions_NearlyEvenlyPartitioned() =
        multiThread_MultiConnection_InTotal_10K_Local_Transactions_NearlyEvenlyPartitioned_Helper { database ->
            transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {}
        }


    private inline fun multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_Helper(crossinline transactionBlock: () -> Unit) {
        multiThread_10K_NearlyEvenlyPartitioned_Helper { num ->
            repeat(num) { transactionBlock() }
        }
    }

    @Benchmark
    fun multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned() =
        multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_Helper { transaction(database) {} }

    @Benchmark
    fun multiThread_Parallel_10K_TransactionNone_ReadOnly_Transactions_NearlyEvenlyPartitioned() =
        multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_Helper {
            transaction(database, Connection.TRANSACTION_NONE, true) {}
        }

    @Benchmark
    fun multiThread_Parallel_10K_TransactionReadUncommitted_ReadOnly_Transactions_NearlyEvenlyPartitioned() =
        multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_Helper {
            transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {}
        }

    @Benchmark
    fun multiThread_Parallel_10K_suspendTransactions_NearlyEvenlyPartitioned() {
        multiThread_10K_NearlyEvenlyPartitioned_Helper { num ->
            runBlocking { repeat(num) { suspendTransaction(database) {} } }
        }
    }

    @Benchmark
    fun multiThread_2x_numProcessors_threads_Parallel_10K_suspendTransactions_NearlyEvenlyPartitioned() {
        multiThread_10K_NearlyEvenlyPartitioned_Helper(2 * numProcessors) { num ->
            runBlocking { repeat(num) { suspendTransaction(database) {} } }
        }
    }


    // This performs poorly.
    @Benchmark
    fun multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_With_CoroutineFlow_FlatMapMerge() {
        val numThreads = numProcessors
        // This dispatcher actually makes performance worse.
        Executors.newFixedThreadPool(numThreads).asCoroutineDispatcher().use {
            runBlocking(it) {
                (0 until `10K`).asFlow()
                    .flatMapMerge(concurrency = numThreads) {
                        flow { emit(transaction(database) {}) }
                    }
                    .collect()
            }
        }
    }

    /*
    // This is not parallel but sequential.
    @Benchmark
    fun multiThreadParallel10KTransactionsEvenlyPartitionedWithCoroutineFlowCollect() {
        runBlocking {
            (0 until `10K`).asFlow()
                .collect { transaction(database) {} }
        }
    }
    */

    @Benchmark
    fun multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_With_JavaStream() {
        IntStream.range(0, `10K`)
            .parallel()
            .forEach { transaction(database) {} }
    }

    @Benchmark
    fun multiThread_Parallel_10K_Transactions_With_Sleep_NearlyEvenlyPartitioned() =
        multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_Helper { transaction(database) { Thread.sleep(1) } }

    /*
    // These don't work because the block inside `transaction` can't be suspend.

    private inline fun multiThreadCoroutineParallel10KTransactionsEvenlyPartitionedHelper(crossinline block: suspend () -> Unit) {
        val numThreads = numProcessors()
        val numTransactionEachThread = `10K` / numThreads
        // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
        runBlocking {
            List(numThreads) {
                launch {
                    repeat(numTransactionEachThread) { transaction(database) { block() } }
                }
            }.forEach {
                it.join()
            }
        }
    }

    @Benchmark
    fun multiThreadCoroutineParallel10KTransactionsEvenlyPartitioned() =
        multiThreadCoroutineParallel10KTransactionsEvenlyPartitionedHelper {}

    @Benchmark
    fun multiThreadCoroutineParallel10KTransactionsWithDelayEvenlyPartitioned() =
        multiThreadCoroutineParallel10KTransactionsEvenlyPartitionedHelper { delay(1) }
    */


    val databaseThreadLocal = ThreadLocal<Database>()
    lateinit var dispatcherWithThreadLocalDatabases: ExecutorCoroutineDispatcher

    @Setup
    fun setUpThreadLocalDatabases() {
        dispatcherWithThreadLocalDatabases = Executors.newFixedThreadPool(numProcessors) {
            Thread {
                it.run()
                databaseThreadLocal.set(databaseConnect())
            }
        }.asCoroutineDispatcher()
    }

    @TearDown
    fun teardownDispatcherWithThreadLocalDatabases() {
        dispatcherWithThreadLocalDatabases.close()
    }

    @Benchmark
    fun multiThread_Concurrent_10K_Transactions_With_ThreadLocal_Databases() {
        runBlocking(dispatcherWithThreadLocalDatabases) {
            awaitAsync10K { transaction(databaseThreadLocal.get()) {} }
        }
    }

    @Benchmark
    fun multiThread_Concurrent_10K_Transactions_With_Implicit_ThreadLocal_Databases() {
        runBlocking(dispatcherWithThreadLocalDatabases) {
            awaitAsync10K { transaction {} }
        }
    }
}