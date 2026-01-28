package com.huanshankeji.exposed.benchmark.jdbc

import com.huanshankeji.exposed.benchmark.WithContainerizedDatabaseBenchmark
import com.huanshankeji.exposedvertxsqlclient.integrated.exposedDatabaseConnect
import kotlinx.benchmark.Setup
import org.jetbrains.exposed.v1.jdbc.Database

abstract class WithContainerizedDatabaseAndExposedDatabaseBenchmark : WithContainerizedDatabaseBenchmark() {
    lateinit var database: Database
    fun databaseConnect() =
        postgreSQLContainer.exposedDatabaseConnect()

    @Setup
    override fun setup() {
        super.setup()
        database = databaseConnect()
    }
}