package com.huanshankeji.exposed.benchmark.jdbc

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.transactions.withThreadLocalTransaction
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

@State(Scope.Benchmark)
class WithThreadLocalTransactionBenchmark : WithContainerizedDatabaseAndExposedDatabaseBenchmark() {
    lateinit var jdbcTransaction: JdbcTransaction

    @Setup
    fun setupTransaction() {
        jdbcTransaction = transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) { this }
    }

    @OptIn(InternalApi::class)
    @Benchmark
    fun withThreadLocalTransaction() {
        withThreadLocalTransaction(jdbcTransaction) {}
    }
}
