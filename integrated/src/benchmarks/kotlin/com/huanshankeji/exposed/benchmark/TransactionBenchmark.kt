package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig.Socket
import com.huanshankeji.exposedvertxsqlclient.exposed.exposedDatabaseConnect
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.concurrent.thread

@State(Scope.Benchmark)
class TransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    @Benchmark
    fun multiThreadMultiConnectionEach10KLocalTransactions() {
        List(2) {
            thread {
                val database = with(postgreSQLContainer) {
                    Socket(
                        this.host,
                        this.firstMappedPort,
                        this.username,
                        this.password,
                        this.databaseName
                    )
                }
                    .exposedDatabaseConnect(
                        "postgresql", "org.postgresql.Driver", {}, null, { TransactionManager(it) }
                    )
                repeat(10000) { transaction(database) {} }
            }
        }.forEach {
            it.join()
        }
    }
}