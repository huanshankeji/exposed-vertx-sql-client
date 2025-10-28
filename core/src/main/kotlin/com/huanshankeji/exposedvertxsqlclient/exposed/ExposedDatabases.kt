package com.huanshankeji.exposedvertxsqlclient.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.jdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection

/**
 * Further configurations such as [setupConnection] and [databaseConfig] are most likely not needed
 * because the Exposed [Database] is mostly only used for table creation and SQL generation.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnect(
    rdbms: String,
    driver: String,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null
) =
    Database.connect(jdbcUrl(rdbms), driver, user, password, setupConnection, databaseConfig)

@ExperimentalEvscApi
fun exposedDatabaseConnect(
    rdbms: String,
    socketConnectionConfig: ConnectionConfig.Socket,
    driver: String,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null
) =
    socketConnectionConfig.exposedDatabaseConnect(rdbms, driver, setupConnection, databaseConfig)
