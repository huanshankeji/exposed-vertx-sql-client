package com.huanshankeji.exposed.benchmark.r2dbc

import com.huanshankeji.exposed.benchmark.`10K`
import com.huanshankeji.exposed.benchmark.WithContainerizedDatabaseBenchmark
import com.huanshankeji.exposed.benchmark.awaitAsync10K
import com.huanshankeji.exposed.benchmark.numProcessors
import com.huanshankeji.exposedvertxsqlclient.r2dbc.connectionPool
import com.huanshankeji.exposedvertxsqlclient.r2dbc.exposedR2dbcDatabaseConnectPostgresql
import io.r2dbc.pool.ConnectionPool
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Setup
import kotlinx.benchmark.TearDown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

abstract class SharedDatabaseTransactionBenchmark : WithContainerizedDatabaseBenchmark() {
    lateinit var database: R2dbcDatabase

    class UnpooledConnection : SharedDatabaseTransactionBenchmark() {
        @Setup
        override fun setup() {
            super.setup()
            database = connectionConfig.exposedR2dbcDatabaseConnectPostgresql()
        }
    }

    abstract class PooledConnection : SharedDatabaseTransactionBenchmark() {
        lateinit var connectionPool: ConnectionPool

        @Setup
        override fun setup() {
            super.setup()
            connectionPool = connectionConfig.connectionPool(connectionPoolSize)
            database = connectionPool.exposedR2dbcDatabaseConnectPostgresql()
        }

        abstract val connectionPoolSize: Int

        @TearDown
        override fun tearDown() {
            runBlocking {
                connectionPool.close().awaitSingle()
            }
            super.tearDown()
        }

        class Size1 : PooledConnection() {
            override val connectionPoolSize: Int get() = 1
        }

        class SizeNumProcessors : PooledConnection() {
            override val connectionPoolSize: Int get() = numProcessors
        }
    }

    @Benchmark
    fun runBlocking_suspendTransaction() = runBlocking {
        suspendTransaction(database) {}
    }

    @Benchmark
    fun runBlocking_10K_suspendTransaction() = runBlocking {
        repeat(`10K`) { suspendTransaction(database) {} }
    }

    @Benchmark
    fun runBlocking_10K_async_suspendTransaction() = runBlocking {
        awaitAsync10K { suspendTransaction(database) {} }
    }

    @Benchmark
    fun runBlocking_dispatchersDefault_10K_async_suspendTransaction() =
        runBlocking(Dispatchers.Default) {
            awaitAsync10K { suspendTransaction(database) {} }
        }
}