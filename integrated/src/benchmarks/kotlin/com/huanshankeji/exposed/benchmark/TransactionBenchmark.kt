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
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
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


    companion object {
        const val `10K` = 10_000

        private fun numProcessors() =
            Runtime.getRuntime().availableProcessors().also {
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

    // ! `await` is actually quite expensive.
    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10K(crossinline block: () -> Unit) =
        List(`10K`) { async { block() } }.awaitAll()

    /**
     * For debugging purposes.
     */
    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10KCountingThreads(crossinline block: () -> Unit) {
        val threadMap = ConcurrentHashMap.newKeySet<Thread>(numProcessors())
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
        Executors.newFixedThreadPool(numProcessors()).asCoroutineDispatcher().use {
            runBlocking(it) {
                awaitAsync10KTransactions()
            }
        }


    @Benchmark
    fun _10KSuspendedTransactions() = runBlocking {
        repeat(`10K`) { newSuspendedTransaction(db = database) {} }
    }

    @Benchmark
    fun _10KSuspendedTransactionAsyncs() = runBlocking {
        List(`10K`) { suspendedTransactionAsync(db = database) {} }.awaitAll()
    }

    @Benchmark
    fun multiThreadMultiConnectionEach10KLocalTransactions() {
        // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
        List(numProcessors()) {
            thread {
                val database = databaseConnect()
                repeat(`10K`) { transaction(database) {} }
            }
        }.forEach {
            it.join()
        }
    }

    private inline fun multiThread10KNearlyEvenlyPartitionedHelper(crossinline threadBlock: (num: Int) -> Unit) {
        val numThreads = numProcessors()
        // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
        List(numThreads) { i ->
            thread {
                val start = i * `10K` / numThreads
                val end = (i + 1) * `10K` / numThreads
                threadBlock(end - start)
            }
        }.forEach { it.join() }
    }


    private inline fun multiThreadMultiConnectionInTotal10KLocalTransactionsNearlyEvenlyPartitionedHelper(crossinline transactionBlock: (database: Database) -> Unit) {
        multiThread10KNearlyEvenlyPartitionedHelper { num ->
            val database = databaseConnect()
            repeat(num) { transactionBlock(database) }
        }
    }

    @Benchmark
    fun multiThread_MultiConnection_InTotal_10K_Local_Transactions_NearlyEvenlyPartitioned() =
        multiThreadMultiConnectionInTotal10KLocalTransactionsNearlyEvenlyPartitionedHelper { database ->
            transaction(database) {}
        }

    @Benchmark
    fun multiThread_MultiConnection_InTotal_10K_Local_TransactionNone_ReadOnly_Transactions_NearlyEvenlyPartitioned() =
        multiThreadMultiConnectionInTotal10KLocalTransactionsNearlyEvenlyPartitionedHelper { database ->
            transaction(database, Connection.TRANSACTION_NONE, true) {}
        }

    @Benchmark
    fun multiThread_MultiConnection_InTotal_10K_Local_TransactionReadUncommitted_ReadOnly_Transactions_NearlyEvenlyPartitioned() =
        multiThreadMultiConnectionInTotal10KLocalTransactionsNearlyEvenlyPartitionedHelper { database ->
            transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {}
        }


    private inline fun multiThread_Parallel_10K_Transactions_NearlyEvenlyPartitioned_Helper(crossinline transactionBlock: () -> Unit) {
        multiThread10KNearlyEvenlyPartitionedHelper { num ->
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

    // This performs poorly.
    @Benchmark
    fun multiThreadParallel10KTransactionsNearlyEvenlyPartitionedWithCoroutineFlowFlatMapMerge() {
        val numThreads = numProcessors()
        // This dispatcher actually makes performance worse.
        Executors.newFixedThreadPool(numProcessors()).asCoroutineDispatcher().use {
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
    fun multiThreadParallel10KTransactionsNearlyEvenlyPartitionedWithJavaStream() {
        IntStream.range(0, `10K`)
            .parallel()
            .forEach { transaction(database) {} }
    }

    @Benchmark
    fun multiThreadParallel10KTransactionsWithSleepNearlyEvenlyPartitioned() =
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
        dispatcherWithThreadLocalDatabases = Executors.newFixedThreadPool(numProcessors()) {
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
    fun multiThreadConcurrent10KTransactionsWithThreadLocalDatabases() {
        runBlocking(dispatcherWithThreadLocalDatabases) {
            awaitAsync10K { transaction(databaseThreadLocal.get()) {} }
        }
    }

    @Benchmark
    fun multiThreadConcurrent10KTransactionsWithImplicitThreadLocalDatabases() {
        runBlocking(dispatcherWithThreadLocalDatabases) {
            awaitAsync10K { transaction {} }
        }
    }
}