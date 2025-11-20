package com.huanshankeji.exposedvertxsqlclient.jdbc

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.jdbc.jdbcUrl

internal const val JDBC_URL_FORMAT_NOT_UNIVERSAL_DEPRECATION_MESSAGE =
    "The JDBC URL format is not actually universal."

@Deprecated(JDBC_URL_FORMAT_NOT_UNIVERSAL_DEPRECATION_MESSAGE)
fun ConnectionConfig.Socket.jdbcUrl(rdbms: String) =
    jdbcUrl(rdbms, host, port, database)

@ExperimentalEvscApi
fun ConnectionConfig.Socket.postgresqlAndMysqlJdbcUrl(rdbms: String) =
    "jdbc:$rdbms://$host${port?.let { ":$it" } ?: ""}/$database"

@ExperimentalEvscApi
fun ConnectionConfig.Socket.postgresqlJdbcUrl() =
    postgresqlAndMysqlJdbcUrl("postgresql")

@ExperimentalEvscApi
fun ConnectionConfig.Socket.mysqlJdbcUrl() =
    postgresqlAndMysqlJdbcUrl("mysql")

// https://www.jetbrains.com/help/exposed/working-with-database.html#oracle
@ExperimentalEvscApi
fun ConnectionConfig.Socket.oracleJdbcUrl() =
    "jdbc:oracle:thin:@//$host${port?.let { ":$it" } ?: ""}/$database"

@ExperimentalEvscApi
fun ConnectionConfig.Socket.sqlServerJdbcUrl() =
    "jdbc:sqlserver://$host${port?.let { ":$it" } ?: ""};databaseName=$database"
