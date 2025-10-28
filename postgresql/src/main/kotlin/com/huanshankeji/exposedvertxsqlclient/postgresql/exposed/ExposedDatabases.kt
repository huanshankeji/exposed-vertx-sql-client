package com.huanshankeji.exposedvertxsqlclient.postgresql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.exposed.exposedDatabaseConnect
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection

/**
 * @see exposedDatabaseConnect
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectPostgresql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null
) =
    exposedDatabaseConnect(
        "postgresql", "org.postgresql.Driver", setupConnection, databaseConfig
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectPostgresqlWithParameterConnectionConfig")
fun exposedDatabaseConnectPostgresql(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null
) =
    socketConnectionConfig.exposedDatabaseConnectPostgresql(setupConnection, databaseConfig)
