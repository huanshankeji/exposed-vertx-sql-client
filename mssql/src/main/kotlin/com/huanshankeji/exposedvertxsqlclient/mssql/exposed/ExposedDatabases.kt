package com.huanshankeji.exposedvertxsqlclient.mssql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.sqlServerJdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * @see exposedDatabaseConnect
 * @param url the JDBC URL to override the default [sqlServerJdbcUrl]. This JDBC URL for SQL Server sometimes need to be customized with extra parameters.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectMssql(
    url: String = sqlServerJdbcUrl(),
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    Database.connect(
        url,
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
    url: String = socketConnectionConfig.sqlServerJdbcUrl(),
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnectMssql(url, setupConnection, databaseConfig, manager)
