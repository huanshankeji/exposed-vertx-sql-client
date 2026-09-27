/*
All helpers here have moved:
- EVSC-specific helpers (`connectionConfig()`, `exposedDatabaseConnect()`, `hikariConfig()`, `hikariDataSource()`)
  → `com.huanshankeji.exposedvertxsqlclient.testcontainers` (`exposed-vertx-sql-client-testcontainers` module).
- Generic container factories (`LatestPostgreSQLContainer()`, etc.)
  → `com.huanshankeji.testcontainers` (`com.huanshankeji:kotlin-common-testcontainers`).

This file is kept as a deprecated forwarding shim and will be removed in a future release.
*/

@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.testcontainers.connectionConfig as newConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.testcontainers.exposedDatabaseConnect as newExposedDatabaseConnect
import com.huanshankeji.exposedvertxsqlclient.testcontainers.hikariConfig as newHikariConfig
import com.huanshankeji.exposedvertxsqlclient.testcontainers.hikariDataSource as newHikariDataSource
import com.huanshankeji.testcontainers.LatestPostgreSQLContainer as NewLatestPostgreSQLContainer
import com.zaxxer.hikari.HikariConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mssqlserver.MSSQLServerContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("connectionConfig()", "com.huanshankeji.exposedvertxsqlclient.testcontainers.connectionConfig")
)
fun JdbcDatabaseContainer<*>.connectionConfig() = newConnectionConfig()


// https://testcontainers.com/modules/postgresql/
@Deprecated(
    "Moved to `com.huanshankeji:kotlin-common-testcontainers` as `com.huanshankeji.testcontainers.LatestPostgreSQLContainer`.",
    ReplaceWith("LatestPostgreSQLContainer()", "com.huanshankeji.testcontainers.LatestPostgreSQLContainer")
)
fun LatestPostgreSQLContainer(): PostgreSQLContainer = NewLatestPostgreSQLContainer()

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("exposedDatabaseConnect()", "com.huanshankeji.exposedvertxsqlclient.testcontainers.exposedDatabaseConnect")
)
fun PostgreSQLContainer.exposedDatabaseConnect(): Database = newExposedDatabaseConnect()

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("hikariConfig(maximumPoolSize, extraConfig)", "com.huanshankeji.exposedvertxsqlclient.testcontainers.hikariConfig")
)
fun ConnectionConfig.Socket.hikariConfig(
    maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {},
): HikariConfig = newHikariConfig(maximumPoolSize, extraConfig)

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("hikariConfig(maximumPoolSize, extraConfig)", "com.huanshankeji.exposedvertxsqlclient.testcontainers.hikariConfig")
)
fun PostgreSQLContainer.hikariConfig(maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {}): HikariConfig =
    newHikariConfig(maximumPoolSize, extraConfig)

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("hikariDataSource(maximumPoolSize, extraConfig)", "com.huanshankeji.exposedvertxsqlclient.testcontainers.hikariDataSource")
)
fun PostgreSQLContainer.hikariDataSource(maximumPoolSize: Int, extraConfig: HikariConfig.() -> Unit = {}) =
    newHikariDataSource(maximumPoolSize, extraConfig)


// https://testcontainers.com/modules/mysql/
@Deprecated("Will likely be moved to `kotlin-common-testcontainers` in the future.")
fun LatestMySQLContainer(): MySQLContainer =
    MySQLContainer(DockerImageName.parse("mysql:latest"))

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("exposedDatabaseConnect()", "com.huanshankeji.exposedvertxsqlclient.testcontainers.exposedDatabaseConnect")
)
fun MySQLContainer.exposedDatabaseConnect(): Database = newExposedDatabaseConnect()


// https://testcontainers.com/modules/oracle-free/
@Deprecated("Will likely be moved to `kotlin-common-testcontainers` in the future.")
fun LatestOracleContainer(): OracleContainer =
    OracleContainer(DockerImageName.parse("gvenzl/oracle-free:latest"))

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("exposedDatabaseConnect()", "com.huanshankeji.exposedvertxsqlclient.testcontainers.exposedDatabaseConnect")
)
fun OracleContainer.exposedDatabaseConnect(): Database = newExposedDatabaseConnect()


/*
https://testcontainers.com/modules/mssql/
https://learn.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker
https://hub.docker.com/r/microsoft/mssql-server
*/
@Deprecated("Will likely be moved to `kotlin-common-testcontainers` in the future.")
fun LatestMssqlContainer(): MSSQLServerContainer =
    MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
        .acceptLicense()

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("connectionConfig()", "com.huanshankeji.exposedvertxsqlclient.testcontainers.connectionConfig")
)
fun MSSQLServerContainer.connectionConfig() = newConnectionConfig()

@Deprecated(
    "Moved to `com.huanshankeji.exposedvertxsqlclient.testcontainers`.",
    ReplaceWith("exposedDatabaseConnect()", "com.huanshankeji.exposedvertxsqlclient.testcontainers.exposedDatabaseConnect")
)
fun MSSQLServerContainer.exposedDatabaseConnect(): Database = newExposedDatabaseConnect()
