package com.huanshankeji.exposed.benchmark

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.exposedDatabaseConnectPostgreSql
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@State(Scope.Benchmark)
class WithContainerizedDatabaseBenchmark : AbstractBenchmark() {
    val postgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
    lateinit var database: Database

    fun databaseConnect() =
        exposedDatabaseConnectPostgreSql(with(postgreSQLContainer) {
            ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)
        })

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