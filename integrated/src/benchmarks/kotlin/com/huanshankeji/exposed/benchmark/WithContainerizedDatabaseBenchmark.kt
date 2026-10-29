package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.testcontainers.connectionConfig
import com.huanshankeji.testcontainers.LatestPostgreSQLContainer
import kotlinx.benchmark.Setup
import kotlinx.benchmark.TearDown

abstract class WithContainerizedDatabaseBenchmark : AbstractBenchmark() {
    val postgreSQLContainer = LatestPostgreSQLContainer()
    val connectionConfig get() = postgreSQLContainer.connectionConfig()

    @Setup
    fun setup() {
        postgreSQLContainer.start()
    }

    @TearDown
    fun tearDown() {
        postgreSQLContainer.stop()
    }
}