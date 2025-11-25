package com.huanshankeji.exposedvertxsqlclient.postgresql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.postgresqlJdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * Further configurations such as [setupConnection], [databaseConfig], and [manager] are most likely not needed
 * because the Exposed [Database] is mostly only used for table creation and SQL generation.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectPostgresql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    // https://www.jetbrains.com/help/exposed/working-with-database.html#postgresql
    Database.connect(
        postgresqlJdbcUrl(), "org.postgresql.Driver", user, password, setupConnection, databaseConfig, manager = manager
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
