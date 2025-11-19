package com.huanshankeji.exposedvertxsqlclient.oracle.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * Creates an Oracle JDBC connection URL.
 * Oracle uses a different URL format: jdbc:oracle:thin:@//host:port/service_name
 */
private fun ConnectionConfig.Socket.oracleJdbcUrl(): String =
    "jdbc:oracle:thin:@//$host:$port/$database"

/**
 * Connects to an Oracle database using Exposed.
 * Note: Oracle JDBC URL format is different from the standard format.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectOracle(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    Database.connect(
        oracleJdbcUrl(),
        "oracle.jdbc.OracleDriver",
        user,
        password,
        setupConnection,
        databaseConfig,
        manager = manager
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
