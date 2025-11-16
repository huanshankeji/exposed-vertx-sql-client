package com.huanshankeji.exposedvertxsqlclient.postgresql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.exposed.exposedDatabaseConnect
import com.huanshankeji.exposedvertxsqlclient.jdbc.postgresqlUnixSocketJdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * @see exposedDatabaseConnect
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectPostgresql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    exposedDatabaseConnect(
        "postgresql", "org.postgresql.Driver", setupConnection, databaseConfig, manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectPostgresqlWithParameterConnectionConfig")
fun exposedDatabaseConnectPostgresql(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnectPostgresql(setupConnection, databaseConfig, manager)

/**
 * Connect to PostgreSQL using Unix domain socket with peer authentication.
 * This requires the junixsocket library to be available at runtime.
 * @see ConnectionConfig.UnixDomainSocketWithPeerAuthentication
 */
@ExperimentalEvscApi
fun ConnectionConfig.UnixDomainSocketWithPeerAuthentication.exposedDatabaseConnectPostgresql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    Database.connect(
        postgresqlUnixSocketJdbcUrl(database, path),
        "org.postgresql.Driver",
        // Unix domain socket with peer authentication doesn't require username/password in JDBC URL
        // The authentication is handled by the OS
        setupConnection = setupConnection,
        databaseConfig = databaseConfig,
        manager = manager
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectPostgresqlWithUnixSocket")
fun exposedDatabaseConnectPostgresql(
    unixSocketConnectionConfig: ConnectionConfig.UnixDomainSocketWithPeerAuthentication,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    unixSocketConnectionConfig.exposedDatabaseConnectPostgresql(setupConnection, databaseConfig, manager)

/**
 * Connect to PostgreSQL using any ConnectionConfig type (Socket or UnixDomainSocketWithPeerAuthentication).
 */
@ExperimentalEvscApi
fun ConnectionConfig.exposedDatabaseConnectPostgresql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    when (this) {
        is ConnectionConfig.Socket -> exposedDatabaseConnectPostgresql(setupConnection, databaseConfig, manager)
        is ConnectionConfig.UnixDomainSocketWithPeerAuthentication -> exposedDatabaseConnectPostgresql(setupConnection, databaseConfig, manager)
    }
