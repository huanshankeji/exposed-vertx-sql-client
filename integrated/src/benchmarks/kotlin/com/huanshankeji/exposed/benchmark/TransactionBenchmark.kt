package com.huanshankeji.exposed.benchmark

import com.huanshankeji.kotlinx.coroutines.benchmark.ParameterizedRunBlockingAwaitAsyncsBenchmark
import com.huanshankeji.kotlinx.coroutines.benchmark.RunBlockingAwaitAsyncsBenchmark
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.lang.Thread.sleep
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

        private fun numProcessors() =
            Runtime.getRuntime().availableProcessors().also {
                println("Number of processors: $it")
            }
    }

    @Benchmark
    fun _10KTransactions() {
        repeat(`10K`) { transaction(database) {} }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10K(crossinline block: () -> Unit) =
        List(`10K`) { async { block() } }.awaitAll()

    /**
     * For debugging purposes.
     */
    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend inline fun CoroutineScope.awaitAsync10KCountingThreads(crossinline block: () -> Unit) {
        val threadMap = HashSet<Thread>(numProcessors())
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

    /*
    // TODO adapt to evenly divided as below
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
    */


    private inline fun multiThreadParallel10KTransactionsEvenlyDividedHelper(crossinline block: () -> Unit) {
        val numThreads = numProcessors()
        val numTransactionEachThread = `10K` / numThreads
        // Note that on a device with heterogeneous architecture some threads may finish earlier than others.
        List(numThreads) {
            thread {
                repeat(numTransactionEachThread) { transaction(database) { block() } }
            }
        }.forEach {
            it.join()
        }
    }

    @Benchmark
    fun multiThreadParallel10KTransactionsEvenlyDivided() =
        multiThreadParallel10KTransactionsEvenlyDividedHelper {}

    @Benchmark
    fun multiThreadParallel10KTransactionsWithSleepEvenlyDivided() =
        multiThreadParallel10KTransactionsEvenlyDividedHelper { sleep(1) }

    /*
    // These don't work because the block in side `transaction` can't be suspend.

    private inline fun multiThreadCoroutineParallel10KTransactionsEvenlyDividedHelper(crossinline block: suspend () -> Unit) {
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
    fun multiThreadCoroutineParallel10KTransactionsEvenlyDivided() =
        multiThreadCoroutineParallel10KTransactionsEvenlyDividedHelper {}

    @Benchmark
    fun multiThreadCoroutineParallel10KTransactionsWithDelayEvenlyDivided() =
        multiThreadCoroutineParallel10KTransactionsEvenlyDividedHelper { delay(1) }
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