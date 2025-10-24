package com.huanshankeji.exposedvertxsqlclient.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.jdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * Further configurations such as [setupConnection], [databaseConfig], and [manager] are most likely not needed
 * because the Exposed [Database] is mostly only used for table creation and SQL generation.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnect(
    rdbms: String,
    driver: String,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    //connectionAutoRegistration: DatabaseConnectionAutoRegistration = connectionInstanceImpl, // `connectionInstanceImpl` is `private`
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    Database.connect(jdbcUrl(rdbms), driver, user, password, setupConnection, databaseConfig, manager = manager)

@ExperimentalEvscApi
fun exposedDatabaseConnect(
    rdbms: String,
    socketConnectionConfig: ConnectionConfig.Socket,
    driver: String,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnect(rdbms, driver, setupConnection, databaseConfig, manager)
