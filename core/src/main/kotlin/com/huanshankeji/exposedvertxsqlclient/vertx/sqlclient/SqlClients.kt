package com.huanshankeji.exposedvertxsqlclient.vertx.sqlclient

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig
import io.vertx.core.Vertx
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.SqlConnectOptions

/**
 * [SqlConnectOptions.cachePreparedStatements] improves performance greatly (tested on PostgreSQL) so it's enabled by default. TODO see below
 */
inline fun <SqlClientT : SqlClient, SqlConnectOptionsT : SqlConnectOptions, PoolOptionsT : PoolOptions?> createGenericSqlClient(
    vertx: Vertx?,
    connectionConfig: ConnectionConfig,
    sqlConnectOptionsFromConstructor: SqlConnectOptionsT,
    extraPgConnectOptions: SqlConnectOptionsT.() -> Unit = {},
    poolOptionsFromConstructor: PoolOptionsT,
    extraPoolOptions: PoolOptionsT.() -> Unit = {},
    create: (Vertx?, SqlConnectOptionsT, PoolOptionsT) -> SqlClientT
): SqlClientT {
    val pgConnectOptions = sqlConnectOptionsFromConstructor.apply {
        // TODO consider extracting this into a conventional config and move to "kotlin-common", decoupling it from this library
        cachePreparedStatements = true
        setFrom(connectionConfig)
        extraPgConnectOptions()
    }

    return create(vertx, pgConnectOptions, poolOptionsFromConstructor.apply(extraPoolOptions))
}
