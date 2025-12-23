package com.huanshankeji.exposed.benchmark.r2dbc

import com.huanshankeji.exposed.benchmark.`1M`
import com.huanshankeji.exposed.benchmark.WithContainerizedDatabaseBenchmark
import com.huanshankeji.exposed.benchmark.multiThread_ops_nearlyEvenlyPartitioned_helper
import com.huanshankeji.exposed.benchmark.numProcessors
import com.huanshankeji.exposedvertxsqlclient.r2dbc.exposedR2dbcDatabaseConnectPostgresql
import io.r2dbc.spi.IsolationLevel
import kotlinx.benchmark.Benchmark
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class MultiThreadMultiConnectionLocalTransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    private inline fun multiThread_multiConnection_inTotal_1M_runBlocking_local_suspendTransactions_nearlyEvenlyPartitioned_helper(
        numThreads: Int = numProcessors,
        crossinline numTransactionBlock: suspend (database: R2dbcDatabase, num: Int) -> Unit
    ) {
        multiThread_ops_nearlyEvenlyPartitioned_helper(`1M`, numThreads) { num ->
            val database = connectionConfig.exposedR2dbcDatabaseConnectPostgresql()
            runBlocking { numTransactionBlock(database, num) }
        }
    }

    @Benchmark
    fun mt_mc_inTotal_1M_runBlocking_local_ru_ro_suspendTransactions_nep() =
        multiThread_multiConnection_inTotal_1M_runBlocking_local_suspendTransactions_nearlyEvenlyPartitioned_helper { database, num ->
            repeat(num) { suspendTransaction(database, IsolationLevel.READ_UNCOMMITTED, true) {} }
        }

    @Benchmark
    fun mt_mc_inTotal_1M_await_async_local_ru_ro_suspendTransactions_nep() =
        multiThread_multiConnection_inTotal_1M_runBlocking_local_suspendTransactions_nearlyEvenlyPartitioned_helper { database, num ->
            coroutineScope {
                awaitAll(*Array(num) {
                    async { suspendTransaction(database, IsolationLevel.READ_UNCOMMITTED, true) {} }
                })
            }
        }
}