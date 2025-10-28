package com.huanshankeji.exposedvertxsqlclient.mysql.exposed

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
fun ConnectionConfig.Socket.exposedDatabaseConnectMysql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null
) =
    exposedDatabaseConnect(
        "mysql", "com.mysql.cj.jdbc.Driver", setupConnection, databaseConfig
    )

@ExperimentalEvscApi
@JvmName("exposedDatabaseConnectMysqlWithParameterConnectionConfig")
fun exposedDatabaseConnectMysql(
    socketConnectionConfig: ConnectionConfig.Socket,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null
) =
    socketConnectionConfig.exposedDatabaseConnectMysql(setupConnection, databaseConfig)
