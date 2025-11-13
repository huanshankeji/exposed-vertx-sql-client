package com.huanshankeji.exposed.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@State(Scope.Benchmark)
class TransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    @Benchmark
    fun transaction() {
        transaction(database) {}
    }
}