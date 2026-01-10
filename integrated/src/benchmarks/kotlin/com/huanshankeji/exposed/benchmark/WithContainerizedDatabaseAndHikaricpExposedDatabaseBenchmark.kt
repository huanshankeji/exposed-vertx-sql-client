package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.integrated.hikariDataSource
import com.zaxxer.hikari.HikariDataSource
import kotlinx.benchmark.Setup
import kotlinx.benchmark.TearDown
import org.jetbrains.exposed.v1.jdbc.Database

abstract class WithContainerizedDatabaseAndHikaricpExposedDatabaseBenchmark : WithContainerizedDatabaseBenchmark() {
    abstract val maximumPoolSize: Int
    lateinit var hikariDataSource: HikariDataSource
    lateinit var database: Database

    // temporarily made private because it should not be called directly
    private fun databaseConnect(): Database {
        return Database.connect(hikariDataSource)
    }

    @Setup
    override fun setup() {
        super.setup()
        hikariDataSource = postgreSQLContainer.hikariDataSource(maximumPoolSize)
        database = databaseConnect()
    }

    @TearDown
    override fun tearDown() {
        hikariDataSource.close()
        super.tearDown()
    }
}