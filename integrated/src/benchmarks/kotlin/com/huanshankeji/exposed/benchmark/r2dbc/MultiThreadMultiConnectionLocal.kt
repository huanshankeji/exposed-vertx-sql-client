package com.huanshankeji.exposed.benchmark.r2dbc

import com.huanshankeji.exposed.benchmark.WithContainerizedDatabaseBenchmark
import com.huanshankeji.exposed.benchmark.multiThread_10K_ops_nearlyEvenlyPartitioned_helper
import com.huanshankeji.exposed.benchmark.numProcessors
import com.huanshankeji.exposedvertxsqlclient.r2dbc.exposedR2dbcDatabaseConnectPostgresql
import io.r2dbc.spi.IsolationLevel
import kotlinx.benchmark.Benchmark
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class MultiThreadMultiConnectionLocalTransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    // TODO try `1M`
    private inline fun multiThread_multiConnection_inTotal_10K_runBlocking_local_suspendTransactions_nearlyEvenlyPartitioned_helper(
        numThreads: Int = numProcessors,
        crossinline transactionBlock: suspend (database: R2dbcDatabase) -> Unit
    ) {
        multiThread_10K_ops_nearlyEvenlyPartitioned_helper(numThreads) { num ->
            val database = connectionConfig.exposedR2dbcDatabaseConnectPostgresql()
            runBlocking { repeat(num) { transactionBlock(database) } }
        }
    }

    @Benchmark
    fun mt_mc_inTotal_10K_rb_local_ru_ro_suspendTransactions_nep() =
        multiThread_multiConnection_inTotal_10K_runBlocking_local_suspendTransactions_nearlyEvenlyPartitioned_helper {
            suspendTransaction(it, IsolationLevel.READ_UNCOMMITTED, true) {}
        }
}