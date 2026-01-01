package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.integrated.LatestPostgreSQLContainer
import com.huanshankeji.exposedvertxsqlclient.integrated.connectionConfig
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