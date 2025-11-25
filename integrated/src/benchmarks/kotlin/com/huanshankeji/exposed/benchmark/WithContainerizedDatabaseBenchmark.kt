package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.LatestPostgreSQLContainer
import com.huanshankeji.exposedvertxsqlclient.exposedDatabaseConnect
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.jetbrains.exposed.v1.jdbc.Database

@State(Scope.Benchmark)
class WithContainerizedDatabaseBenchmark : AbstractBenchmark() {
    val postgreSQLContainer = LatestPostgreSQLContainer()
    lateinit var database: Database

    fun databaseConnect() =
        postgreSQLContainer.exposedDatabaseConnect()

    @Setup
    fun setUp() {
        postgreSQLContainer.start()
        database = databaseConnect()
    }

    @TearDown
    fun tearDown() {
        postgreSQLContainer.stop()
    }
}