package com.huanshankeji.exposedvertxsqlclient.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.jdbcUrl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
    manager: (Database) -> TransactionManager = { ThreadLocalTransactionManager(it) }
) =
    Database.connect(jdbcUrl(rdbms), driver, user, password, setupConnection, databaseConfig, manager)

@ExperimentalEvscApi
fun exposedDatabaseConnect(
    rdbms: String,
    socketConnectionConfig: ConnectionConfig.Socket,
    driver: String,
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { ThreadLocalTransactionManager(it) }
) =
    socketConnectionConfig.exposedDatabaseConnect(rdbms, driver, setupConnection, databaseConfig, manager)
