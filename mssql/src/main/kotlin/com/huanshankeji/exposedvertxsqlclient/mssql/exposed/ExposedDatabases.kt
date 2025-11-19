package com.huanshankeji.exposedvertxsqlclient.mssql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * Creates a SQL Server JDBC connection URL.
 * SQL Server uses a different URL format: jdbc:sqlserver://host:port;databaseName=database
 */
private fun ConnectionConfig.Socket.mssqlJdbcUrl(): String =
    "jdbc:sqlserver://$host:$port;databaseName=$database"

/**
 * Connects to a SQL Server database using Exposed.
 * Note: SQL Server JDBC URL format is different from the standard format.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectMssql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    Database.connect(
        mssqlJdbcUrl(),
        "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        user,
        password,
        setupConnection,
        databaseConfig,
        manager = manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectMssqlWithParameterConnectionConfig")
fun exposedDatabaseConnectMssql(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnectMssql(setupConnection, databaseConfig, manager)
