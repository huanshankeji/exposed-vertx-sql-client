@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.postgresqlJdbcUrl
import com.huanshankeji.exposedvertxsqlclient.mssql.exposed.exposedDatabaseConnectMssql
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.oracle.exposed.exposedDatabaseConnectOracle
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mssqlserver.MSSQLServerContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

fun JdbcDatabaseContainer<*>.connectionConfig() =
    ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)


// https://testcontainers.com/modules/postgresql/
fun LatestPostgreSQLContainer(): PostgreSQLContainer =
    PostgreSQLContainer(DockerImageName.parse("postgres:latest"))

// TODO consider deprecating these functions and recommending the caller to call `connectionConfig()` and then call the extensions function on `ConnectionConfig.Socket` instead
fun PostgreSQLContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectPostgresql()

// move to the `postgresql` module if it's proved useful
fun ConnectionConfig.Socket.hikariConfig(
    maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {},
): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = postgresqlJdbcUrl()
        driverClassName = "org.postgresql.Driver" // TODO extract
        username = user
        password = this@hikariConfig.password
        this.maximumPoolSize = maximumPoolSize
        // configurations for SQL preparation for Vert.x SQL Clients
        isReadOnly = true
        transactionIsolation = "TRANSACTION_READ_UNCOMMITTED"
        extraConfig()
    }

fun PostgreSQLContainer.hikariConfig(maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {}): HikariConfig {
    val connectionConfig = connectionConfig()
    return connectionConfig.hikariConfig(maximumPoolSize, extraConfig)
}

fun PostgreSQLContainer.hikariDataSource(maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {}) =
    HikariDataSource(hikariConfig(maximumPoolSize, extraConfig))


// https://testcontainers.com/modules/mysql/
fun LatestMySQLContainer(): MySQLContainer =
    MySQLContainer(DockerImageName.parse("mysql:latest"))

fun MySQLContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectMysql()


// https://testcontainers.com/modules/oracle-free/
fun LatestOracleContainer(): OracleContainer =
    OracleContainer(DockerImageName.parse("gvenzl/oracle-free:latest"))

fun OracleContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectOracle()


/*
https://testcontainers.com/modules/mssql/
https://learn.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker
https://hub.docker.com/r/microsoft/mssql-server
*/
fun LatestMssqlContainer(): MSSQLServerContainer =
    MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
        .acceptLicense()

// `MSSQLServerContainer` doesn't support `getDatabaseName()`, so we need a specific implementation.
fun MSSQLServerContainer.connectionConfig() =
    ConnectionConfig.Socket(host, firstMappedPort, username, password, "master")

fun MSSQLServerContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectMssql()
