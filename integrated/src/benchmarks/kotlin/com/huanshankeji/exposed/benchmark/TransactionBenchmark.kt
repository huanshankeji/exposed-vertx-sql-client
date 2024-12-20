package com.huanshankeji.exposed.benchmark

import com.huanshankeji.kotlinx.coroutines.benchmark.ParameterizedRunBlockingAwaitAsyncsBenchmark
import com.huanshankeji.kotlinx.coroutines.benchmark.RunBlockingAwaitAsyncsBenchmark
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@State(Scope.Benchmark)
class TransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    @Benchmark
    fun transaction() {
        transaction(database) {}
    }

    companion object {
        const val `10K` = 10_000
    }

    @Benchmark
    fun _10KTransactions() {
        repeat(`10K`) { transaction(database) {} }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10K(crossinline block: () -> Unit) =
        List(`10K`) { async { block() } }.awaitAll()

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun CoroutineScope.awaitAsync10KTransactions() =
        awaitAsync10K { transaction(database) {} }

    /**
     * Compare with [RunBlockingAwaitAsyncsBenchmark].
     */
    @Benchmark
    fun singleThreadConcurrent10KTransactions() =
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        runBlocking(newSingleThreadContext("single thread")) {
            awaitAsync10KTransactions()
        }


    /**
     * Compare with [ParameterizedRunBlockingAwaitAsyncsBenchmark].
     */
    @Benchmark
    fun multiThreadConcurrent10KTransactionsWithSharedDatabase() =
        runBlocking { awaitAsync10KTransactions() }


    @Benchmark
    fun _10KSuspendedTransactions() = runBlocking {
        repeat(`10K`) { newSuspendedTransaction(db = database) {} }
    }

    @Benchmark
    fun _10KSuspendedTransactionAsyncs() = runBlocking {
        List(`10K`) { suspendedTransactionAsync(db = database) {} }.awaitAll()
    }

    private fun numProcessors() =
        Runtime.getRuntime().availableProcessors().also {
            println("Number of processors: $it")
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