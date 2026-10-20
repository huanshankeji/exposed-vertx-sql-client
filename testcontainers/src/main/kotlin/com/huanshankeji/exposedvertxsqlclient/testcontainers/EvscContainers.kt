@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.testcontainers

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.postgresqlJdbcUrl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mssqlserver.MSSQLServerContainer

/*
EVSC-specific Testcontainers helpers.
Generic container factories and `ContainerState.hostAndPort()` live in `com.huanshankeji:kotlin-common-testcontainers`.
*/

fun JdbcDatabaseContainer<*>.connectionConfig() =
    ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)

// move to the `postgresql` module if it's proved useful
@ExperimentalEvscApi
fun ConnectionConfig.Socket.postgresqlHikariConfig(
    maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {},
): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = postgresqlJdbcUrl()
        driverClassName = "org.postgresql.Driver" // TODO extract
        username = user
        password = this@postgresqlHikariConfig.password
        this.maximumPoolSize = maximumPoolSize
        // configurations for SQL preparation for Vert.x SQL Clients
        isReadOnly = true
        transactionIsolation = "TRANSACTION_READ_UNCOMMITTED"
        extraConfig()
    }

@ExperimentalEvscApi
fun ConnectionConfig.Socket.postgresqlHikariDataSource(maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {}) =
    HikariDataSource(postgresqlHikariConfig(maximumPoolSize, extraConfig))

// `MSSQLServerContainer` doesn't support `getDatabaseName()`, so we need a specific implementation.
fun MSSQLServerContainer.connectionConfig() =
    ConnectionConfig.Socket(host, firstMappedPort, username, password, "master")
