package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
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
