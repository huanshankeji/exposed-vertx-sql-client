package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.mssql.exposed.exposedDatabaseConnectMssql
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.oracle.exposed.exposedDatabaseConnectOracle
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mssqlserver.MSSQLServerContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

fun JdbcDatabaseContainer<*>.connectionConfig() =
    ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)


fun LatestPostgreSQLContainer(): PostgreSQLContainer =
    PostgreSQLContainer(DockerImageName.parse("postgres:latest"))

fun PostgreSQLContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectPostgresql()

// https://testcontainers.com/modules/postgresql/
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

fun MSSQLServerContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectMssql()
