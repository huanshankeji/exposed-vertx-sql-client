package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.db2.exposed.exposedDatabaseConnectDb2
import com.huanshankeji.exposedvertxsqlclient.mssql.exposed.exposedDatabaseConnectMssql
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.oracle.exposed.exposedDatabaseConnectOracle
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.Db2Container
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

fun LatestPostgreSQLContainer(): PostgreSQLContainer =
    PostgreSQLContainer(DockerImageName.parse("postgres:latest"))

fun JdbcDatabaseContainer<*>.connectionConfig() =
    ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)

fun PostgreSQLContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectPostgresql()

fun LatestMySQLContainer(): MySQLContainer =
    MySQLContainer(DockerImageName.parse("mysql:latest"))

fun MySQLContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectMysql()

fun LatestOracleContainer(): OracleContainer =
    OracleContainer(DockerImageName.parse("gvenzl/oracle-free:latest"))

fun OracleContainer.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectOracle()

fun LatestMssqlContainer(): MSSQLServerContainer<*> =
    MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))

fun MSSQLServerContainer<*>.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectMssql()

fun LatestDb2Container(): Db2Container =
    Db2Container(DockerImageName.parse("icr.io/db2_community/db2:latest"))

fun Db2Container.exposedDatabaseConnect(): Database =
    connectionConfig().exposedDatabaseConnectDb2()
