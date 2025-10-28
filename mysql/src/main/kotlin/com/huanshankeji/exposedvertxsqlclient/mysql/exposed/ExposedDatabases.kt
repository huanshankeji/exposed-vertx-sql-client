package com.huanshankeji.exposedvertxsqlclient.mysql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.exposed.exposedDatabaseConnect
import com.huanshankeji.exposedvertxsqlclient.jdbc.mysqlUnixSocketJdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * @see exposedDatabaseConnect
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectMysql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    exposedDatabaseConnect(
        "mysql", "com.mysql.cj.jdbc.Driver", setupConnection, databaseConfig, manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectMysqlWithParameterConnectionConfig")
fun exposedDatabaseConnectMysql(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnectMysql(setupConnection, databaseConfig, manager)

/**
 * Connect to MySQL using Unix domain socket with peer authentication.
 * This requires the junixsocket library to be available at runtime.
 * @see ConnectionConfig.UnixDomainSocketWithPeerAuthentication
 */
@ExperimentalEvscApi
fun ConnectionConfig.UnixDomainSocketWithPeerAuthentication.exposedDatabaseConnectMysql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    Database.connect(
        mysqlUnixSocketJdbcUrl(database, path),
        "com.mysql.cj.jdbc.Driver",
        // Unix domain socket with peer authentication doesn't require username/password in JDBC URL
        // The authentication is handled by the OS
        setupConnection = setupConnection,
        databaseConfig = databaseConfig,
        manager = manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectMysqlWithUnixSocket")
fun exposedDatabaseConnectMysql(
    unixSocketConnectionConfig: ConnectionConfig.UnixDomainSocketWithPeerAuthentication,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    unixSocketConnectionConfig.exposedDatabaseConnectMysql(setupConnection, databaseConfig, manager)

/**
 * Connect to MySQL using any ConnectionConfig type (Socket or UnixDomainSocketWithPeerAuthentication).
 */
@ExperimentalEvscApi
fun ConnectionConfig.exposedDatabaseConnectMysql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    when (this) {
        is ConnectionConfig.Socket -> exposedDatabaseConnectMysql(setupConnection, databaseConfig, manager)
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> exposedDatabaseConnectMysql(setupConnection, databaseConfig, manager)
    }
