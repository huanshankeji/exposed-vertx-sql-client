package com.huanshankeji.exposedvertxsqlclient.oracle.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.oracleJdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * @see exposedDatabaseConnect
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectOracle(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    // https://www.jetbrains.com/help/exposed/working-with-database.html#oracle
    Database.connect(
        oracleJdbcUrl(), "oracle.jdbc.OracleDriver", user, password, setupConnection, databaseConfig, manager = manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectOracleWithParameterConnectionConfig")
fun exposedDatabaseConnectOracle(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnectOracle(setupConnection, databaseConfig, manager)
