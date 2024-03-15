package com.huanshankeji.exposed.benchmark

import com.huanshankeji.kotlinx.coroutine.awaitAny
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
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

    private suspend fun awaitAsync10KTransactions() =
        coroutineScope {
            List(`10K`) { async { transaction(database) {} } }.awaitAll()
        }

    @Benchmark
    fun singleThreadConcurrent10KTransactions() = runBlocking {
        awaitAsync10KTransactions()
    }


    @Benchmark
    fun multiThreadConcurrent10KTransactions() = runBlocking {
        withContext(Dispatchers.Default) {
            awaitAsync10KTransactions()
        }
    }


    @Benchmark
    fun _10KSuspendedTransactions() = runBlocking {
        repeat(`10K`) { newSuspendedTransaction(db = database) {} }
    }

    @Benchmark
    fun _10KSuspendedTransactionAsyncs() = runBlocking {
        List(`10K`) { suspendedTransactionAsync(db = database) {} }.awaitAny()
    }

    @Benchmark
    fun multiThreadMultiConnectionEach10KLocalTransactions() {
        List(Runtime.getRuntime().availableProcessors()) {
            thread {
                val database = databaseConnect()
                repeat(`10K`) { transaction(database) {} }
            }
        }.forEach {
            it.join()
        }
    }
}