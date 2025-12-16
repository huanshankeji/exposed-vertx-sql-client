package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.exposedDatabaseConnect
import kotlinx.benchmark.Setup
import org.jetbrains.exposed.v1.jdbc.Database

abstract class WithContainerizedDatabaseAndExposedDatabaseBenchmark : WithContainerizedDatabaseBenchmark() {
    lateinit var database: Database
    fun databaseConnect() =
        postgreSQLContainer.exposedDatabaseConnect()

    @Setup
    override fun setUp() {
        super.setUp()
        database = databaseConnect()
    }
}