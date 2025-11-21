package com.huanshankeji.exposedvertxsqlclient.mysql.exposed

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.jdbc.mysqlJdbcUrl
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * Further configurations such as [setupConnection], [databaseConfig], and [manager] are most likely not needed
 * because the Exposed [Database] is mostly only used for table creation and SQL generation.
 */
@ExperimentalEvscApi
fun ConnectionConfig.Socket.exposedDatabaseConnectMysql(
    setupConnection: (Connection) -> Unit = {},
    databaseConfig: DatabaseConfig? = null,
    manager: (Database) -> TransactionManager = { TransactionManager(it) }
) =
    // https://www.jetbrains.com/help/exposed/working-with-database.html#mysql
    Database.connect(
        mysqlJdbcUrl(), "com.mysql.cj.jdbc.Driver", user, password, setupConnection, databaseConfig, manager = manager
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
