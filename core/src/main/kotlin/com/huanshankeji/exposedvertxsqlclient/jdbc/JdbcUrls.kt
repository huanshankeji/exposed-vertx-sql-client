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

// https://www.jetbrains.com/help/exposed/working-with-database.html#postgresql
@ExperimentalEvscApi
fun ConnectionConfig.Socket.postgresqlJdbcUrl() =
    postgresqlAndMysqlJdbcUrl("postgresql")

// https://www.jetbrains.com/help/exposed/working-with-database.html#mysql
@ExperimentalEvscApi
fun ConnectionConfig.Socket.mysqlJdbcUrl() =
    postgresqlAndMysqlJdbcUrl("mysql")

// https://www.jetbrains.com/help/exposed/working-with-database.html#oracle
@ExperimentalEvscApi
fun ConnectionConfig.Socket.oracleJdbcUrl() =
    "jdbc:oracle:thin:@//$host${port?.let { ":$it" } ?: ""}/$database"

// https://www.jetbrains.com/help/exposed/working-with-database.html#sql-server
@ExperimentalEvscApi
fun ConnectionConfig.Socket.sqlServerJdbcUrl() =
    "jdbc:sqlserver://$host${port?.let { ":$it" } ?: ""};databaseName=$database"

/**
 * Suitable for container testing.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.sqlServerJdbcUrlWithEncryptEqFalse() =
    sqlServerJdbcUrl() + ";encrypt=false"
