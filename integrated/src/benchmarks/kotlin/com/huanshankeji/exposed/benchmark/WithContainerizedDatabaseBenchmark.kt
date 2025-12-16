package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.LatestPostgreSQLContainer
import kotlinx.benchmark.Setup
import kotlinx.benchmark.TearDown

abstract class WithContainerizedDatabaseBenchmark : AbstractBenchmark() {
    val postgreSQLContainer = LatestPostgreSQLContainer()

    @Setup
    fun setUp() {
        postgreSQLContainer.start()
    }

    @TearDown
    fun tearDown() {
        postgreSQLContainer.stop()
    }
}