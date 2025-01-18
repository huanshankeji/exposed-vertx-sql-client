package com.huanshankeji.exposedvertxsqlclient.mysql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.exposed.exposedDatabaseConnect
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

/**
 * @see exposedDatabaseConnect
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectMysql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { ThreadLocalTransactionManager(it) }
) =
    exposedDatabaseConnect(
        "mysql", "com.mysql.cj.jdbc.Driver", setupConnection, databaseConfig, manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectMySQLWithParameterConnectionConfig")
fun exposedDatabaseConnectMysql(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { ThreadLocalTransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnectMysql(setupConnection, databaseConfig, manager)
