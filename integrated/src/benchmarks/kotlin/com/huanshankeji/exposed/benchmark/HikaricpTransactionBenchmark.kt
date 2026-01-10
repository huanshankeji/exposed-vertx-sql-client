package com.huanshankeji.exposed.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@State(Scope.Benchmark)
class HikaricpTransactionBenchmark : WithContainerizedDatabaseAndHikaricpExposedDatabaseBenchmark() {
    // Change this value to see how it affects performance
    override val maximumPoolSize: Int get() = numProcessors * 64

    @Benchmark
    fun transaction() {
        transaction(database) {}
    }

    @Benchmark
    fun multiThread_parallel_10K_transactions_nearlyEvenlyPartitioned() {
        multiThread_10K_ops_nearlyEvenlyPartitioned_helper { num ->
            repeat(num) { transaction(database) {} }
        }
    }
}