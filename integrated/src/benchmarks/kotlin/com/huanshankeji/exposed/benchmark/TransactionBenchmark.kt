package com.huanshankeji.exposed.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.concurrent.thread

@State(Scope.Benchmark)
class TransactionBenchmark : WithContainerizedDatabaseBenchmark() {

    companion object {
        const val `10K` = 10_000

        private fun numProcessors() =
            Runtime.getRuntime().availableProcessors().also {
                println("Number of processors: $it")
            }
    }

    @Benchmark
    fun multiThreadMultiConnectionEach10KLocalTransactions() {
        List(Runtime.getRuntime().availableProcessors()) {
            thread {
                val database = with(postgreSQLContainer) {
                    Database.connect(
                        "jdbc:${"postgresql"}://$host$firstMappedPort/$databaseName",
                        "org.postgresql.Driver",
                        username,
                        password
                    )
                }
                repeat(10_000) { transaction(database) {} }
            }
        }.forEach {
            it.join()
        }
    }
}